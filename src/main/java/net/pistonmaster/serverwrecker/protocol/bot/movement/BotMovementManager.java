/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.protocol.bot.movement;

import com.github.steveice10.mc.protocol.data.game.entity.RotationOrigin;
import com.github.steveice10.mc.protocol.data.game.entity.attribute.Attribute;
import com.github.steveice10.mc.protocol.data.game.entity.attribute.AttributeModifier;
import com.github.steveice10.mc.protocol.data.game.entity.attribute.AttributeType;
import com.github.steveice10.mc.protocol.data.game.entity.attribute.ModifierOperation;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerRotPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerStatusOnlyPacket;
import it.unimi.dsi.fastutil.Pair;
import lombok.Getter;
import net.pistonmaster.serverwrecker.data.BlockType;
import net.pistonmaster.serverwrecker.protocol.bot.SessionDataManager;
import net.pistonmaster.serverwrecker.protocol.bot.block.BlockStateMeta;
import net.pistonmaster.serverwrecker.protocol.bot.state.EntityAttributesState;
import net.pistonmaster.serverwrecker.protocol.bot.state.LevelState;
import net.pistonmaster.serverwrecker.util.MathHelper;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Java port of prismarine-physics
 */
public class BotMovementManager {
    @Getter
    private final ControlState controlState = new ControlState();
    private final PhysicsData physics = new PhysicsData();
    @Getter
    private final PlayerMovementState entity;
    private final SessionDataManager dataManager;
    private double lastX = 0;
    private double lastY = 0;
    private double lastZ = 0;
    private float lastYaw = 0;
    private float lastPitch = 0;
    private boolean lastOnGround = false;
    private int positionReminder = 0;

    private final List<BlockType> WATER_TYPES = List.of(BlockType.WATER);
    private final List<BlockType> LAVA_TYPES = List.of(BlockType.WATER);
    private final List<BlockType> WATER_LIKE_TYPES = List.of(
            BlockType.WATER, BlockType.SEAGRASS, BlockType.TALL_SEAGRASS,
            BlockType.KELP, BlockType.KELP_PLANT, BlockType.BUBBLE_COLUMN
    );

    public BotMovementManager(SessionDataManager dataManager, double x, double y, double z, float yaw, float pitch) {
        this.entity = new PlayerMovementState(
                dataManager.getSelfAttributeState(),
                dataManager.getSelfEffectState(),
                dataManager.getInventoryManager().getPlayerInventory()
        );
        entity.pos = new MutableVector3d(x, y, z);

        entity.yaw = yaw;
        entity.pitch = pitch;
        this.dataManager = dataManager;
    }

    public void tick() {
        var world = dataManager.getCurrentLevel();
        if (world == null) return;

        entity.updateData();

        var vel = entity.vel;
        var pos = entity.pos;

        {
            var playerBB = getPlayerBB(pos);
            var waterBB = playerBB.deflate(0.001, 0.401, 0.001);
            var lavaBB = playerBB.deflate(0.1, 0.4, 0.1);

            entity.isInWater = isInWaterApplyCurrent(world, waterBB, vel);
            entity.isInLava = isMaterialInBB(world, lavaBB, LAVA_TYPES);
        }

        // Reset velocity component if it falls under the threshold
        if (Math.abs(vel.x) < physics.negligeableVelocity) vel.x = 0;
        if (Math.abs(vel.y) < physics.negligeableVelocity) vel.y = 0;
        if (Math.abs(vel.z) < physics.negligeableVelocity) vel.z = 0;

        // Handle inputs
        if (controlState.isJumping() || entity.jumpQueued) {
            if (entity.jumpTicks > 0) {
                entity.jumpTicks--;
            }

            if (entity.isInWater || entity.isInLava) {
                vel.y += 0.04;
            } else if (entity.onGround && entity.jumpTicks == 0) {
                var blockBelow = world.getBlockStateAt(entity.pos.floored().offset(0, -0.5, 0).toImmutableInt());
                vel.y = (float) (0.42) * ((blockBelow.isPresent() && blockBelow.get().blockType() == BlockType.HONEY_BLOCK) ? physics.honeyblockJumpSpeed : 1);
                if (entity.jumpBoost > 0) {
                    vel.y += 0.1 * entity.jumpBoost;
                }

                if (controlState.isSprinting()) {
                    var yaw = Math.PI - entity.yaw;
                    vel.x -= Math.sin(yaw) * 0.2;
                    vel.z += Math.cos(yaw) * 0.2;
                }

                entity.jumpTicks = physics.autojumpCooldown;
            }
        } else {
            entity.jumpTicks = 0; // reset autojump cooldown
        }
        entity.jumpQueued = false;

        var forward = 0.0F;
        if (controlState.isForward()) {
            forward++;
        }

        if (controlState.isBackward()) {
            forward--;
        }

        var strafe = 0.0F;
        if (controlState.isRight()) {
            strafe++;
        }

        if (controlState.isLeft()) {
            strafe--;
        }

        strafe *= 0.98F;
        forward *= 0.98F;

        if (controlState.isSneaking()) {
            strafe *= (float) ((double) strafe * physics.sneakSpeed);
            forward *= (float) ((double) forward * physics.sneakSpeed);
        }

        entity.elytraFlying = entity.elytraFlying && entity.elytraEquipped && !entity.onGround && entity.levitation == 0;

        if (entity.fireworkRocketDuration > 0) {
            if (!entity.elytraFlying) {
                entity.fireworkRocketDuration = 0;
            } else {
                var lookingVector = getLookingVector(entity);
                var lookDir = lookingVector.lookDir;
                vel.x += lookDir.x * 0.1 + (lookDir.x * 1.5 - vel.x) * 0.5;
                vel.y += lookDir.y * 0.1 + (lookDir.y * 1.5 - vel.y) * 0.5;
                vel.z += lookDir.z * 0.1 + (lookDir.z * 1.5 - vel.z) * 0.5;
                --entity.fireworkRocketDuration;
            }
        }

        moveEntityWithHeading(world, strafe, forward);

        // Detect whether anything changed
        var xDiff = entity.pos.x - lastX;
        var yDiff = entity.pos.y - lastY;
        var zDiff = entity.pos.z - lastZ;
        var yawDiff = (double) (entity.yaw - lastYaw);
        var pitchDiff = (double) (entity.pitch - lastPitch);
        var sendPos = MathHelper.lengthSquared(xDiff, yDiff, zDiff) > MathHelper.square(2.0E-4) || ++positionReminder >= 20;
        var sendRot = pitchDiff != 0.0 || yawDiff != 0.0;
        var sendOnGround = entity.onGround != lastOnGround;

        // Send position packets if changed
        if (sendPos && sendRot) {
            sendPosRot();
        } else if (sendPos) {
            sendPos();
        } else if (sendRot) {
            sendRot();
        } else if (sendOnGround) {
            sendOnGround();
        }
    }

    public void sendPosRot() {
        var onGround = entity.onGround;

        lastOnGround = onGround;

        var x = entity.pos.x;
        var y = entity.pos.y;
        var z = entity.pos.z;

        lastX = x;
        lastY = y;
        lastZ = z;
        positionReminder = 0;

        var yaw = entity.yaw;
        var pitch = entity.pitch;

        lastYaw = yaw;
        lastPitch = pitch;

        dataManager.getSession().send(new ServerboundMovePlayerPosRotPacket(onGround, x, y, z, yaw, pitch));
    }

    public void sendPos() {
        var onGround = entity.onGround;

        lastOnGround = onGround;

        var x = entity.pos.x;
        var y = entity.pos.y;
        var z = entity.pos.z;

        lastX = x;
        lastY = y;
        lastZ = z;
        positionReminder = 0;

        dataManager.getSession().send(new ServerboundMovePlayerPosPacket(onGround, x, y, z));
    }

    public void sendRot() {
        var onGround = entity.onGround;

        lastOnGround = onGround;

        var yaw = entity.yaw;
        var pitch = entity.pitch;

        lastYaw = yaw;
        lastPitch = pitch;

        dataManager.getSession().send(new ServerboundMovePlayerRotPacket(onGround, yaw, pitch));
    }

    public void sendOnGround() {
        var onGround = entity.onGround;

        lastOnGround = onGround;

        dataManager.getSession().send(new ServerboundMovePlayerStatusOnlyPacket(onGround));
    }

    public void moveEntityWithHeading(LevelState world, float strafe, float forward) {
        var vel = entity.vel;
        var pos = entity.pos;

        var gravityMultiplier = (vel.y <= 0 && entity.slowFalling > 0) ? physics.slowFalling : 1;
        var speed = getSpeed();

        if (entity.isInWater || entity.isInLava) {
            // Water / Lava movement
            var lastY = pos.y;
            var liquidSpeed = physics.liquidSpeed;
            var typeSpeed = entity.isInWater ? physics.waterSpeed : physics.lavaSpeed;
            var horizontalInertia = typeSpeed;

            if (entity.isInWater) {
                var depthStrider = (float) entity.depthStrider;

                if (depthStrider > 3) {
                    depthStrider = 3;
                }

                if (!entity.onGround) {
                    depthStrider *= 0.5F;
                }

                if (depthStrider > 0) {
                    horizontalInertia += ((0.54600006F - horizontalInertia) * depthStrider) / 3;
                    liquidSpeed += (speed - liquidSpeed) * depthStrider / 3;
                }

                if (entity.dolphinsGrace > 0) {
                    horizontalInertia = 0.96F;
                }
            }

            applyHeading(strafe, forward, liquidSpeed);

            moveEntity(world, vel.x, vel.y, vel.z);
            vel.y *= typeSpeed;
            vel.y -= (entity.isInWater ? physics.waterGravity : physics.lavaGravity) * gravityMultiplier;
            vel.x *= horizontalInertia;
            vel.z *= horizontalInertia;

            if (entity.isCollidedHorizontally && doesNotCollide(world, pos.offset(vel.x, vel.y + 0.6 - pos.y + lastY, vel.z))) {
                vel.y = physics.outOfLiquidImpulse; // jump out of liquid
            }
        } else if (entity.elytraFlying) {
            var lookingData = getLookingVector(entity);
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

            if (entity.onGround) {
                entity.elytraFlying = false;
            }
        } else {
            // Normal movement
            var xzMultiplier = 0.91F;
            float frictionInfluencedSpeed;

            var blockUnder = world.getBlockStateAt(pos.offset(0, -0.500001F, 0).toImmutableInt());
            if (entity.onGround && blockUnder.isPresent()) {
                var friction = getBlockFriction(blockUnder.get().blockType());
                xzMultiplier *= friction;
                frictionInfluencedSpeed = speed * (0.21600002F / (friction * friction * friction));
            } else {
                frictionInfluencedSpeed = getFlyingSpeed();
            }

            applyHeading(strafe, forward, frictionInfluencedSpeed);

            if (isOnLadder(world, pos.toImmutableInt())) {
                vel.x = GenericMath.clamp(-physics.ladderMaxSpeed, vel.x, physics.ladderMaxSpeed);
                vel.z = GenericMath.clamp(-physics.ladderMaxSpeed, vel.z, physics.ladderMaxSpeed);
                vel.y = Math.max(vel.y, controlState.isSneaking() ? 0 : -physics.ladderMaxSpeed);
            }

            moveEntity(world, vel.x, vel.y, vel.z);

            if ((entity.isCollidedHorizontally || controlState.isJumping()) && isOnLadder(world, pos.toImmutableInt())) {
                vel.y = physics.ladderClimbSpeed; // climb ladder
            }

            // Apply friction and gravity
            if (entity.levitation > 0) {
                vel.y += (0.05 * entity.levitation - vel.y) * 0.2;
            } else {
                vel.y -= physics.gravity * gravityMultiplier;
            }

            vel.x *= xzMultiplier;
            vel.y *= physics.airdrag;
            vel.z *= xzMultiplier;
        }
    }

    private float getFlyingSpeed() {
        if (entity.flying) {
            var abilitiesData = dataManager.getAbilitiesData();
            var flySpeed = abilitiesData == null ? 0.05F : abilitiesData.flySpeed();
            return controlState.isSprinting() ? flySpeed * 2.0F : flySpeed;
        } else {
            return controlState.isSprinting() ? 0.025999999F : 0.02F;
        }
    }

    public float getSpeed() {
        var attribute = entity.getAttributesState();
        Attribute playerSpeedAttribute;
        if (attribute.hasAttribute(AttributeType.Builtin.GENERIC_MOVEMENT_SPEED)) {
            // Use server-side player attributes
            playerSpeedAttribute = attribute.getAttribute(AttributeType.Builtin.GENERIC_MOVEMENT_SPEED);
        } else {
            // Create an attribute if the player does not have it
            playerSpeedAttribute = new Attribute(AttributeType.Builtin.GENERIC_MOVEMENT_SPEED, physics.playerSpeed);
        }

        if (controlState.isSprinting()) {
            if (playerSpeedAttribute.getModifiers().stream().noneMatch(modifier ->
                    modifier.getUuid().equals(physics.sprintingUUID))) {
                playerSpeedAttribute.getModifiers().add(new AttributeModifier(
                        physics.sprintingUUID,
                        physics.sprintSpeed,
                        ModifierOperation.MULTIPLY
                ));
            }
        } else {
            // Client-side sprinting (don't rely on server-side sprinting)
            // setSprinting in LivingEntity.java
            playerSpeedAttribute.getModifiers().removeIf(modifier ->
                    modifier.getUuid().equals(physics.sprintingUUID));
        }

        return (float) EntityAttributesState.getAttributeValue(playerSpeedAttribute);
    }

    public void moveEntity(LevelState world, double dx, double dy, double dz) {
        var vel = entity.vel;
        var pos = entity.pos;

        var playerBB = getPlayerBB(pos);

        if (entity.isInWeb) {
            dx *= 0.25;
            dy *= 0.05;
            dz *= 0.25;
            vel.x = 0;
            vel.y = 0;
            vel.z = 0;
            entity.isInWeb = false;
        }

        var oldVelX = dx;
        var oldVelY = dy;
        var oldVelZ = dz;

        if (controlState.isSneaking() && entity.onGround) {
            var step = 0.05;

            // In the 3 loops bellow, y offset should be -1, but that doesnt reproduce vanilla behavior.
            for (; dx != 0 && getSurroundingBBs(world, playerBB.move(dx, 0, 0)).isEmpty(); oldVelX = dx) {
                if (dx < step && dx >= -step) dx = 0;
                else if (dx > 0) dx -= step;
                else dx += step;
            }

            for (; dz != 0 && getSurroundingBBs(world, playerBB.move(0, 0, dz)).isEmpty(); oldVelZ = dz) {
                if (dz < step && dz >= -step) dz = 0;
                else if (dz > 0) dz -= step;
                else dz += step;
            }

            while (dx != 0 && dz != 0 && getSurroundingBBs(world, playerBB.move(dx, 0, dz)).isEmpty()) {
                if (dx < step && dx >= -step) dx = 0;
                else if (dx > 0) dx -= step;
                else dx += step;

                if (dz < step && dz >= -step) dz = 0;
                else if (dz > 0) dz -= step;
                else dz += step;

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
        entity.isCollidedHorizontally = dx != oldVelX || dz != oldVelZ;
        entity.isCollidedVertically = dy != oldVelY;
        entity.onGround = entity.isCollidedVertically && oldVelY < 0;

        // We collided, so we block the motion
        if (dx != oldVelX) {
            vel.x = 0;
        }

        if (dz != oldVelZ) {
            vel.z = 0;
        }

        if (dy != oldVelY) {
            var blockAtFeet = world.getBlockStateAt(pos.offset(0, -0.2, 0).toImmutableInt());
            if (blockAtFeet.isPresent()
                    && blockAtFeet.get().blockType() == BlockType.SLIME_BLOCK
                    && !controlState.isSneaking()) {
                vel.y = -vel.y;
            } else {
                vel.y = 0;
            }
        }

        // Finally, apply block collisions (web, soulsand...)
        consumeCollisionBlocks(world, resultingBB.deflate(0.001, 0.001, 0.001), (block, cursor) -> {
            if (block.blockType() == BlockType.COBWEB) {
                entity.isInWeb = true;
            } else if (block.blockType() == BlockType.BUBBLE_COLUMN) {
                var down = !block.blockShapeType().properties().getBoolean("drag");
                var aboveBlock = world.getBlockStateAt(cursor.add(0, 1, 0));
                var bubbleDrag = (aboveBlock.isPresent() && aboveBlock.get().blockType() == BlockType.AIR) ?
                        physics.bubbleColumnSurfaceDrag : physics.bubbleColumnDrag;
                if (down) {
                    vel.y = Math.max(bubbleDrag.maxDown(), vel.y - bubbleDrag.down());
                } else {
                    vel.y = Math.min(bubbleDrag.maxUp(), vel.y + bubbleDrag.up());
                }
            }
        });

        var blockBelow = world.getBlockStateAt(entity.pos.floored().offset(0, -0.5, 0).toImmutableInt());
        if (blockBelow.isPresent()) {
            if (blockBelow.get().blockType() == BlockType.SOUL_SAND) {
                vel.x *= physics.soulsandSpeed;
                vel.z *= physics.soulsandSpeed;
            } else if (blockBelow.get().blockType() == BlockType.HONEY_BLOCK) {
                vel.x *= physics.honeyblockSpeed;
                vel.z *= physics.honeyblockSpeed;
            }
        }
    }

    private static void consumeCollisionBlocks(LevelState world, AABB queryBB, BiConsumer<BlockStateMeta, Vector3i> consumer) {
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
                    var block = world.getBlockStateAt(cursor);
                    if (block.isEmpty()) {
                        continue;
                    }

                    consumer.accept(block.get(), cursor);
                }
            }
        }
    }

    private static List<AABB> getCollisionBoxes(LevelState world, AABB queryBB) {
        var startX = MathHelper.floorDouble(queryBB.minX - 1.0E-7) - 1;
        var endX = MathHelper.floorDouble(queryBB.maxX + 1.0E-7) + 1;
        var startY = MathHelper.floorDouble(queryBB.minY - 1.0E-7) - 1;
        var endY = MathHelper.floorDouble(queryBB.maxY + 1.0E-7) + 1;
        var startZ = MathHelper.floorDouble(queryBB.minZ - 1.0E-7) - 1;
        var endZ = MathHelper.floorDouble(queryBB.maxZ + 1.0E-7) + 1;

        var predictedSize = (endX - startX + 1) * (endY - startY + 1) * (endZ - startZ + 1);
        var collisionBoxes = new ArrayList<AABB>(predictedSize);
        for (var x = startX; x <= endX; x++) {
            for (var y = startY; y <= endY; y++) {
                for (var z = startZ; z <= endZ; z++) {
                    var cursor = Vector3i.from(x, y, z);
                    var block = world.getBlockStateAt(cursor);
                    if (block.isEmpty()) {
                        continue;
                    }

                    for (var shape : block.get().blockShapeType().blockShapes()) {
                        var shapeBB = shape.createAABBAt(x, y, z);

                        if (shapeBB.intersects(queryBB)) {
                            collisionBoxes.add(shapeBB);
                        }
                    }
                }
            }
        }

        return collisionBoxes;
    }

    private Vector3d collide(LevelState world, AABB playerBB, Vector3d targetVec) {
        var initialCollisionVec = targetVec.lengthSquared() == 0.0 ? targetVec : collideBoundingBox(world, targetVec, playerBB);
        var xChanged = targetVec.getX() != initialCollisionVec.getY();
        var yChanged = targetVec.getY() != initialCollisionVec.getY();
        var zChanged = targetVec.getZ() != initialCollisionVec.getZ();
        var collidedY = entity.onGround || yChanged && targetVec.getY() < 0;

        // Step on block if height < stepHeight
        if (physics.stepHeight > 0 && collidedY && (xChanged || zChanged)) {
            var fullStep = collideBoundingBox(world, Vector3d.from(targetVec.getX(), physics.stepHeight, targetVec.getZ()), playerBB);
            var justStep = collideBoundingBox(world, Vector3d.from(0.0, physics.stepHeight, 0.0), playerBB.expandTowards(targetVec.getX(), 0.0, targetVec.getZ()));
            if (justStep.getY() < physics.stepHeight) {
                var justMove = collideBoundingBox(world, Vector3d.from(targetVec.getX(), 0.0, targetVec.getZ()), playerBB.move(justStep)).add(justStep);
                if (horizontalDistanceSqr(justMove) > horizontalDistanceSqr(fullStep)) {
                    fullStep = justMove;
                }
            }

            if (horizontalDistanceSqr(fullStep) > horizontalDistanceSqr(initialCollisionVec)) {
                return fullStep.add(collideBoundingBox(world, Vector3d.from(0.0, -fullStep.getY() + targetVec.getY(), 0.0), playerBB.move(fullStep)));
            }
        }

        return initialCollisionVec;
    }

    private static double horizontalDistanceSqr(Vector3d vec) {
        return vec.getX() * vec.getX() + vec.getZ() * vec.getZ();
    }

    public static Vector3d collideBoundingBox(LevelState world, Vector3d targetVec, AABB queryBB) {
        return collideWith(targetVec, queryBB, getCollisionBoxes(world, queryBB.expandTowards(targetVec)));
    }

    private static Vector3d collideWith(Vector3d direction, AABB boundingBox, List<AABB> collisionBoxes) {
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

        var yawRadians = Math.toRadians(entity.yaw);
        var sin = Math.sin(yawRadians);
        var cos = Math.cos(yawRadians);

        var vel = entity.vel;
        vel.x += strafe * cos - forward * sin;
        vel.z += forward * cos + strafe * sin;
    }

    private LookingVectorData getLookingVector(PlayerMovementState entity) {
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

        var yaw = entity.yaw;
        var pitch = entity.pitch;
        var sinYaw = Math.sin(yaw);
        var cosYaw = Math.cos(yaw);
        var sinPitch = Math.sin(pitch);
        var cosPitch = Math.cos(pitch);
        var lookX = -sinYaw * cosPitch;
        var lookZ = -cosYaw * cosPitch;
        var lookDir = new MutableVector3d(lookX, sinPitch, lookZ);
        return new LookingVectorData(
                yaw,
                pitch,
                sinYaw,
                cosYaw,
                sinPitch,
                cosPitch,
                lookX,
                sinPitch,
                lookZ,
                lookDir
        );
    }

    public void setMotion(double motionX, double motionY, double motionZ) {
        entity.vel.x = motionX;
        entity.vel.y = motionY;
        entity.vel.z = motionZ;
    }

    public Vector3i getBlockPos() {
        return entity.pos.toImmutableInt();
    }

    public void setPosition(double x, double y, double z) {
        entity.pos.x = x;
        entity.pos.y = y;
        entity.pos.z = z;
    }

    public void setRotation(float yaw, float pitch) {
        entity.yaw = yaw;
        entity.pitch = pitch;
    }

    public Vector3d getPlayerPos() {
        return entity.pos.toImmutable();
    }

    public float getYaw() {
        return entity.yaw;
    }

    public float getPitch() {
        return entity.pitch;
    }

    public Vector3d getRotationVector() {
        var yawRadians = (float) Math.toRadians(entity.yaw);
        var pitchRadians = (float) Math.toRadians(entity.pitch);
        var x = -Math.sin(yawRadians) * Math.cos(pitchRadians);
        var y = -Math.sin(pitchRadians);
        var z = Math.cos(yawRadians) * Math.cos(pitchRadians);
        return Vector3d.from(x, y, z);
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
            MutableVector3d lookDir
    ) {
    }

    public boolean isOnLadder(LevelState world, Vector3i pos) {
        var block = world.getBlockStateAt(pos);
        if (block.isEmpty()) {
            return false;
        }

        var blockType = block.get().blockType();
        return blockType == BlockType.LADDER || blockType == BlockType.VINE;
    }

    public boolean doesNotCollide(LevelState world, MutableVector3d pos) {
        var pBB = getPlayerBB(pos);
        return getSurroundingBBs(world, pBB)
                .stream()
                .noneMatch(pBB::intersects)
                && getWaterInBB(world, pBB).isEmpty();
    }

    public int getLiquidHeightPercent(@Nullable BlockStateMeta meta) {
        return (getRenderedDepth(meta) + 1) / 9;
    }

    public int getRenderedDepth(@Nullable BlockStateMeta meta) {
        if (meta == null) return -1;

        if (WATER_LIKE_TYPES.contains(meta.blockType())) return 0;

        if (meta.blockShapeType().properties().getBoolean("waterlogged")) return 0;

        if (!WATER_TYPES.contains(meta.blockType())) return -1;

        var level = meta.blockShapeType().properties().getInt("level");
        return level >= 8 ? 0 : level;
    }

    public Vector3i getFlow(LevelState world, BlockStateMeta meta, Vector3i block) {
        var curlevel = getRenderedDepth(meta);
        var flow = new MutableVector3d(0, 0, 0);
        for (var combination : new int[][]{new int[]{0, 1}, new int[]{-1, 0}, new int[]{0, -1}, new int[]{1, 0}}) {
            var dx = combination[0];
            var dz = combination[1];
            var adjBlockVec = block.add(dx, 0, dz);
            var adjBlock = world.getBlockStateAt(adjBlockVec);
            var adjLevel = getRenderedDepth(adjBlock.orElse(null));
            if (adjLevel < 0) {
                if (adjBlock.isPresent() && adjBlock.get().blockShapeType().isEmpty()) {
                    var adjLevel2Vec = block.add(dx, -1, dz);
                    var adjLevel2 = getRenderedDepth(world.getBlockStateAt(adjLevel2Vec).orElse(null));
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

        if (meta.blockShapeType().properties().getInt("level") >= 8) {
            for (var combination : new int[][]{new int[]{0, 1}, new int[]{-1, 0}, new int[]{0, -1}, new int[]{1, 0}}) {
                var dx = combination[0];
                var dz = combination[1];
                var adjBlock = world.getBlockStateAt(block.add(dx, 0, dz));
                var adjUpBlock = world.getBlockStateAt(block.add(dx, 1, dz));
                if ((adjBlock.isPresent() && adjBlock.get().blockShapeType().isEmpty())
                        || (adjUpBlock.isPresent() && adjUpBlock.get().blockShapeType().isEmpty())) {
                    flow.normalize().translate(0, -6, 0);
                }
            }
        }

        return flow.normalize().toImmutableInt();
    }

    public List<AABB> getSurroundingBBs(LevelState world, AABB queryBB) {
        var surroundingBBs = new ArrayList<AABB>();

        var minX = MathHelper.floorDouble(queryBB.minX);
        var minY = MathHelper.floorDouble(queryBB.minY - 1);
        var minZ = MathHelper.floorDouble(queryBB.minZ);

        var maxX = MathHelper.floorDouble(queryBB.maxX);
        var maxY = MathHelper.floorDouble(queryBB.maxY);
        var maxZ = MathHelper.floorDouble(queryBB.maxZ);

        for (var x = minX; x <= maxX; x++) {
            for (var y = minY; y <= maxY; y++) {
                for (var z = minZ; z <= maxZ; z++) {
                    var blockState = world.getBlockStateAt(Vector3i.from(x, y, z));
                    if (blockState.isEmpty()) {
                        continue;
                    }

                    var blockShapeType = blockState.get().blockShapeType();
                    if (blockShapeType.hasNoCollisions()) {
                        continue;
                    }

                    for (var shape : blockShapeType.blockShapes()) {
                        surroundingBBs.add(shape.createAABBAt(x, y, z));
                    }
                }
            }
        }

        return surroundingBBs;
    }

    public boolean isMaterialInBB(LevelState world, AABB queryBB, List<BlockType> types) {
        var minX = MathHelper.floorDouble(queryBB.minX);
        var minY = MathHelper.floorDouble(queryBB.minY);
        var minZ = MathHelper.floorDouble(queryBB.minZ);

        var maxX = MathHelper.floorDouble(queryBB.maxX);
        var maxY = MathHelper.floorDouble(queryBB.maxY);
        var maxZ = MathHelper.floorDouble(queryBB.maxZ);

        for (var x = minX; x <= maxX; x++) {
            for (var y = minY; y <= maxY; y++) {
                for (var z = minZ; z <= maxZ; z++) {
                    var block = world.getBlockStateAt(Vector3i.from(x, y, z));

                    if (block.isEmpty()) {
                        continue;
                    }

                    if (types.contains(block.get().blockType())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public List<Pair<Vector3i, BlockStateMeta>> getWaterInBB(LevelState world, AABB bb) {
        var waterBlocks = new ArrayList<Pair<Vector3i, BlockStateMeta>>();

        consumeCollisionBlocks(world, bb, (block, cursor) -> {
            if (WATER_TYPES.contains(block.blockType())
                    || WATER_LIKE_TYPES.contains(block.blockType())
                    || block.blockShapeType().properties().getBoolean("waterlogged")) {
                var waterLevel = cursor.getY() + 1 - getLiquidHeightPercent(block);
                if (Math.ceil(bb.maxY) >= waterLevel) {
                    waterBlocks.add(Pair.of(cursor, block));
                }
            }
        });

        return waterBlocks;
    }

    public boolean isInWaterApplyCurrent(LevelState world, AABB bb, MutableVector3d vel) {
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

    public AABB getPlayerBB(MutableVector3d pos) {
        var w = physics.playerWidth / 2F;
        var h = getBoundingBoxHeight();
        return new AABB(pos.x - w, pos.y, pos.z - w, pos.x + w, pos.y + h, pos.z + w);
    }

    public void setPositionToBB(AABB bb, MutableVector3d pos) {
        pos.x = (bb.minX + bb.maxX) / 2;
        pos.y = bb.minY;
        pos.z = (bb.minZ + bb.maxZ) / 2;
    }

    public void jump() {
        entity.jumpQueued = true;
    }

    /**
     * Updates the rotation to look at a given block or location.
     *
     * @param origin The rotation origin, either EYES or FEET.
     * @param block  The block or location to look at.
     */
    public void lookAt(RotationOrigin origin, Vector3d block) {
        var eyes = origin == RotationOrigin.EYES;

        var dx = block.getX() - entity.pos.x;
        var dy = block.getY() - (eyes ? entity.pos.y + getEyeHeight() : entity.pos.y);
        var dz = block.getZ() - entity.pos.z;

        var r = Math.sqrt(dx * dx + dy * dy + dz * dz);
        var yaw = -Math.atan2(dx, dz) / Math.PI * 180;
        if (yaw < 0) {
            yaw = 360 + yaw;
        }

        var pitch = -Math.asin(dy / r) / Math.PI * 180;

        entity.yaw = (float) yaw;
        entity.pitch = (float) pitch;
    }

    public float getBoundingBoxHeight() {
        return this.controlState.isSneaking() ? physics.playerSneakHeight : physics.playerHeight;
    }

    public Vector3d getEyePosition() {
        return Vector3d.from(entity.pos.x, entity.pos.y + getEyeHeight(), entity.pos.z);
    }

    public float getEyeHeight() {
        return this.controlState.isSneaking() ? 1.50F : 1.62F;
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
}
