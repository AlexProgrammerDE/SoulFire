package net.pistonmaster.serverwrecker.protocol.bot.movement;

import com.github.steveice10.mc.protocol.data.game.entity.attribute.Attribute;
import com.github.steveice10.mc.protocol.data.game.entity.attribute.AttributeModifier;
import com.github.steveice10.mc.protocol.data.game.entity.attribute.AttributeType;
import com.github.steveice10.mc.protocol.data.game.entity.attribute.ModifierOperation;
import it.unimi.dsi.fastutil.Pair;
import net.pistonmaster.serverwrecker.data.BlockType;
import net.pistonmaster.serverwrecker.protocol.bot.block.BlockStateMeta;
import net.pistonmaster.serverwrecker.protocol.bot.state.EntityAttributesState;
import net.pistonmaster.serverwrecker.protocol.bot.state.LevelState;
import org.cloudburstmc.math.vector.Vector3i;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Java port of
 */
public class BotMovementManagerV2 {
    private final ControlState controlState = new ControlState();
    private final PhysicsData physics = new PhysicsData();

    public double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(val, max));
    }

    private final List<BlockType> WATER_TYPES = List.of(BlockType.WATER);
    private final List<BlockType> LAVA_TYPES = List.of(BlockType.WATER);
    private final List<BlockType> WATER_LIKE_TYPES = List.of(
            BlockType.WATER, BlockType.SEAGRASS, BlockType.TALL_SEAGRASS,
            BlockType.KELP, BlockType.KELP_PLANT, BlockType.BUBBLE_COLUMN
    );

    public AABB getPlayerBB(MutableVector3d pos) {
        var w = physics.playerHalfWidth;
        return new AABB(-w, 0, -w, w, physics.playerHeight, w).offset(pos.x, pos.y, pos.z);
    }

    public void setPositionToBB(AABB bb, MutableVector3d pos) {
        pos.x = bb.minX + physics.playerHalfWidth;
        pos.y = bb.minY;
        pos.z = bb.minZ + physics.playerHalfWidth;
    }

    public List<AABB> getSurroundingBBs(LevelState world, AABB queryBB) {
        var surroundingBBs = new ArrayList<AABB>();
        var cursor = new MutableVector3d(0, 0, 0);
        for (cursor.y = Math.floor(queryBB.minY) - 1; cursor.y <= Math.floor(queryBB.maxY); cursor.y++) {
            for (cursor.z = Math.floor(queryBB.minZ); cursor.z <= Math.floor(queryBB.maxZ); cursor.z++) {
                for (cursor.x = Math.floor(queryBB.minX); cursor.x <= Math.floor(queryBB.maxX); cursor.x++) {
                    var blockPos = cursor.toImmutableInt();
                    var block = world.getBlockStateAt(cursor.toImmutableInt());
                    if (block.isPresent()) {
                        for (var shape : block.get().blockShapeType().blockShapes()) {
                            var blockBB = new AABB(shape.minX(), shape.minY(), shape.minZ(), shape.maxX(), shape.maxY(), shape.maxZ());
                            blockBB.offset(blockPos.getX(), blockPos.getY(), blockPos.getZ());
                            surroundingBBs.add(blockBB);
                        }
                    }
                }
            }
        }
        return surroundingBBs;
    }

    public void adjustPositionHeight(LevelState world, MutableVector3d pos) {
        var playerBB = getPlayerBB(pos);
        var queryBB = playerBB.copy().extend(0, -1, 0);
        var surroundingBBs = getSurroundingBBs(world, queryBB);

        var dy = -1D;
        for (var blockBB : surroundingBBs) {
            dy = blockBB.computeOffsetY(playerBB, dy);
        }
        pos.y += dy;
    }

    public void moveEntity(PlayerMovementState entity, LevelState world, double dx, double dy, double dz) {
        var vel = entity.vel;
        var pos = entity.pos;

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
            for (; dx != 0 && getSurroundingBBs(world, getPlayerBB(pos).offset(dx, 0, 0)).isEmpty(); oldVelX = dx) {
                if (dx < step && dx >= -step) dx = 0;
                else if (dx > 0) dx -= step;
                else dx += step;
            }

            for (; dz != 0 && getSurroundingBBs(world, getPlayerBB(pos).offset(0, 0, dz)).isEmpty(); oldVelZ = dz) {
                if (dz < step && dz >= -step) dz = 0;
                else if (dz > 0) dz -= step;
                else dz += step;
            }

            while (dx != 0 && dz != 0 && getSurroundingBBs(world, getPlayerBB(pos).offset(dx, 0, dz)).isEmpty()) {
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

        var playerBB = getPlayerBB(pos);
        var queryBB = playerBB.copy().extend(dx, dy, dz);
        var surroundingBBs = getSurroundingBBs(world, queryBB);
        var oldBB = playerBB.copy();

        for (var blockBB : surroundingBBs) {
            dy = blockBB.computeOffsetY(playerBB, dy);
        }
        playerBB.offset(0, dy, 0);

        for (var blockBB : surroundingBBs) {
            dx = blockBB.computeOffsetX(playerBB, dx);
        }
        playerBB.offset(dx, 0, 0);

        for (var blockBB : surroundingBBs) {
            dz = blockBB.computeOffsetZ(playerBB, dz);
        }
        playerBB.offset(0, 0, dz);

        // Step on block if height < stepHeight
        if (physics.stepHeight > 0 &&
                (entity.onGround || (dy != oldVelY && oldVelY < 0)) &&
                (dx != oldVelX || dz != oldVelZ)) {
            var oldVelXCol = dx;
            var oldVelYCol = dy;
            var oldVelZCol = dz;
            var oldBBCol = playerBB.copy();

            dy = physics.stepHeight;
            var queryBB2 = oldBB.copy().extend(oldVelX, dy, oldVelZ);
            var surroundingBBs2 = getSurroundingBBs(world, queryBB2);

            var BB1 = oldBB.copy();
            var BB2 = oldBB.copy();
            var BB_XZ = BB1.copy().extend(dx, 0, dz);

            var dy1 = dy;
            var dy2 = dy;
            for (var blockBB : surroundingBBs2) {
                dy1 = blockBB.computeOffsetY(BB_XZ, dy1);
                dy2 = blockBB.computeOffsetY(BB2, dy2);
            }
            BB1.offset(0, dy1, 0);
            BB2.offset(0, dy2, 0);

            var dx1 = oldVelX;
            var dx2 = oldVelX;
            for (var blockBB : surroundingBBs2) {
                dx1 = blockBB.computeOffsetX(BB1, dx1);
                dx2 = blockBB.computeOffsetX(BB2, dx2);
            }
            BB1.offset(dx1, 0, 0);
            BB2.offset(dx2, 0, 0);

            var dz1 = oldVelZ;
            var dz2 = oldVelZ;
            for (var blockBB : surroundingBBs2) {
                dz1 = blockBB.computeOffsetZ(BB1, dz1);
                dz2 = blockBB.computeOffsetZ(BB2, dz2);
            }
            BB1.offset(0, 0, dz1);
            BB2.offset(0, 0, dz2);

            var norm1 = dx1 * dx1 + dz1 * dz1;
            var norm2 = dx2 * dx2 + dz2 * dz2;

            if (norm1 > norm2) {
                dx = dx1;
                dy = -dy1;
                dz = dz1;
                playerBB = BB1;
            } else {
                dx = dx2;
                dy = -dy2;
                dz = dz2;
                playerBB = BB2;
            }

            for (var blockBB : surroundingBBs2) {
                dy = blockBB.computeOffsetY(playerBB, dy);
            }
            playerBB.offset(0, dy, 0);

            if (oldVelXCol * oldVelXCol + oldVelZCol * oldVelZCol >= dx * dx + dz * dz) {
                dx = oldVelXCol;
                dy = oldVelYCol;
                dz = oldVelZCol;
                playerBB = oldBBCol;
            }
        }

        // Update flags
        setPositionToBB(playerBB, pos);
        entity.isCollidedHorizontally = dx != oldVelX || dz != oldVelZ;
        entity.isCollidedVertically = dy != oldVelY;
        entity.onGround = entity.isCollidedVertically && oldVelY < 0;

        var blockAtFeet = world.getBlockStateAt(pos.offset(0, -0.2, 0).toImmutableInt());

        if (dx != oldVelX) vel.x = 0;
        if (dz != oldVelZ) vel.z = 0;
        if (dy != oldVelY) {
            if (blockAtFeet.isPresent() && blockAtFeet.get().blockType() == BlockType.SLIME_BLOCK && !controlState.isSneaking()) {
                vel.y = -vel.y;
            } else {
                vel.y = 0;
            }
        }

        // Finally, apply block collisions (web, soulsand...)
        playerBB.contract(0.001, 0.001, 0.001);
        var cursor = new MutableVector3d(0, 0, 0);
        for (cursor.y = Math.floor(playerBB.minY); cursor.y <= Math.floor(playerBB.maxY); cursor.y++) {
            for (cursor.z = Math.floor(playerBB.minZ); cursor.z <= Math.floor(playerBB.maxZ); cursor.z++) {
                for (cursor.x = Math.floor(playerBB.minX); cursor.x <= Math.floor(playerBB.maxX); cursor.x++) {
                    var block = world.getBlockStateAt(cursor.toImmutableInt());
                    if (block.isPresent()) {
                        if (block.get().blockType() == BlockType.COBWEB) {
                            entity.isInWeb = true;
                        } else if (block.get().blockType() == BlockType.BUBBLE_COLUMN) {
                            var down = !block.get().blockShapeType().properties().getBoolean("drag");
                            var aboveBlock = world.getBlockStateAt(cursor.offset(0, 1, 0).toImmutableInt());
                            var bubbleDrag = (aboveBlock.isPresent() && aboveBlock.get().blockType() == BlockType.AIR) ?
                                    physics.bubbleColumnSurfaceDrag : physics.bubbleColumnDrag;
                            if (down) {
                                vel.y = Math.max(bubbleDrag.maxDown(), vel.y - bubbleDrag.down());
                            } else {
                                vel.y = Math.min(bubbleDrag.maxUp(), vel.y + bubbleDrag.up());
                            }
                        }
                    }
                }
            }
        }

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

    public void applyHeading(PlayerMovementState entity, double strafe, double forward, double multiplier) {
        var speed = Math.sqrt(strafe * strafe + forward * forward);
        if (speed < 0.01) return;

        speed = multiplier / Math.max(speed, 1);

        strafe *= speed;
        forward *= speed;

        var yaw = Math.PI - entity.yaw;
        var sin = Math.sin(yaw);
        var cos = Math.cos(yaw);

        var vel = entity.vel;
        vel.x -= strafe * cos + forward * sin;
        vel.z += forward * cos - strafe * sin;
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
        return getSurroundingBBs(world, pBB).stream().noneMatch(pBB::intersects) && getWaterInBB(world, pBB).isEmpty();
    }

    public void moveEntityWithHeading(PlayerMovementState entity, LevelState world, double strafe, double forward) {
        var vel = entity.vel;
        var pos = entity.pos;

        var gravityMultiplier = (vel.y <= 0 && entity.slowFalling > 0) ? physics.slowFalling : 1;

        if (entity.isInWater || entity.isInLava) {
            // Water / Lava movement
            var lastY = pos.y;
            var acceleration = physics.liquidAcceleration;
            var inertia = entity.isInWater ? physics.waterInertia : physics.lavaInertia;
            var horizontalInertia = inertia;

            if (entity.isInWater) {
                double strider = Math.min(entity.depthStrider, 3);
                if (!entity.onGround) {
                    strider *= 0.5;
                }
                if (strider > 0) {
                    horizontalInertia += (0.546 - horizontalInertia) * strider / 3;
                    acceleration += (0.7 - acceleration) * strider / 3;
                }

                if (entity.dolphinsGrace > 0) horizontalInertia = 0.96;
            }

            applyHeading(entity, strafe, forward, acceleration);
            moveEntity(entity, world, vel.x, vel.y, vel.z);
            vel.y *= inertia;
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
            moveEntity(entity, world, vel.x, vel.y, vel.z);

            if (entity.onGround) {
                entity.elytraFlying = false;
            }
        } else {
            // Normal movement
            var acceleration = 0.0;
            var inertia = 0.0;
            var blockUnder = world.getBlockStateAt(pos.offset(0, -1, 0).toImmutableInt());
            if (entity.onGround && blockUnder.isPresent()) {
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
                    if (playerSpeedAttribute.getModifiers().stream().noneMatch(modifier -> modifier.getUuid().equals(physics.sprintingUUID))) {
                        playerSpeedAttribute.getModifiers().add(new AttributeModifier(
                                physics.sprintingUUID,
                                physics.sprintSpeed,
                                ModifierOperation.MULTIPLY
                        ));
                    }
                } else {
                    // Client-side sprinting (don't rely on server-side sprinting)
                    // setSprinting in LivingEntity.java
                    playerSpeedAttribute.getModifiers().removeIf(modifier -> modifier.getUuid().equals(physics.sprintingUUID));
                }

                // Calculate what the speed is (0.1 if no modification)
                var attributeSpeed = EntityAttributesState.getAttributeValue(playerSpeedAttribute);
                inertia = getBlockSlipperiness(blockUnder.get().blockType()) * 0.91;
                acceleration = attributeSpeed * (0.1627714 / (inertia * inertia * inertia));
                if (acceleration < 0) acceleration = 0; // acceleration should not be negative
            } else {
                acceleration = physics.airborneAcceleration;
                inertia = physics.airborneInertia;

                if (controlState.isSprinting()) {
                    var airSprintFactor = physics.airborneAcceleration * 0.3;
                    acceleration += airSprintFactor;
                }
            }

            applyHeading(entity, strafe, forward, acceleration);

            if (isOnLadder(world, pos.toImmutableInt())) {
                vel.x = clamp(-physics.ladderMaxSpeed, vel.x, physics.ladderMaxSpeed);
                vel.z = clamp(-physics.ladderMaxSpeed, vel.z, physics.ladderMaxSpeed);
                vel.y = Math.max(vel.y, controlState.isSneaking() ? 0 : -physics.ladderMaxSpeed);
            }

            moveEntity(entity, world, vel.x, vel.y, vel.z);

            if (isOnLadder(world, pos.toImmutableInt()) && (entity.isCollidedHorizontally)) {
                vel.y = physics.ladderClimbSpeed; // climb ladder
            }

            // Apply friction and gravity
            if (entity.levitation > 0) {
                vel.y += (0.05 * entity.levitation - vel.y) * 0.2;
            } else {
                vel.y -= physics.gravity * gravityMultiplier;
            }
            vel.y *= physics.airdrag;
            vel.x *= inertia;
            vel.z *= inertia;
        }
    }

    public boolean isMaterialInBB(LevelState world, AABB queryBB, List<BlockType> types) {
        var cursor = new MutableVector3d(0, 0, 0);
        for (cursor.y = Math.floor(queryBB.minY); cursor.y <= Math.floor(queryBB.maxY); cursor.y++) {
            for (cursor.z = Math.floor(queryBB.minZ); cursor.z <= Math.floor(queryBB.maxZ); cursor.z++) {
                for (cursor.x = Math.floor(queryBB.minX); cursor.x <= Math.floor(queryBB.maxX); cursor.x++) {
                    var block = world.getBlockStateAt(cursor.toImmutableInt());

                    if (block.isPresent() && types.contains(block.get().blockType())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public int getLiquidHeightPcent(@Nullable BlockStateMeta meta, Vector3i block) {
        return (getRenderedDepth(meta, block) + 1) / 9;
    }

    public int getRenderedDepth(@Nullable BlockStateMeta meta, Vector3i block) {
        if (meta == null) return -1;

        if (WATER_LIKE_TYPES.contains(meta.blockType())) return 0;

        if (meta.blockShapeType().properties().getBoolean("waterlogged")) return 0;

        if (!WATER_TYPES.contains(meta.blockType())) return -1;

        var level = meta.blockShapeType().properties().getInt("level");
        return level >= 8 ? 0 : level;
    }

    public Vector3i getFlow(LevelState world, BlockStateMeta meta, Vector3i block) {
        var curlevel = getRenderedDepth(meta, block);
        var flow = new MutableVector3d(0, 0, 0);
        for (var combination : new int[][]{new int[]{0, 1}, new int[]{-1, 0}, new int[]{0, -1}, new int[]{1, 0}}) {
            var dx = combination[0];
            var dz = combination[1];
            var adjBlockVec = block.add(dx, 0, dz);
            var adjBlock = world.getBlockStateAt(adjBlockVec);
            var adjLevel = getRenderedDepth(adjBlock.orElse(null), adjBlockVec);
            if (adjLevel < 0) {
                if (adjBlock.isPresent() && adjBlock.get().blockShapeType().isEmpty()) {
                    var adjLevel2Vec = block.add(dx, -1, dz);
                    var adjLevel2 = getRenderedDepth(world.getBlockStateAt(adjLevel2Vec).orElse(null), adjLevel2Vec);
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

    public List<Pair<Vector3i, BlockStateMeta>> getWaterInBB(LevelState world, AABB bb) {
        var waterBlocks = new ArrayList<Pair<Vector3i, BlockStateMeta>>();
        var cursor = new MutableVector3d(0, 0, 0);
        for (cursor.y = Math.floor(bb.minY); cursor.y <= Math.floor(bb.maxY); cursor.y++) {
            for (cursor.z = Math.floor(bb.minZ); cursor.z <= Math.floor(bb.maxZ); cursor.z++) {
                for (cursor.x = Math.floor(bb.minX); cursor.x <= Math.floor(bb.maxX); cursor.x++) {
                    var cursorVec = cursor.toImmutableInt();
                    var block = world.getBlockStateAt(cursorVec);
                    if (block.isPresent() && (WATER_TYPES.contains(block.get().blockType())
                            || WATER_LIKE_TYPES.contains(block.get().blockType())
                            || block.get().blockShapeType().properties().getBoolean("waterlogged"))) {
                        var waterLevel = cursor.y + 1 - getLiquidHeightPcent(block.get(), cursorVec);
                        if (Math.ceil(bb.maxY) >= waterLevel) {
                            waterBlocks.add(Pair.of(cursorVec, block.get()));
                        }
                    }
                }
            }
        }
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

    public void simulatePlayer(PlayerMovementState entity, LevelState world) {
        var vel = entity.vel;
        var pos = entity.pos;

        var waterBB = getPlayerBB(pos).contract(0.001, 0.401, 0.001);
        var lavaBB = getPlayerBB(pos).contract(0.1, 0.4, 0.1);

        entity.isInWater = isInWaterApplyCurrent(world, waterBB, vel);
        entity.isInLava = isMaterialInBB(world, lavaBB, LAVA_TYPES);

        // Reset velocity component if it falls under the threshold
        if (Math.abs(vel.x) < physics.negligeableVelocity) vel.x = 0;
        if (Math.abs(vel.y) < physics.negligeableVelocity) vel.y = 0;
        if (Math.abs(vel.z) < physics.negligeableVelocity) vel.z = 0;

        // Handle inputs
        if (controlState.isJumping() || entity.jumpQueued) {
            if (entity.jumpTicks > 0) entity.jumpTicks--;
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

        var rightNumber = controlState.isRight() ? 1 : 0;
        var leftNumber = controlState.isLeft() ? 1 : 0;
        var forwardNumber = controlState.isForward() ? 1 : 0;
        var backNumber = controlState.isBackward() ? 1 : 0;
        var strafe = (rightNumber - leftNumber) * 0.98;
        var forward = (forwardNumber - backNumber) * 0.98;

        if (controlState.isSneaking()) {
            strafe *= physics.sneakSpeed;
            forward *= physics.sneakSpeed;
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

        moveEntityWithHeading(entity, world, strafe, forward);
    }

    private double getBlockSlipperiness(BlockType blockType) {
        if (blockType == BlockType.SLIME_BLOCK) {
            return 0.8D;
        } else if (blockType == BlockType.ICE || blockType == BlockType.PACKED_ICE) {
            return 0.98D;
        } else if (blockType == BlockType.BLUE_ICE) {
            return 0.989D;
        } else {
            return physics.defaultSlipperiness; // Normal block
        }
    }
}
