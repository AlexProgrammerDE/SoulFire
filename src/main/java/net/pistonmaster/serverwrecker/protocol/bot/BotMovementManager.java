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
package net.pistonmaster.serverwrecker.protocol.bot;

import com.github.steveice10.mc.protocol.data.game.entity.RotationOrigin;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerRotPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerStatusOnlyPacket;
import lombok.Data;
import lombok.Setter;
import lombok.ToString;
import net.pistonmaster.serverwrecker.data.BlockType;
import net.pistonmaster.serverwrecker.protocol.bot.model.AbilitiesData;
import net.pistonmaster.serverwrecker.protocol.bot.state.LevelState;
import net.pistonmaster.serverwrecker.util.BoundingBox;
import net.pistonmaster.serverwrecker.util.MathHelper;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Huge credit to <a href="https://github.com/LabyStudio/java-minecraft/blob/master/src/main/java/de/labystudio/game/player/Player.java">LabyStudio/java-minecraft</a>
 */
@Data
public final class BotMovementManager {
    private static final float STEP_HEIGHT = 0.5F;
    @ToString.Exclude
    private final SessionDataManager dataManager;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private BoundingBox boundingBox;
    private double motionX;
    private double motionY;
    private double motionZ;
    private boolean onGround = false;
    private boolean collision = false;
    private float jumpMovementFactor = 0.02F;
    private float speedInAir = 0.02F;
    private float moveForward;
    private float moveStrafing;
    private boolean jumping;
    private boolean sprinting;
    private boolean sneaking;
    @Setter
    private boolean flying;
    @Setter
    private float abilitiesFlySpeed = 0.05F;
    @Setter
    private float walkSpeed = 0.10000000149011612F;
    private int jumpTicks;
    private Vector3d movementTarget;
    private int ticksWithoutPacket = 0;

    public BotMovementManager(SessionDataManager dataManager, double x, double y, double z, float yaw, float pitch, @Nullable AbilitiesData data) {
        this.dataManager = dataManager;
        this.yaw = yaw;
        this.pitch = pitch;
        if (data != null) {
            this.flying = data.flying();
            this.abilitiesFlySpeed = data.flySpeed();
            this.walkSpeed = data.walkSpeed();
        }

        setPosition(x, y, z);
    }

    public void setPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        updateBoundingBox(); // New position, new bounding box
    }

    public void setSneaking(boolean sneaking) {
        this.sneaking = sneaking;
        updateBoundingBox(); // New height, new bounding box
    }

    public void updateBoundingBox() {
        float w = getBoundingBoxWidth() / 2;
        float h = getBoundingBoxHeight() / 2;
        this.boundingBox = new BoundingBox(x - w, y, z - w, x + w, y + h, z + w);
    }

    public void setRotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public void setMotion(double x, double y, double z) {
        this.motionX = x;
        this.motionY = y;
        this.motionZ = z;
    }

    public void turn(float xo, float yo) {
        this.yaw = ((float) (this.yaw + xo * 0.15D));
        this.pitch = ((float) (this.pitch - yo * 0.15D));
        if (this.pitch < -90.0F) {
            this.pitch = -90.0F;
        }

        if (this.pitch > 90.0F) {
            this.pitch = 90.0F;
        }
    }

    public void lookAt(RotationOrigin origin, Vector3d block) {
        boolean eyes = origin == RotationOrigin.EYES;

        double dx = block.getX() - this.x;
        double dy = block.getY() - (eyes ? this.y + getEyeHeight() : this.y);
        double dz = block.getZ() - this.z;

        double distanceXZ = Math.sqrt(dx * dx + dz * dz);
        double yaw = Math.toDegrees(Math.atan2(-dx, dz));
        double pitch = Math.toDegrees(Math.atan2(dy, distanceXZ));

        this.yaw = (float) yaw;
        this.pitch = (float) pitch;
    }

    private float updateRotation(float angle, float targetAngle, float maxIncrease) {
        float f = MathHelper.wrapDegrees(targetAngle - angle);
        if (f > maxIncrease) {
            f = maxIncrease;
        }

        if (f < -maxIncrease) {
            f = -maxIncrease;
        }

        return angle + f;
    }

    public void tick() {
        double startX = this.x;
        double startY = this.y;
        double startZ = this.z;

        float startPitch = this.pitch;
        float startYaw = this.yaw;

        boolean startOnGround = this.onGround;

        if (this.jumpTicks > 0) {
            --this.jumpTicks;
        }

        // Stop if too slow
        if (Math.abs(this.motionX) < 0.003D) {
            this.motionX = 0.0D;
        }
        if (Math.abs(this.motionY) < 0.003D) {
            this.motionY = 0.0D;
        }
        if (Math.abs(this.motionZ) < 0.003D) {
            this.motionZ = 0.0D;
        }

        // Jump
        if (this.jumping) {
            if (this.isInFluid()) {
                this.motionY += 0.03999999910593033D;
            } else if (this.onGround && this.jumpTicks == 0) {
                this.jump();
                this.jumpTicks = 10;
            }
        } else {
            this.jumpTicks = 0;
        }

        this.moveStrafing *= 0.98F;
        this.moveForward *= 0.98F;

        if (this.flying) {
            this.travelFlying(this.moveForward, 0, this.moveStrafing);
        } else {
            if (this.isInFluid()) {
                // Is inside of water
                this.travelInWater(this.moveForward, 0, this.moveStrafing);
            } else {
                // Is on land
                this.travel(this.moveForward, 0, this.moveStrafing);
            }
        }

        this.jumpMovementFactor = this.speedInAir;

        if (this.sprinting) {
            this.jumpMovementFactor = (float) ((double) this.jumpMovementFactor + (double) this.speedInAir * 0.3D);

            if (this.moveForward <= 0 || this.collision || this.sneaking) {
                this.sprinting = false;
            }
        }

        if (movementTarget != null) {
            lookAt(RotationOrigin.FEET, movementTarget);

            double speed = 0.2;
            this.motionX = -Math.sin(Math.toRadians(this.yaw)) * Math.cos(Math.toRadians(this.pitch)) * speed;
            this.motionY = -Math.sin(Math.toRadians(this.pitch)) * speed;
            this.motionZ = Math.cos(Math.toRadians(this.yaw)) * Math.cos(Math.toRadians(this.pitch)) * speed;
        }

        // Detect whether positions changed
        boolean positionChanged = startX != this.x || startY != this.y || startZ != this.z;
        boolean rotationChanged = startPitch != this.pitch || startYaw != this.yaw;
        boolean onGroundChanged = startOnGround != this.onGround;

        // Send position packets if changed
        if (positionChanged && rotationChanged) {
            ticksWithoutPacket = 0;
            sendPosRot();
        } else if (positionChanged) {
            ticksWithoutPacket = 0;
            sendPos();
        } else if (rotationChanged) {
            ticksWithoutPacket = 0;
            sendRot();
        } else if (onGroundChanged) {
            ticksWithoutPacket = 0;
            sendOnGround();
        } else if (++ticksWithoutPacket > 20) {
            // Vanilla sends a position packet every 20 ticks if nothing changed
            ticksWithoutPacket = 0;
            sendPos();
        }
    }

    public void sendPosRot() {
        dataManager.getSession().send(new ServerboundMovePlayerPosRotPacket(this.onGround, this.x, this.y, this.z, this.yaw, this.pitch));
    }

    public void sendPos() {
        dataManager.getSession().send(new ServerboundMovePlayerPosPacket(this.onGround, this.x, this.y, this.z));
    }

    public void sendRot() {
        dataManager.getSession().send(new ServerboundMovePlayerRotPacket(this.onGround, this.yaw, this.pitch));
    }

    public void sendOnGround() {
        dataManager.getSession().send(new ServerboundMovePlayerStatusOnlyPacket(this.onGround));
    }

    public boolean isInFluid() {
        Vector3i blockPos = this.getBlockPos();
        LevelState level = getLevelSafe();
        if (level.isOutOfWorld(blockPos)) {
            return false;
        }

        return level.getBlockTypeAt(blockPos).isFluid();
    }

    public void jump() {
        this.motionY = 0.42D;

        if (this.sprinting) {
            float radiansYaw = (float) Math.toRadians(this.yaw);
            this.motionX -= Math.sin(radiansYaw) * 0.2F;
            this.motionZ += Math.cos(radiansYaw) * 0.2F;
        }
    }

    private void travelFlying(float forward, float vertical, float strafe) {
        float flySpeed = getFlySpeed();
        // Fly move up and down
        if (this.sneaking) {
            this.moveStrafing = strafe / 0.3F;
            this.moveForward = forward / 0.3F;
            this.motionY -= flySpeed * 3.0F;
        }

        if (this.jumping) {
            this.motionY += flySpeed * 3.0F;
        }

        double prevMotionY = this.motionY;
        float prevJumpMovementFactor = this.jumpMovementFactor;
        this.jumpMovementFactor = flySpeed * (this.sprinting ? 2 : 1);

        this.travel(forward, vertical, strafe);

        this.motionY = prevMotionY * 0.6D;
        this.jumpMovementFactor = prevJumpMovementFactor;

        if (this.onGround) {
            this.flying = false;
        }
    }

    private void travelInWater(float forward, float vertical, float strafe) {
        float slipperiness = 0.8F;
        float friction = 0.02F;

        this.moveRelative(forward, vertical, strafe, friction);
        this.collision = this.moveCollide(-this.motionX, this.motionY, -this.motionZ);

        this.motionX *= slipperiness;
        this.motionY *= 0.800000011920929D;
        this.motionZ *= slipperiness;
        this.motionY -= 0.02D;
    }

    public void travel(float forward, float vertical, float strafe) {
        float prevSlipperiness = this.getBlockSlipperiness() * 0.91F;

        float value = 0.16277136F / (prevSlipperiness * prevSlipperiness * prevSlipperiness);

        float friction;
        if (this.onGround) {
            friction = this.getAIMoveSpeed() * value;
        } else {
            friction = this.jumpMovementFactor;
        }

        this.moveRelative(forward, vertical, strafe, friction);

        // Get new speed
        float slipperiness = this.getBlockSlipperiness() * 0.91F;

        // Move
        this.collision = this.moveCollide(this.motionX, this.motionY, this.motionZ);

        // Gravity
        if (!this.flying) {
            this.motionY -= 0.08D;
        }

        // Decrease motion
        this.motionX *= slipperiness;
        this.motionY *= 0.9800000190734863D;
        this.motionZ *= slipperiness;
    }

    private float getBlockSlipperiness() {
        if (!this.onGround) {
            return 1.0F;
        }

        Vector3i blockPos = this.getPlayerPos().add(Vector3d.from(0, -0.5, 0)).toInt();
        LevelState level = getLevelSafe();
        if (level.isOutOfWorld(blockPos)) {
            return 1.0F;
        }

        BlockType blockType = level.getBlockTypeAt(blockPos);
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

    private float getAIMoveSpeed() {
        return this.sprinting ? 0.13000001F : walkSpeed;
    }

    public void moveRelative(double forward, double up, double strafe, double friction) {
        double distance = strafe * strafe + up * up + forward * forward;

        if (distance < 1.0E-4F) {
            return;
        }

        distance = Math.sqrt(distance);

        if (distance < 1.0F) {
            distance = 1.0F;
        }

        distance = friction / distance;
        strafe = strafe * distance;
        up = up * distance;
        forward = forward * distance;

        double yawRadians = Math.toRadians(this.yaw);
        double sin = Math.sin(yawRadians);
        double cos = Math.cos(yawRadians);

        this.motionX += strafe * cos - forward * sin;
        this.motionY += up;
        this.motionZ += forward * cos + strafe * sin;
    }

    public boolean moveCollide(double targetX, double targetY, double targetZ) {
        LevelState level = getLevelSafe();
        // Target position
        double originalTargetX = targetX;
        double originalTargetY = targetY;
        double originalTargetZ = targetZ;

        if (this.onGround && this.sneaking) {
            for (; targetX != 0.0D && level.getCollisionBoxes(this.boundingBox.offset(targetX, -STEP_HEIGHT, 0.0D)).isEmpty(); originalTargetX = targetX) {
                if (targetX < 0.05D && targetX >= -0.05D) {
                    targetX = 0.0D;
                } else if (targetX > 0.0D) {
                    targetX -= 0.05D;
                } else {
                    targetX += 0.05D;
                }
            }

            for (; targetZ != 0.0D && level.getCollisionBoxes(this.boundingBox.offset(0.0D, -STEP_HEIGHT, targetZ)).isEmpty(); originalTargetZ = targetZ) {
                if (targetZ < 0.05D && targetZ >= -0.05D) {
                    targetZ = 0.0D;
                } else if (targetZ > 0.0D) {
                    targetZ -= 0.05D;
                } else {
                    targetZ += 0.05D;
                }
            }

            for (; targetX != 0.0D && targetZ != 0.0D && level.getCollisionBoxes(this.boundingBox.offset(targetX, -STEP_HEIGHT, targetZ)).isEmpty(); originalTargetZ = targetZ) {
                if (targetX < 0.05D && targetX >= -0.05D) {
                    targetX = 0.0D;
                } else if (targetX > 0.0D) {
                    targetX -= 0.05D;
                } else {
                    targetX += 0.05D;
                }

                originalTargetX = targetX;

                if (targetZ < 0.05D && targetZ >= -0.05D) {
                    targetZ = 0.0D;
                } else if (targetZ > 0.0D) {
                    targetZ -= 0.05D;
                } else {
                    targetZ += 0.05D;
                }
            }
        }

        // Get level tiles as bounding boxes
        List<BoundingBox> boundingBoxList = level.getCollisionBoxes(this.boundingBox.expand(targetX, targetY, targetZ));

        // Move bounding box
        for (BoundingBox aABB : boundingBoxList) {
            targetY = aABB.clipYCollide(this.boundingBox, targetY);
        }
        this.boundingBox.move(0.0F, targetY, 0.0F);

        for (BoundingBox aABB : boundingBoxList) {
            targetX = aABB.clipXCollide(this.boundingBox, targetX);
        }
        this.boundingBox.move(targetX, 0.0F, 0.0F);

        for (BoundingBox aABB : boundingBoxList) {
            targetZ = aABB.clipZCollide(this.boundingBox, targetZ);
        }
        this.boundingBox.move(0.0F, 0.0F, targetZ);

        this.onGround = originalTargetY != targetY && originalTargetY < 0.0F;

        // Stop motion on collision
        if (originalTargetX != targetX) {
            this.motionX = 0.0F;
        }
        if (originalTargetY != targetY) {
            this.motionY = 0.0F;
        }
        if (originalTargetZ != targetZ) {
            this.motionZ = 0.0F;
        }

        // Update position
        this.x = ((this.boundingBox.minX + this.boundingBox.maxX) / 2.0F);
        this.y = this.boundingBox.minY;
        this.z = ((this.boundingBox.minZ + this.boundingBox.maxZ) / 2.0F);

        // Horizontal collision?
        return originalTargetX != targetX || originalTargetZ != targetZ;
    }

    public float getEyeHeight() {
        return this.sneaking ? 1.50F : 1.62F;
    }

    public float getBoundingBoxWidth() {
        return 0.6F;
    }

    public float getBoundingBoxHeight() {
        return this.sneaking ? 1.5F : 1.8F;
    }

    public int getBlockPosX() {
        return (int) this.x - (this.x < 0 ? 1 : 0);
    }

    public int getBlockPosY() {
        return (int) this.y - (this.y < 0 ? 1 : 0);
    }

    public int getBlockPosZ() {
        return (int) this.z - (this.z < 0 ? 1 : 0);
    }

    public Vector3i getBlockPos() {
        return Vector3i.from(getBlockPosX(), getBlockPosY(), getBlockPosZ());
    }

    public Vector3d getPlayerPos() {
        return Vector3d.from(this.x, this.y, this.z);
    }

    public float getFlySpeed() {
        return isSprinting() ? this.abilitiesFlySpeed * 2.0F : this.abilitiesFlySpeed;
    }

    private LevelState getLevelSafe() {
        // SessionDataManager ensures that the current level is never null
        LevelState level = dataManager.getCurrentLevel();
        assert level != null;
        return level;
    }
}
