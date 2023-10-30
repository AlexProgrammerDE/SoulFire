package net.pistonmaster.serverwrecker.protocol.bot;

import net.pistonmaster.serverwrecker.data.BlockType;
import net.pistonmaster.serverwrecker.protocol.bot.state.LevelState;
import net.pistonmaster.serverwrecker.util.BoundingBox;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.ArrayList;
import java.util.List;

public class BotMovementManagerV2 {
    private final ControlState controlState = new ControlState();

    private float getBlockSlipperiness(BlockType blockType) {
        if (blockType == BlockType.SLIME_BLOCK) {
            return 0.8F;
        } else if (blockType == BlockType.ICE || blockType == BlockType.PACKED_ICE) {
            return 0.98F;
        } else if (blockType == BlockType.BLUE_ICE) {
            return 0.989F;
        } else {
            return 0.6F; // Normal block
        }
    }

    {

        // Block ids
        var soulsandId = BlockType.SOUL_SAND;
        var honeyblockId = BlockType.HONEY_BLOCK
        var webId = BlockType.COBWEB
        var waterIds = [BlockType.water.id, BlockType.flowing_water ? BlockType.flowing_water.id : -1;]
        var lavaIds = [BlockType.lava.id, BlockType.flowing_lava ? BlockType.flowing_lava.id : -1;]
        var ladderId = BlockType.ladder.id
        var vineId = BlockType.VINE;
        var waterLike = new BlockType[]{
                BlockType.SEAGRASS,
                BlockType.TALL_SEAGRASS,
                BlockType.KELP,
                BlockType.KELP_PLANT,
                BlockType.BUBBLE_COLUMN
        };

        var physics = {
                gravity:0.08, // blocks/tick^2 https://minecraft.gamepedia.com/Entity#Motion_of_entities
            airdrag;:Math.fround(1 - 0.02), // actually (1 - drag)
            yawSpeed;:3.0,
            pitchSpeed;:3.0,
            playerSpeed;:0.1,
            sprintSpeed;:0.3,
            sneakSpeed;:0.3,
            stepHeight;:0.6, // how much height can the bot step on without jump
            negligeableVelocity;:0.003, // actually 0.005 for 1.8, but seems fine
            soulsandSpeed;:0.4,
            honeyblockSpeed;:0.4,
            honeyblockJumpSpeed;:0.4,
            ladderMaxSpeed;:0.15,
            ladderClimbSpeed;:0.2,
            playerHalfWidth;:0.3,
            playerHeight;:1.8,
            waterInertia;:0.8,
            lavaInertia;:0.5,
            liquidAcceleration;:0.02,
            airborneInertia;:0.91,
            airborneAcceleration;:0.02,
            defaultSlipperiness;:0.6,
            outOfLiquidImpulse;:0.3,
            autojumpCooldown;:10, // ticks (0.5s)
            bubbleColumnSurfaceDrag;:{
        down:
        0.03,
                maxDown;:-0.9,
                up;:0.1,
                maxUp;:1.8
    },
        bubbleColumnDrag:
        {
            down:
            0.03,
                    maxDown;:-0.3,
                up;:0.06,
                maxUp;:0.7
        },
        slowFalling:
        0.125,
                movementSpeedAttribute;:mcData.attributesByName.movementSpeed.resource,
            sprintingUUID;:
        '662a6b8d-da3e-4c1c-8813-96ea6097278d' // SPEED_MODIFIER_SPRINTING_UUID is from LivingEntity.java
    }

        physics.waterGravity = physics.gravity / 16;
        physics.lavaGravity = physics.gravity / 4;
    }

    public BoundingBox getPlayerBB(pos) {
        var w = physics.playerHalfWidth
        return new BoundingBox(-w, 0, -w, w, physics.playerHeight, w).offset(pos.x, pos.y, pos.z);
    }

    public void setPositionToBB(BoundingBox bb, pos) {
        pos.x = bb.minX + physics.playerHalfWidth;
        pos.y = bb.minY;
        pos.z = bb.minZ + physics.playerHalfWidth;
    }

    public List<BoundingBox> getSurroundingBBs(LevelState world, BoundingBox queryBB) {
        var surroundingBBs = new ArrayList<BoundingBox>();
        var cursor = Vector3i.from(0, 0, 0)
        for (cursor.y = Math.floor(queryBB.minY) - 1; cursor.y <= Math.floor(queryBB.maxY); cursor.y++) {
            for (cursor.z = Math.floor(queryBB.minZ); cursor.z <= Math.floor(queryBB.maxZ); cursor.z++) {
                for (cursor.x = Math.floor(queryBB.minX); cursor.x <= Math.floor(queryBB.maxX); cursor.x++) {
                    var block = world.getBlockStateAt(cursor)
                    if (block) {
                        var blockPos = block.position
                        for (var shape : block.shapes){
                            var blockBB = new BoundingBox(shape[0], shape[1], shape[2], shape[3], shape[4], shape[5])
                            blockBB.offset(blockPos.x, blockPos.y, blockPos.z);
                            surroundingBBs.push(blockBB);
                        }
                    }
                }
            }
        }
        return surroundingBBs;
    }

    public void adjustPositionHeight(pos) {
        var playerBB = getPlayerBB(pos)
        var queryBB = playerBB.clone().extend(0, -1, 0)
        var surroundingBBs = getSurroundingBBs(world, queryBB)

        var dy = -1D
        for (var blockBB : surroundingBBs){
            dy = blockBB.computeOffsetY(playerBB, dy);
        }
        pos.y += dy;
    }

    public void moveEntity(entity, LevelState world, double dx, double dy, double dz) {
        var vel = entity.vel
        var pos = entity.pos

        if (entity.isInWeb) {
            dx *= 0.25;
            dy *= 0.05;
            dz *= 0.25;
            vel.x = 0;
            vel.y = 0;
            vel.z = 0;
            entity.isInWeb = false;
        }

        var oldVelX = dx
        var oldVelY = dy
        var oldVelZ = dz

        if (controlState.isSneaking() && entity.onGround) {
            var step = 0.05

            // In the 3 loops bellow, y offset should be -1, but that doesnt reproduce vanilla behavior.
            for (; dx != = 0 && getSurroundingBBs(world, getPlayerBB(pos).offset(dx, 0, 0)).length == = 0; oldVelX = dx) {
                if (dx < step && dx >= -step) dx = 0;
                else if (dx > 0) dx -= step;
                else dx += step;
            }

            for (; dz != = 0 && getSurroundingBBs(world, getPlayerBB(pos).offset(0, 0, dz)).length == = 0; oldVelZ = dz) {
                if (dz < step && dz >= -step) dz = 0;
                else if (dz > 0) dz -= step;
                else dz += step;
            }

            while (dx != = 0 && dz != = 0 && getSurroundingBBs(world, getPlayerBB(pos).offset(dx, 0, dz)).length == = 0) {
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

        var playerBB = getPlayerBB(pos)
        var queryBB = playerBB.clone().extend(dx, dy, dz)
        var surroundingBBs = getSurroundingBBs(world, queryBB)
        var oldBB = playerBB.clone()

        for (var blockBB : surroundingBBs){
            dy = blockBB.computeOffsetY(playerBB, dy);
        }
        playerBB.offset(0, dy, 0);

        for (var blockBB : surroundingBBs){
            dx = blockBB.computeOffsetX(playerBB, dx);
        }
        playerBB.offset(dx, 0, 0);

        for (var blockBB : surroundingBBs){
            dz = blockBB.computeOffsetZ(playerBB, dz);
        }
        playerBB.offset(0, 0, dz);

        // Step on block if height < stepHeight
        if (physics.stepHeight > 0 &&
                (entity.onGround || (dy != = oldVelY && oldVelY < 0)) &&
                (dx != = oldVelX || dz != = oldVelZ)) {
            var oldVelXCol = dx
            var oldVelYCol = dy
            var oldVelZCol = dz
            var oldBBCol = playerBB.clone()

            dy = physics.stepHeight;
            var queryBB = oldBB.clone().extend(oldVelX, dy, oldVelZ)
            var surroundingBBs = getSurroundingBBs(world, queryBB)

            var BB1 = oldBB.clone()
            var BB2 = oldBB.clone()
            var BB_XZ = BB1.clone().extend(dx, 0, dz)

            var dy1 = dy
            var dy2 = dy
            for (var blockBB : surroundingBBs){
                dy1 = blockBB.computeOffsetY(BB_XZ, dy1);
                dy2 = blockBB.computeOffsetY(BB2, dy2);
            }
            BB1.offset(0, dy1, 0);
            BB2.offset(0, dy2, 0);

            var dx1 = oldVelX
            var dx2 = oldVelX
            for (var blockBB : surroundingBBs){
                dx1 = blockBB.computeOffsetX(BB1, dx1);
                dx2 = blockBB.computeOffsetX(BB2, dx2);
            }
            BB1.offset(dx1, 0, 0);
            BB2.offset(dx2, 0, 0);

            var dz1 = oldVelZ
            var dz2 = oldVelZ
            for (var blockBB : surroundingBBs){
                dz1 = blockBB.computeOffsetZ(BB1, dz1);
                dz2 = blockBB.computeOffsetZ(BB2, dz2);
            }
            BB1.offset(0, 0, dz1);
            BB2.offset(0, 0, dz2);

            var norm1 = dx1 * dx1 + dz1 * dz1
            var norm2 = dx2 * dx2 + dz2 * dz2

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

            for (var blockBB : surroundingBBs){
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
        entity.isCollidedHorizontally = dx != = oldVelX || dz != = oldVelZ;
        entity.isCollidedVertically = dy != = oldVelY;
        entity.onGround = entity.isCollidedVertically && oldVelY < 0;

        var blockAtFeet = world.getBlockStateAt(pos.offset(0, -0.2, 0))

        if (dx != = oldVelX) vel.x = 0;
        if (dz != = oldVelZ) vel.z = 0;
        if (dy != = oldVelY) {
            if (blockAtFeet && blockAtFeet.type == = slimeBlockId && !entity.control.sneak) {
                vel.y = -vel.y;
            } else {
                vel.y = 0;
            }
        }

        // Finally, apply block collisions (web, soulsand...)
        playerBB.contract(0.001, 0.001, 0.001);
        var cursor = Vector3i.from(0, 0, 0)
        for (cursor.y = Math.floor(playerBB.minY); cursor.y <= Math.floor(playerBB.maxY); cursor.y++) {
            for (cursor.z = Math.floor(playerBB.minZ); cursor.z <= Math.floor(playerBB.maxZ); cursor.z++) {
                for (cursor.x = Math.floor(playerBB.minX); cursor.x <= Math.floor(playerBB.maxX); cursor.x++) {
                    var block = world.getBlockStateAt(cursor)
                    if (block) {
                        if (block.type == = webId) {
                            entity.isInWeb = true;
                        } else if (block.type == = bubblecolumnId) {
                            var down = !block.metadata
                            var aboveBlock = world.getBlockStateAt(cursor.offset(0, 1, 0))
                            var bubbleDrag = (aboveBlock && aboveBlock.type == = 0 /* air */) ? physics.bubbleColumnSurfaceDrag : physics.bubbleColumnDrag
                            if (down) {
                                vel.y = Math.max(bubbleDrag.maxDown, vel.y - bubbleDrag.down);
                            } else {
                                vel.y = Math.min(bubbleDrag.maxUp, vel.y + bubbleDrag.up);
                            }
                        }
                    }
                }
            }
        }


        const blockBelow = world.getBlockStateAt(entity.pos.floored().offset(0, -0.5, 0));
        if (blockBelow) {
            if (blockBelow.type == = soulsandId) {
                vel.x *= physics.soulsandSpeed;
                vel.z *= physics.soulsandSpeed;
            } else if (blockBelow.type == = honeyblockId) {
                vel.x *= physics.honeyblockSpeed;
                vel.z *= physics.honeyblockSpeed;
            }
        }
    }

    public Vector3f getLookingVector(entity) {
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

        var yaw = entity.yaw
        var pitch = entity.pitch
        var sinYaw = Math.sin(yaw)
        var cosYaw = Math.cos(yaw)
        var sinPitch = Math.sin(pitch)
        var cosPitch = Math.cos(pitch)
        var lookX = -sinYaw * cosPitch
        var lookY = sinPitch
        var lookZ = -cosYaw * cosPitch
        var lookDir = Vector3i.from(lookX, lookY, lookZ)
        return {
                yaw,
                pitch,
                sinYaw,
                cosYaw,
                sinPitch,
                cosPitch,
                lookX,
                lookY,
                lookZ,
                lookDir
        };
    }

    public void applyHeading(entity, float strafe, float forward, multiplier) {
        var speed = Math.sqrt(strafe * strafe + forward * forward)
        if (speed < 0.01) return Vector3i.from(0, 0, 0);

        speed = multiplier / Math.max(speed, 1);

        strafe *= speed;
        forward *= speed;

        var yaw = Math.PI - entity.yaw
        var sin = Math.sin(yaw)
        var cos = Math.cos(yaw)

        var vel = entity.vel
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

    public boolean doesNotCollide(LevelState world, pos) {
        var pBB = getPlayerBB(pos)
        return !getSurroundingBBs(world, pBB).some(x = > pBB.intersects(x);) &&getWaterInBB(world, pBB).length == = 0;
    }

    public void moveEntityWithHeading(entity, LevelState world, float strafe, float forward) {
        var vel = entity.vel
        var pos = entity.pos

        var gravityMultiplier = (vel.y <= 0 && entity.slowFalling > 0) ? physics.slowFalling : 1

        if (entity.isInWater || entity.isInLava) {
            // Water / Lava movement
            var lastY = pos.y
            var acceleration = physics.liquidAcceleration
            var inertia = entity.isInWater ? physics.waterInertia : physics.lavaInertia
            var horizontalInertia = inertia

            if (entity.isInWater) {
                var strider = Math.min(entity.depthStrider, 3)
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
            var {
                pitch,
                        sinPitch,
                        cosPitch,
                        lookDir;
            } =getLookingVector(entity);
            var horizontalSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z)
            var cosPitchSquared = cosPitch * cosPitch
            vel.y += physics.gravity * gravityMultiplier * (-1.0 + cosPitchSquared * 0.75);
            // cosPitch is in [0, 1], so cosPitch > 0.0 is just to protect against
            // divide by zero errors
            if (vel.y < 0.0 && cosPitch > 0.0) {
                var movingDownSpeedModifier = vel.y * (-0.1) * cosPitchSquared
                vel.x += lookDir.x * movingDownSpeedModifier / cosPitch;
                vel.y += movingDownSpeedModifier;
                vel.z += lookDir.z * movingDownSpeedModifier / cosPitch;
            }

            if (pitch < 0.0 && cosPitch > 0.0) {
                var lookDownSpeedModifier = horizontalSpeed * (-sinPitch) * 0.04
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
            var acceleration = 0.0
            var inertia = 0.0
            var blockUnder = world.getBlockStateAt(pos.offset(0, -1, 0))
            if (entity.onGround && blockUnder) {
                var playerSpeedAttribute
                if (entity.attributes && entity.attributes[physics.movementSpeedAttribute]) {
                    // Use server-side player attributes
                    playerSpeedAttribute = entity.attributes[physics.movementSpeedAttribute];
                } else {
                    // Create an attribute if the player does not have it
                    playerSpeedAttribute = attribute.createAttributeValue(physics.playerSpeed);
                }
                // Client-side sprinting (don't rely on server-side sprinting)
                // setSprinting in LivingEntity.java
                playerSpeedAttribute = attribute.deleteAttributeModifier(playerSpeedAttribute, physics.sprintingUUID); // always delete sprinting (if it exists)
                if (entity.control.sprint) {
                    if (!attribute.checkAttributeModifier(playerSpeedAttribute, physics.sprintingUUID)) {
                        playerSpeedAttribute = attribute.addAttributeModifier(playerSpeedAttribute, {
                                uuid;:physics.sprintingUUID,
                                amount;:physics.sprintSpeed,
                                operation;:2
            })
                    }
                }
                // Calculate what the speed is (0.1 if no modification)
                var attributeSpeed = attribute.getAttributeValue(playerSpeedAttribute)
                inertia = (blockSlipperiness[blockUnder.type] || physics.defaultSlipperiness) * 0.91;
                acceleration = attributeSpeed * (0.1627714 / (inertia * inertia * inertia));
                if (acceleration < 0) acceleration = 0; // acceleration should not be negative
            } else {
                acceleration = physics.airborneAcceleration;
                inertia = physics.airborneInertia;

                if (controlState.isSprinting()) {
                    var airSprintFactor = physics.airborneAcceleration * 0.3
                    acceleration += airSprintFactor;
                }
            }

            applyHeading(entity, strafe, forward, acceleration);

            if (isOnLadder(world, pos)) {
                vel.x = math.clamp(-physics.ladderMaxSpeed, vel.x, physics.ladderMaxSpeed);
                vel.z = math.clamp(-physics.ladderMaxSpeed, vel.z, physics.ladderMaxSpeed);
                vel.y = Math.max(vel.y, entity.control.sneak ? 0 : -physics.ladderMaxSpeed);
            }

            moveEntity(entity, world, vel.x, vel.y, vel.z);

            if (isOnLadder(world, pos) && (entity.isCollidedHorizontally)) {
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

    public boolean isMaterialInBB(LevelState world, queryBB, types) {
        var cursor = Vector3i.from(0, 0, 0)
        for (cursor.y = Math.floor(queryBB.minY); cursor.y <= Math.floor(queryBB.maxY); cursor.y++) {
            for (cursor.z = Math.floor(queryBB.minZ); cursor.z <= Math.floor(queryBB.maxZ); cursor.z++) {
                for (cursor.x = Math.floor(queryBB.minX); cursor.x <= Math.floor(queryBB.maxX); cursor.x++) {
                    var block = world.getBlockStateAt(cursor)
                    if (block && types.includes(block.type)) return true;
                }
            }
        }
        return false;
    }

    public int getLiquidHeightPcent(Vector3i block) {
        return (getRenderedDepth(block) + 1) / 9;
    }

    public int getRenderedDepth(Vector3i block) {
        if (!block) return -1;
        if (waterLike.has(block.type)) return 0;
        if (block.getProperties().waterlogged) return 0;
        if (!waterIds.includes(block.type)) return -1;
        var meta = block.metadata
        return meta >= 8 ? 0 : meta;
    }

    public Vector3i getFlow(LevelState world, block) {
        var curlevel = getRenderedDepth(block)
        var flow = Vector3i.from(0, 0, 0)
        for (var[dx, dz;;] : [[0, 1;], [-1, 0;], [0, -1;], [1, 0;]]){
            var adjBlock = world.getBlockStateAt(block.position.offset(dx, 0, dz))
            var adjLevel = getRenderedDepth(adjBlock)
            if (adjLevel < 0) {
                if (adjBlock && adjBlock.boundingBox != = 'empty') {
                    var adjLevel = getRenderedDepth(world.getBlockStateAt(block.position.offset(dx, -1, dz)))
                    if (adjLevel >= 0) {
                        var f = adjLevel - (curlevel - 8)
                        flow.x += dx * f;
                        flow.z += dz * f;
                    }
                }
            } else {
                var f = adjLevel - curlevel
                flow.x += dx * f;
                flow.z += dz * f;
            }
        }

        if (block.metadata >= 8) {
            for (var[dx, dz;;] : [[0, 1;], [-1, 0;], [0, -1;], [1, 0;]]){
                var adjBlock = world.getBlockStateAt(block.position.offset(dx, 0, dz))
                var adjUpBlock = world.getBlockStateAt(block.position.offset(dx, 1, dz))
                if ((adjBlock && adjBlock.boundingBox != = 'empty') || (adjUpBlock && adjUpBlock.boundingBox != = 'empty')) {
                    flow.normalize().translate(0, -6, 0);
                }
            }
        }

        return flow.normalize();
    }

    public List<Vector3i> getWaterInBB(LevelState world, BoundingBox bb) {
        var waterBlocks = new ArrayList<Vector3i>();
        var cursor = new Vector3i(0, 0, 0)
        for (cursor.y = Math.floor(bb.minY); cursor.y <= Math.floor(bb.maxY); cursor.y++) {
            for (cursor.z = Math.floor(bb.minZ); cursor.z <= Math.floor(bb.maxZ); cursor.z++) {
                for (cursor.x = Math.floor(bb.minX); cursor.x <= Math.floor(bb.maxX); cursor.x++) {
                    var block = world.getBlockStateAt(cursor)
                    if (block && (waterIds.includes(block.type) || waterLike.has(block.type) || block.getProperties().waterlogged)) {
                        var waterLevel = cursor.y + 1 - getLiquidHeightPcent(block)
                        if (Math.ceil(bb.maxY) >= waterLevel) waterBlocks.push(block);
                    }
                }
            }
        }
        return waterBlocks;
    }

    public boolean isInWaterApplyCurrent(LevelState world, BoundingBox bb, vel) {
        var acceleration = Vector3i.from(0, 0, 0)
        var waterBlocks = getWaterInBB(world, bb)
        var isInWater = waterBlocks.length > 0
        for (var block : waterBlocks){
            var flow = getFlow(world, block)
            acceleration.add(flow);
        }

        var len = acceleration.norm()
        if (len > 0) {
            vel.x += acceleration.x / len * 0.014;
            vel.y += acceleration.y / len * 0.014;
            vel.z += acceleration.z / len * 0.014;
        }
        return isInWater;
    }

    public void simulatePlayer(entity, LevelState world) {
        var vel = entity.vel
        var pos = entity.pos

        var waterBB = getPlayerBB(pos).contract(0.001, 0.401, 0.001)
        var lavaBB = getPlayerBB(pos).contract(0.1, 0.4, 0.1)

        entity.isInWater = isInWaterApplyCurrent(world, waterBB, vel);
        entity.isInLava = isMaterialInBB(world, lavaBB, lavaIds);

        // Reset velocity component if it falls under the threshold
        if (Math.abs(vel.x) < physics.negligeableVelocity) vel.x = 0;
        if (Math.abs(vel.y) < physics.negligeableVelocity) vel.y = 0;
        if (Math.abs(vel.z) < physics.negligeableVelocity) vel.z = 0;

        // Handle inputs
        if (controlState.isJumping() || entity.jumpQueued) {
            if (entity.jumpTicks > 0) entity.jumpTicks--;
            if (entity.isInWater || entity.isInLava) {
                vel.y += 0.04;
            } else if (entity.onGround && entity.jumpTicks == = 0) {
                var blockBelow = world.getBlockStateAt(entity.pos.floored().offset(0, -0.5, 0))
                vel.y = Math.fround(0.42) * ((blockBelow && blockBelow.type == = honeyblockId) ? physics.honeyblockJumpSpeed : 1);
                if (entity.jumpBoost > 0) {
                    vel.y += 0.1 * entity.jumpBoost;
                }
                if (entity.control.sprint) {
                    var yaw = Math.PI - entity.yaw
                    vel.x -= Math.sin(yaw) * 0.2;
                    vel.z += Math.cos(yaw) * 0.2;
                }
                entity.jumpTicks = physics.autojumpCooldown;
            }
        } else {
            entity.jumpTicks = 0; // reset autojump cooldown
        }
        entity.jumpQueued = false;

        var strafe = (entity.control.right - entity.control.left) * 0.98
        var forward = (entity.control.forward - entity.control.back) * 0.98

        if (controlState.isSneaking()) {
            strafe *= physics.sneakSpeed;
            forward *= physics.sneakSpeed;
        }

        entity.elytraFlying = entity.elytraFlying && entity.elytraEquipped && !entity.onGround && !entity.levitation;

        if (entity.fireworkRocketDuration > 0) {
            if (!entity.elytraFlying) {
                entity.fireworkRocketDuration = 0;
            } else {
                var {
                    lookDir
                } =getLookingVector(entity);
                vel.x += lookDir.x * 0.1 + (lookDir.x * 1.5 - vel.x) * 0.5;
                vel.y += lookDir.y * 0.1 + (lookDir.y * 1.5 - vel.y) * 0.5;
                vel.z += lookDir.z * 0.1 + (lookDir.z * 1.5 - vel.z) * 0.5
                        --; entity.fireworkRocketDuration
            }
        }

        moveEntityWithHeading(entity, world, strafe, forward);
    }
}
