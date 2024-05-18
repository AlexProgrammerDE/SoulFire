/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.protocol.bot.movement;

import com.soulfiremc.server.data.Attribute;
import com.soulfiremc.server.data.AttributeType;
import com.soulfiremc.server.data.BlockState;
import com.soulfiremc.server.data.BlockTags;
import com.soulfiremc.server.data.BlockType;
import com.soulfiremc.server.data.ModifierOperation;
import com.soulfiremc.server.protocol.bot.SessionDataManager;
import com.soulfiremc.server.protocol.bot.state.Level;
import com.soulfiremc.server.protocol.bot.state.TagsState;
import com.soulfiremc.server.protocol.bot.state.entity.ClientEntity;
import com.soulfiremc.server.protocol.bot.state.entity.Entity;
import com.soulfiremc.server.util.MathHelper;
import it.unimi.dsi.fastutil.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import lombok.Getter;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;
import org.jetbrains.annotations.Nullable;

/**
 * Java port of prismarine-physics.
 */
public class BotMovementManager {
  private static final List<BlockType> WATER_TYPES = List.of(BlockType.WATER);
  private static final List<BlockType> LAVA_TYPES = List.of(BlockType.WATER);
  private static final List<BlockType> WATER_LIKE_TYPES =
    List.of(
      BlockType.WATER,
      BlockType.SEAGRASS,
      BlockType.TALL_SEAGRASS,
      BlockType.KELP,
      BlockType.KELP_PLANT,
      BlockType.BUBBLE_COLUMN);
  private final ClientEntity clientEntity;
  @Getter
  private final PlayerMovementState movementState;
  private final PhysicsData physics;
  private final TagsState tagsState;
  private final ControlState controlState;

  public BotMovementManager(
    SessionDataManager dataManager,
    PlayerMovementState movementState,
    ClientEntity clientEntity) {
    this.clientEntity = clientEntity;
    this.movementState = movementState;
    this.physics = clientEntity.physics();
    this.tagsState = dataManager.tagsState();
    this.controlState = dataManager.controlState();
  }

  private static void consumeIntersectedBlocks(
    Level world, AABB queryBB, BiConsumer<BlockState, Vector3i> consumer) {
    var startX = MathHelper.floorDouble(queryBB.minX - 1.0E-7) - 1;
    var endX = MathHelper.floorDouble(queryBB.maxX + 1.0E-7) + 1;
    var startY = MathHelper.floorDouble(queryBB.minY - 1.0E-7) - 1;
    var endY = MathHelper.floorDouble(queryBB.maxY + 1.0E-7) + 1;
    var startZ = MathHelper.floorDouble(queryBB.minZ - 1.0E-7) - 1;
    var endZ = MathHelper.floorDouble(queryBB.maxZ + 1.0E-7) + 1;

    for (var x = startX; x <= endX; x++) {
      for (var y = startY; y <= endY; y++) {
        for (var z = startZ; z <= endZ; z++) {
          var cursor = Vector3i.from(x, y, z);
          var block = world.getBlockState(cursor);

          for (var collisionBox : block.getCollisionBoxes(cursor)) {
            if (collisionBox.intersects(queryBB)) {
              consumer.accept(block, cursor);
              break;
            }
          }
        }
      }
    }
  }

  private static double horizontalDistanceSqr(Vector3d vec) {
    return vec.getX() * vec.getX() + vec.getZ() * vec.getZ();
  }

  public static Vector3d collideBoundingBox(Level world, Vector3d targetVec, AABB queryBB) {
    return collideWith(
      targetVec, queryBB, world.getCollisionBoxes(queryBB.expandTowards(targetVec)));
  }

  private static Vector3d collideWith(
    Vector3d direction, AABB boundingBox, List<AABB> collisionBoxes) {
    var dx = direction.getX();
    var dy = direction.getY();
    var dz = direction.getZ();

    if (dy != 0) {
      for (var blockBB : collisionBoxes) {
        dy = blockBB.computeOffsetY(boundingBox, dy);
      }

      if (dy != 0) {
        boundingBox = boundingBox.move(0, dy, 0);
      }
    }

    var xLessThanZ = Math.abs(dx) < Math.abs(dz);
    if (xLessThanZ && dz != 0) {
      for (var blockBB : collisionBoxes) {
        dz = blockBB.computeOffsetZ(boundingBox, dz);
      }

      if (dz != 0) {
        boundingBox = boundingBox.move(0, 0, dz);
      }
    }

    if (dx != 0) {
      for (var blockBB : collisionBoxes) {
        dx = blockBB.computeOffsetX(boundingBox, dx);
      }

      if (!xLessThanZ && dx != 0) {
        boundingBox = boundingBox.move(dx, 0, 0);
      }
    }

    if (!xLessThanZ && dz != 0.0) {
      for (var blockBB : collisionBoxes) {
        dz = blockBB.computeOffsetZ(boundingBox, dz);
      }
    }

    return Vector3d.from(dx, dy, dz);
  }

  private static LookingVectorData getLookingVector(Entity clientEntity) {
    // given a yaw pitch, we need the looking vector

    // yaw is right handed rotation about y (up) starting from -z (north)
    // pitch is -90 looking down, 90 looking up, 0 looking at horizon
    // lets get its coordinate system.
    // var x' = -z (north)
    // var y' = -x (west)
    // var z' = y (up)

    // the non normalized looking vector in x', y', z' space is
    // x' is cos(yaw)
    // y' is sin(yaw)
    // z' is tan(pitch)

    // substituting back in x, y, z, we get the looking vector in the normal x, y, z space
    // -z = cos(yaw) => z = -cos(yaw)
    // -x = sin(yaw) => x = -sin(yaw)
    // y = tan(pitch)

    // normalizing the vectors, we divide each by |sqrt(x*x + y*y + z*z)|
    // x*x + z*z = sin^2 + cos^2 = 1
    // so |sqrt(xx+yy+zz)| = |sqrt(1+tan^2(pitch))|
    //     = |sqrt(1+sin^2(pitch)/cos^2(pitch))|
    //     = |sqrt((cos^2+sin^2)/cos^2(pitch))|
    //     = |sqrt(1/cos^2(pitch))|
    //     = |+/- 1/cos(pitch)|
    //     = 1/cos(pitch) since pitch in [-90, 90]

    // the looking vector is therefore
    // x = -sin(yaw) * cos(pitch)
    // y = tan(pitch) * cos(pitch) = sin(pitch)
    // z = -cos(yaw) * cos(pitch)

    var yaw = clientEntity.yaw();
    var pitch = clientEntity.pitch();
    var sinYaw = Math.sin(yaw);
    var cosYaw = Math.cos(yaw);
    var sinPitch = Math.sin(pitch);
    var cosPitch = Math.cos(pitch);
    var lookX = -sinYaw * cosPitch;
    var lookZ = -cosYaw * cosPitch;
    var lookDir = new MutableVector3d(lookX, sinPitch, lookZ);
    return new LookingVectorData(
      yaw, pitch, sinYaw, cosYaw, sinPitch, cosPitch, lookX, sinPitch, lookZ, lookDir);
  }

  public static boolean isMaterialInBB(Level world, AABB queryBB, List<BlockType> types) {
    var minX = MathHelper.floorDouble(queryBB.minX);
    var minY = MathHelper.floorDouble(queryBB.minY);
    var minZ = MathHelper.floorDouble(queryBB.minZ);

    var maxX = MathHelper.floorDouble(queryBB.maxX);
    var maxY = MathHelper.floorDouble(queryBB.maxY);
    var maxZ = MathHelper.floorDouble(queryBB.maxZ);

    for (var x = minX; x <= maxX; x++) {
      for (var y = minY; y <= maxY; y++) {
        for (var z = minZ; z <= maxZ; z++) {
          var block = world.getBlockState(Vector3i.from(x, y, z));
          if (types.contains(block.blockType())) {
            return true;
          }
        }
      }
    }

    return false;
  }

  public static void setPositionToBB(AABB bb, MutableVector3d pos) {
    pos.x = (bb.minX + bb.maxX) / 2;
    pos.y = bb.minY;
    pos.z = (bb.minZ + bb.maxZ) / 2;
  }

  public boolean isClimbable(Level world, Vector3i pos) {
    var blockType = world.getBlockState(pos).blockType();
    return tagsState.isValueInTag(blockType, BlockTags.CLIMBABLE)
      || blockType == BlockType.POWDER_SNOW;
  }

  public void tick() {
    var world = clientEntity.level();
    var vel = movementState.vel;

    {
      var playerBB = clientEntity.boundingBox();
      var waterBB = playerBB.deflate(0.001, 0.401, 0.001);
      var lavaBB = playerBB.deflate(0.1, 0.4, 0.1);

      movementState.isInWater = isInWaterApplyCurrent(world, waterBB, vel);
      movementState.isInLava = isMaterialInBB(world, lavaBB, LAVA_TYPES);
    }

    // Reset velocity component if it falls under the threshold
    if (Math.abs(vel.x) < physics.negligeableVelocity) {
      vel.x = 0;
    }
    if (Math.abs(vel.y) < physics.negligeableVelocity) {
      vel.y = 0;
    }
    if (Math.abs(vel.z) < physics.negligeableVelocity) {
      vel.z = 0;
    }

    // Handle inputs
    if (controlState.jumping() || movementState.jumpQueued) {
      if (movementState.jumpTicks > 0) {
        movementState.jumpTicks--;
      }

      if (movementState.isInWater || movementState.isInLava) {
        vel.y += 0.04;
      } else if (movementState.onGround && movementState.jumpTicks == 0) {
        var blockBelow =
          world.getBlockState(movementState.pos.floored().offset(0, -0.5, 0).toImmutableInt());
        vel.y =
          (float) (0.42)
            * ((blockBelow.blockType() == BlockType.HONEY_BLOCK)
            ? physics.honeyblockJumpSpeed
            : 1);
        if (movementState.jumpBoost > 0) {
          vel.y += 0.1 * movementState.jumpBoost;
        }

        if (controlState.sprinting()) {
          var yaw = Math.PI - clientEntity.yaw();
          vel.x -= Math.sin(yaw) * 0.2;
          vel.z += Math.cos(yaw) * 0.2;
        }

        movementState.jumpTicks = physics.autojumpCooldown;
      }
    } else {
      movementState.jumpTicks = 0; // reset autojump cooldown
    }

    movementState.jumpQueued = false;

    var forward = 0.0F;
    if (controlState.forward()) {
      forward++;
    }

    if (controlState.backward()) {
      forward--;
    }

    var strafe = 0.0F;
    if (controlState.right()) {
      strafe++;
    }

    if (controlState.left()) {
      strafe--;
    }

    strafe *= 0.98F;
    forward *= 0.98F;

    if (controlState.sneaking()) {
      strafe *= (float) ((double) strafe * physics.sneakSpeed);
      forward *= (float) ((double) forward * physics.sneakSpeed);
    }

    movementState.elytraFlying =
      movementState.elytraFlying
        && movementState.elytraEquipped
        && !movementState.onGround
        && movementState.levitation == 0;

    if (movementState.fireworkRocketDuration > 0) {
      if (!movementState.elytraFlying) {
        movementState.fireworkRocketDuration = 0;
      } else {
        var lookingVector = getLookingVector(clientEntity);
        var lookDir = lookingVector.lookDir;
        vel.x += lookDir.x * 0.1 + (lookDir.x * 1.5 - vel.x) * 0.5;
        vel.y += lookDir.y * 0.1 + (lookDir.y * 1.5 - vel.y) * 0.5;
        vel.z += lookDir.z * 0.1 + (lookDir.z * 1.5 - vel.z) * 0.5;
        --movementState.fireworkRocketDuration;
      }
    }

    moveEntityWithHeading(world, strafe, forward);
  }

  public void moveEntityWithHeading(Level world, float strafe, float forward) {
    var vel = movementState.vel;
    var pos = movementState.pos;

    var gravityMultiplier = (vel.y <= 0 && movementState.slowFalling > 0) ? physics.slowFalling : 1;
    var speed = getSpeed();

    if (movementState.isInWater || movementState.isInLava) {
      // Water / Lava movement
      var lastY = pos.y;
      var liquidSpeed = physics.liquidSpeed;
      var typeSpeed = movementState.isInWater ? physics.waterSpeed : physics.lavaSpeed;
      var horizontalInertia = typeSpeed;

      if (movementState.isInWater) {
        var depthStrider = (float) movementState.depthStrider;

        if (depthStrider > 3) {
          depthStrider = 3;
        }

        if (!movementState.onGround) {
          depthStrider *= 0.5F;
        }

        if (depthStrider > 0) {
          horizontalInertia += ((0.54600006F - horizontalInertia) * depthStrider) / 3;
          liquidSpeed += (speed - liquidSpeed) * depthStrider / 3;
        }

        if (movementState.dolphinsGrace > 0) {
          horizontalInertia = 0.96F;
        }
      }

      applyHeading(strafe, forward, liquidSpeed);

      moveEntity(world, vel.x, vel.y, vel.z);
      vel.y *= typeSpeed;
      vel.y -=
        (movementState.isInWater ? physics.waterGravity : physics.lavaGravity)
          * gravityMultiplier;
      vel.x *= horizontalInertia;
      vel.z *= horizontalInertia;

      if (movementState.isCollidedHorizontally
        && doesNotCollide(world, pos.offset(vel.x, vel.y + 0.6 - pos.y + lastY, vel.z))) {
        vel.y = physics.outOfLiquidImpulse; // jump out of liquid
      }
    } else if (movementState.elytraFlying) {
      var lookingData = getLookingVector(clientEntity);
      var pitch = lookingData.pitch;
      var sinPitch = lookingData.sinPitch;
      var cosPitch = lookingData.cosPitch;
      var lookDir = lookingData.lookDir;

      var horizontalSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
      var cosPitchSquared = cosPitch * cosPitch;
      vel.y += physics.gravity * gravityMultiplier * (-1.0 + cosPitchSquared * 0.75);
      // cosPitch is in [0, 1], so cosPitch > 0.0 is just to protect against
      // divide by zero errors
      if (vel.y < 0.0 && cosPitch > 0.0) {
        var movingDownSpeedModifier = vel.y * (-0.1) * cosPitchSquared;
        vel.x += lookDir.x * movingDownSpeedModifier / cosPitch;
        vel.y += movingDownSpeedModifier;
        vel.z += lookDir.z * movingDownSpeedModifier / cosPitch;
      }

      if (pitch < 0.0 && cosPitch > 0.0) {
        var lookDownSpeedModifier = horizontalSpeed * (-sinPitch) * 0.04;
        vel.x += -lookDir.x * lookDownSpeedModifier / cosPitch;
        vel.y += lookDownSpeedModifier * 3.2;
        vel.z += -lookDir.z * lookDownSpeedModifier / cosPitch;
      }

      if (cosPitch > 0.0) {
        vel.x += (lookDir.x / cosPitch * horizontalSpeed - vel.x) * 0.1;
        vel.z += (lookDir.z / cosPitch * horizontalSpeed - vel.z) * 0.1;
      }

      vel.x *= 0.99;
      vel.y *= 0.98;
      vel.z *= 0.99;
      moveEntity(world, vel.x, vel.y, vel.z);

      if (movementState.onGround) {
        movementState.elytraFlying = false;
      }
    } else {
      // Normal movement
      var xzMultiplier = 0.91F;
      float frictionInfluencedSpeed;

      var blockUnder = world.getBlockState(pos.offset(0, -0.500001F, 0).toImmutableInt());
      if (movementState.onGround) {
        var friction = getBlockFriction(blockUnder.blockType());
        xzMultiplier *= friction;
        frictionInfluencedSpeed = speed * (0.21600002F / (friction * friction * friction));
      } else {
        frictionInfluencedSpeed = getFlyingSpeed();
      }

      applyHeading(strafe, forward, frictionInfluencedSpeed);

      if (isClimbable(world, pos.toImmutableInt())) {
        vel.x = MathHelper.doubleClamp(vel.x, -physics.climbMaxSpeed, physics.climbMaxSpeed);
        vel.z = MathHelper.doubleClamp(vel.z, -physics.climbMaxSpeed, physics.climbMaxSpeed);
        vel.y = Math.max(vel.y, controlState.sneaking() ? 0 : -physics.climbMaxSpeed);
      }

      moveEntity(world, vel.x, vel.y, vel.z);

      if ((movementState.isCollidedHorizontally || controlState.jumping())
        && isClimbable(world, pos.toImmutableInt())) {
        vel.y = physics.climbSpeed; // climb ladder
      }

      // Apply friction and gravity
      if (movementState.levitation > 0) {
        vel.y += (0.05 * movementState.levitation - vel.y) * 0.2;
      } else {
        vel.y -= physics.gravity * gravityMultiplier;
      }

      vel.x *= xzMultiplier;
      vel.y *= physics.airdrag;
      vel.z *= xzMultiplier;
    }
  }

  private float getFlyingSpeed() {
    if (controlState.flying()) {
      var abilitiesData = clientEntity.abilities();
      var flySpeed = abilitiesData == null ? 0.05F : abilitiesData.flySpeed();
      return controlState.sprinting() ? flySpeed * 2.0F : flySpeed;
    } else {
      return controlState.sprinting() ? 0.025999999F : 0.02F;
    }
  }

  public float getSpeed() {
    var attribute = movementState.entity().attributeState();
    var playerSpeedAttribute = attribute.getOrCreateAttribute(AttributeType.GENERIC_MOVEMENT_SPEED);

    if (controlState.sprinting()) {
      playerSpeedAttribute
        .modifiers()
        .putIfAbsent(
          physics.sprintingUUID,
          new Attribute.Modifier(
            physics.sprintingUUID, physics.sprintSpeed, ModifierOperation.ADD_MULTIPLIED_TOTAL));
    } else {
      // Client-side sprinting (don't rely on server-side sprinting)
      // setSprinting in LivingEntity.java
      playerSpeedAttribute.modifiers().remove(physics.sprintingUUID);
    }

    return (float) clientEntity.attributeValue(AttributeType.GENERIC_MOVEMENT_SPEED);
  }

  public void moveEntity(Level world, double dx, double dy, double dz) {
    var vel = movementState.vel;
    var pos = movementState.pos;

    var playerBB = clientEntity.boundingBox(pos.toImmutable());

    if (movementState.isInWeb) {
      dx *= 0.25;
      dy *= 0.05;
      dz *= 0.25;
      vel.x = 0;
      vel.y = 0;
      vel.z = 0;
      movementState.isInWeb = false;
    }

    var oldVelX = dx;
    var oldVelY = dy;
    var oldVelZ = dz;

    if (controlState.sneaking() && movementState.onGround) {
      var step = 0.05;

      // In the 3 loops bellow, y offset should be -1, but that doesnt reproduce vanilla behavior.
      for (; dx != 0 && world.getCollisionBoxes(playerBB.move(dx, 0, 0)).isEmpty(); oldVelX = dx) {
        if (dx < step && dx >= -step) {
          dx = 0;
        } else if (dx > 0) {
          dx -= step;
        } else {
          dx += step;
        }
      }

      for (; dz != 0 && world.getCollisionBoxes(playerBB.move(0, 0, dz)).isEmpty(); oldVelZ = dz) {
        if (dz < step && dz >= -step) {
          dz = 0;
        } else if (dz > 0) {
          dz -= step;
        } else {
          dz += step;
        }
      }

      while (dx != 0 && dz != 0 && world.getCollisionBoxes(playerBB.move(dx, 0, dz)).isEmpty()) {
        if (dx < step && dx >= -step) {
          dx = 0;
        } else if (dx > 0) {
          dx -= step;
        } else {
          dx += step;
        }

        if (dz < step && dz >= -step) {
          dz = 0;
        } else if (dz > 0) {
          dz -= step;
        } else {
          dz += step;
        }

        oldVelX = dx;
        oldVelZ = dz;
      }
    }

    var collisionResult = collide(world, playerBB, Vector3d.from(dx, dy, dz));
    dx = collisionResult.getX();
    dy = collisionResult.getY();
    dz = collisionResult.getZ();

    var resultingBB = playerBB.move(dx, dy, dz);

    // Update flags
    setPositionToBB(resultingBB, pos);
    movementState.isCollidedHorizontally = dx != oldVelX || dz != oldVelZ;
    movementState.isCollidedVertically = dy != oldVelY;
    movementState.onGround = movementState.isCollidedVertically && oldVelY < 0;

    // We collided, so we block the motion
    if (dx != oldVelX) {
      vel.x = 0;
    }

    if (dz != oldVelZ) {
      vel.z = 0;
    }

    if (dy != oldVelY) {
      var blockAtFeet = world.getBlockState(pos.offset(0, -0.2, 0).toImmutableInt());
      if (blockAtFeet.blockType() == BlockType.SLIME_BLOCK && !controlState.sneaking()) {
        vel.y = -vel.y;
      } else {
        vel.y = 0;
      }
    }

    // Finally, apply block collisions (web, soulsand...)
    consumeIntersectedBlocks(
      world,
      resultingBB.deflate(0.001, 0.001, 0.001),
      (block, cursor) -> {
        if (block.blockType() == BlockType.COBWEB) {
          movementState.isInWeb = true;
        } else if (block.blockType() == BlockType.BUBBLE_COLUMN) {
          var down = !block.properties().getBoolean("drag");
          var aboveBlock = world.getBlockState(cursor.add(0, 1, 0));
          var bubbleDrag =
            aboveBlock.blockType() == BlockType.AIR
              ? physics.bubbleColumnSurfaceDrag
              : physics.bubbleColumnDrag;
          if (down) {
            vel.y = Math.max(bubbleDrag.maxDown(), vel.y - bubbleDrag.down());
          } else {
            vel.y = Math.min(bubbleDrag.maxUp(), vel.y + bubbleDrag.up());
          }
        }
      });

    var blockBelow =
      world.getBlockState(movementState.pos.floored().offset(0, -0.5, 0).toImmutableInt());
    if (blockBelow.blockType() == BlockType.SOUL_SAND) {
      vel.x *= physics.soulsandSpeed;
      vel.z *= physics.soulsandSpeed;
    } else if (blockBelow.blockType() == BlockType.HONEY_BLOCK) {
      vel.x *= physics.honeyblockSpeed;
      vel.z *= physics.honeyblockSpeed;
    }
  }

  private Vector3d collide(Level world, AABB playerBB, Vector3d targetVec) {
    var initialCollisionVec =
      targetVec.lengthSquared() == 0.0
        ? targetVec
        : collideBoundingBox(world, targetVec, playerBB);
    var xChanged = targetVec.getX() != initialCollisionVec.getY();
    var yChanged = targetVec.getY() != initialCollisionVec.getY();
    var zChanged = targetVec.getZ() != initialCollisionVec.getZ();
    var collidedY = movementState.onGround || yChanged && targetVec.getY() < 0;

    // Step on block if height < stepHeight
    if (physics.stepHeight > 0 && collidedY && (xChanged || zChanged)) {
      var fullStep =
        collideBoundingBox(
          world,
          Vector3d.from(targetVec.getX(), physics.stepHeight, targetVec.getZ()),
          playerBB);
      var justStep =
        collideBoundingBox(
          world,
          Vector3d.from(0.0, physics.stepHeight, 0.0),
          playerBB.expandTowards(targetVec.getX(), 0.0, targetVec.getZ()));
      if (justStep.getY() < physics.stepHeight) {
        var justMove =
          collideBoundingBox(
            world,
            Vector3d.from(targetVec.getX(), 0.0, targetVec.getZ()),
            playerBB.move(justStep))
            .add(justStep);
        if (horizontalDistanceSqr(justMove) > horizontalDistanceSqr(fullStep)) {
          fullStep = justMove;
        }
      }

      if (horizontalDistanceSqr(fullStep) > horizontalDistanceSqr(initialCollisionVec)) {
        return fullStep.add(
          collideBoundingBox(
            world,
            Vector3d.from(0.0, -fullStep.getY() + targetVec.getY(), 0.0),
            playerBB.move(fullStep)));
      }
    }

    return initialCollisionVec;
  }

  public void applyHeading(double strafe, double forward, float speed) {
    var distanceSquared = strafe * strafe + forward * forward;
    if (distanceSquared < 1.0E-7) {
      return;
    }

    if (distanceSquared > 1) {
      var distance = Math.sqrt(distanceSquared);
      strafe /= distance;
      forward /= distance;
    }

    strafe *= speed;
    forward *= speed;

    var yawRadians = Math.toRadians(clientEntity.yaw());
    var sin = Math.sin(yawRadians);
    var cos = Math.cos(yawRadians);

    var vel = movementState.vel;
    vel.x += strafe * cos - forward * sin;
    vel.z += forward * cos + strafe * sin;
  }

  public boolean doesNotCollide(Level world, MutableVector3d pos) {
    var pBB = clientEntity.boundingBox(pos.toImmutable());
    return world.getCollisionBoxes(pBB).isEmpty() && getWaterInBB(world, pBB).isEmpty();
  }

  public int getLiquidHeightPercent(@Nullable BlockState meta) {
    return (getRenderedDepth(meta) + 1) / 9;
  }

  public int getRenderedDepth(@Nullable BlockState meta) {
    if (meta == null) {
      return -1;
    }

    if (WATER_LIKE_TYPES.contains(meta.blockType())) {
      return 0;
    }

    if (meta.properties().getBoolean("waterlogged")) {
      return 0;
    }

    if (!WATER_TYPES.contains(meta.blockType())) {
      return -1;
    }

    var level = meta.properties().getInt("level");
    return level >= 8 ? 0 : level;
  }

  public Vector3i getFlow(Level world, BlockState meta, Vector3i block) {
    var curlevel = getRenderedDepth(meta);
    var flow = new MutableVector3d(0, 0, 0);
    for (var combination :
      new int[][] {new int[] {0, 1}, new int[] {-1, 0}, new int[] {0, -1}, new int[] {1, 0}}) {
      var dx = combination[0];
      var dz = combination[1];
      var adjBlockVec = block.add(dx, 0, dz);
      var adjBlock = world.getBlockState(adjBlockVec);
      var adjLevel = getRenderedDepth(adjBlock);
      if (adjLevel < 0) {
        if (adjBlock.blockShapeGroup().hasNoCollisions()) {
          var adjLevel2Vec = block.add(dx, -1, dz);
          var adjLevel2 = getRenderedDepth(world.getBlockState(adjLevel2Vec));
          if (adjLevel2 >= 0) {
            var f = adjLevel2 - (curlevel - 8);
            flow.x += dx * f;
            flow.z += dz * f;
          }
        }
      } else {
        var f = adjLevel - curlevel;
        flow.x += dx * f;
        flow.z += dz * f;
      }
    }

    if (meta.properties().getInt("level") >= 8) {
      for (var combination :
        new int[][] {new int[] {0, 1}, new int[] {-1, 0}, new int[] {0, -1}, new int[] {1, 0}}) {
        var dx = combination[0];
        var dz = combination[1];
        var adjBlock = world.getBlockState(block.add(dx, 0, dz));
        var adjUpBlock = world.getBlockState(block.add(dx, 1, dz));
        if ((adjBlock.blockShapeGroup().hasNoCollisions())
          || (adjUpBlock.blockShapeGroup().hasNoCollisions())) {
          flow.normalize().translate(0, -6, 0);
        }
      }
    }

    return flow.normalize().toImmutableInt();
  }

  public List<Pair<Vector3i, BlockState>> getWaterInBB(Level world, AABB bb) {
    var waterBlocks = new ArrayList<Pair<Vector3i, BlockState>>();

    consumeIntersectedBlocks(
      world,
      bb,
      (block, cursor) -> {
        if (WATER_TYPES.contains(block.blockType())
          || WATER_LIKE_TYPES.contains(block.blockType())
          || block.properties().getBoolean("waterlogged")) {
          var waterLevel = cursor.getY() + 1 - getLiquidHeightPercent(block);
          if (Math.ceil(bb.maxY) >= waterLevel) {
            waterBlocks.add(Pair.of(cursor, block));
          }
        }
      });

    return waterBlocks;
  }

  public boolean isInWaterApplyCurrent(Level world, AABB bb, MutableVector3d vel) {
    var acceleration = new MutableVector3d(0, 0, 0);
    var waterBlocks = getWaterInBB(world, bb);
    var isInWater = !waterBlocks.isEmpty();
    for (var block : waterBlocks) {
      var flow = getFlow(world, block.right(), block.left());
      acceleration.add(flow);
    }

    var len = acceleration.norm();
    if (len > 0) {
      vel.x += acceleration.x / len * 0.014;
      vel.y += acceleration.y / len * 0.014;
      vel.z += acceleration.z / len * 0.014;
    }
    return isInWater;
  }

  private float getBlockFriction(BlockType blockType) {
    if (blockType == BlockType.SLIME_BLOCK) {
      return 0.8F;
    } else if (blockType == BlockType.ICE || blockType == BlockType.PACKED_ICE) {
      return 0.98F;
    } else if (blockType == BlockType.BLUE_ICE) {
      return 0.989F;
    } else {
      return physics.defaultSlipperiness; // Normal block
    }
  }

  record LookingVectorData(
    float yaw,
    float pitch,
    double sinYaw,
    double cosYaw,
    double sinPitch,
    double cosPitch,
    double lookX,
    double lookY,
    double lookZ,
    MutableVector3d lookDir) {}
}
