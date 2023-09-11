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

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Huge credit to <a href="https://github.com/LabyStudio/java-minecraft/blob/master/src/main/java/de/labystudio/game/player/Player.java">LabyStudio/java-minecraft</a>
 */
@Data
public final class BotMovementManager {
    private static final double STEP_HEIGHT = 0.6D;
    @ToString.Exclude
    private final SessionDataManager dataManager;
    private final ControlState controlState = new ControlState();
    private double x;
    private double y;
    private double z;
    private double lastSentX;
    private double lastSentY;
    private double lastSentZ;
    private float yaw;
    private float pitch;
    private float lastSentYaw;
    private float lastSentPitch;
    private BoundingBox boundingBox;
    private double motionX;
    private double motionY;
    private double motionZ;
    private boolean onGround = false;
    private boolean horizontalCollision = false;
    private float jumpMovementFactor = 0.02F;
    private float speedInAir = 0.02F;
    private float moveForward;
    private float moveStrafing;
    @Setter
    private float abilitiesFlySpeed = 0.05F;
    @Setter
    private float walkSpeed = 0.10000000149011612F;
    private int jumpTicks;
    private int ticksWithoutPacket = 0;

    public BotMovementManager(SessionDataManager dataManager, double x, double y, double z, float yaw, float pitch, @Nullable AbilitiesData data) {
        this.dataManager = dataManager;
        this.yaw = yaw;
        this.pitch = pitch;
        if (data != null) {
            this.controlState.setFlying(data.flying());
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
        this.controlState.setSneaking(sneaking);
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

    /**
     * Updates the rotation to look at a given block or location.
     *
     * @param origin The rotation origin, either EYES or FEET.
     * @param block  The block or location to look at.
     */
    public void lookAt(RotationOrigin origin, Vector3d block) {
        boolean eyes = origin == RotationOrigin.EYES;

        double dx = block.getX() - this.x;
        double dy = block.getY() - (eyes ? this.y + getEyeHeight() : this.y);
        double dz = block.getZ() - this.z;

        double r = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double yaw = -Math.atan2(dx, dz) / Math.PI * 180;
        if (yaw < 0) {
            yaw = 360 + yaw;
        }

        double pitch = -Math.asin(dy / r) / Math.PI * 180;

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

        boolean startOnGround = this.onGround;

        this.updateMovementStateInput();

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
        if (this.controlState.isJumping()) {
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

        if (this.controlState.isFlying()) {
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

        if (this.controlState.isSprinting()) {
            this.jumpMovementFactor = (float) ((double) this.jumpMovementFactor + (double) this.speedInAir * 0.3D);

            if (this.moveForward <= 0 || this.horizontalCollision || this.controlState.isSneaking()) {
                this.controlState.setSprinting(false);
            }
        }

        // Detect whether positions changed
        boolean positionChanged = startX != this.x || startY != this.y || startZ != this.z;
        boolean rotationChanged = yaw != this.lastSentYaw || pitch != this.lastSentPitch;
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

    private void updateMovementStateInput() {
        float moveForward = 0.0F;
        float moveStrafe = 0.0F;

        if (controlState.isForward()) {
            moveForward++;
        }

        if (controlState.isBackward()) {
            moveForward--;
        }

        if (controlState.isLeft()) {
            moveStrafe++;
        }

        if (controlState.isRight()) {
            moveStrafe--;
        }

        if (controlState.isSneaking()) {
            moveStrafe = (float) ((double) moveStrafe * 0.3D);
            moveForward = (float) ((double) moveForward * 0.3D);
        }

        this.moveForward = moveForward;
        this.moveStrafing = moveStrafe;
    }

    public void sendPosRot() {
        lastSentX = x;
        lastSentY = y;
        lastSentZ = z;
        lastSentYaw = yaw;
        lastSentPitch = pitch;
        dataManager.getSession().send(new ServerboundMovePlayerPosRotPacket(this.onGround, this.x, this.y, this.z, this.yaw, this.pitch));
    }

    public void sendPos() {
        lastSentX = x;
        lastSentY = y;
        lastSentZ = z;
        dataManager.getSession().send(new ServerboundMovePlayerPosPacket(this.onGround, this.x, this.y, this.z));
    }

    public void sendRot() {
        lastSentYaw = yaw;
        lastSentPitch = pitch;
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

        Optional<BlockType> blockType = level.getBlockTypeAt(blockPos);
        return blockType.map(BlockType::isFluid).orElse(false);
    }

    public void jump() {
        this.motionY = 0.42D;

        if (this.controlState.isSprinting()) {
            float radiansYaw = (float) Math.toRadians(this.yaw);
            this.motionX -= Math.sin(radiansYaw) * 0.2F;
            this.motionZ += Math.cos(radiansYaw) * 0.2F;
        }
    }

    private void travelFlying(float forward, float vertical, float strafe) {
        float flySpeed = getFlySpeed();
        // Fly move up and down
        if (this.controlState.isSneaking()) {
            this.moveStrafing = strafe / 0.3F;
            this.moveForward = forward / 0.3F;
            this.motionY -= flySpeed * 3.0F;
        }

        if (this.controlState.isJumping()) {
            this.motionY += flySpeed * 3.0F;
        }

        double prevMotionY = this.motionY;
        float prevJumpMovementFactor = this.jumpMovementFactor;
        this.jumpMovementFactor = flySpeed * (this.controlState.isSprinting() ? 2 : 1);

        this.travel(forward, vertical, strafe);

        this.motionY = prevMotionY * 0.6D;
        this.jumpMovementFactor = prevJumpMovementFactor;

        if (this.onGround) {
            this.controlState.setFlying(false);
        }
    }

    private void travelInWater(float forward, float vertical, float strafe) {
        float slipperiness = 0.8F;
        float friction = 0.02F;

        this.moveRelative(forward, vertical, strafe, friction);
        this.horizontalCollision = this.moveCollide(-this.motionX, this.motionY, -this.motionZ);

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
        this.horizontalCollision = this.moveCollide(this.motionX, this.motionY, this.motionZ);

        // Gravity
        if (!this.controlState.isFlying()) {
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

        Vector3i blockPos = this.getPlayerPos().add(0, -0.5, 0).toInt();
        LevelState level = getLevelSafe();
        if (level.isOutOfWorld(blockPos)) {
            return 1.0F;
        }

        Optional<BlockType> optionalBlockType = level.getBlockTypeAt(blockPos);
        if (optionalBlockType.isEmpty()) {
            return 1.0F;
        }

        BlockType blockType = optionalBlockType.get();

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
        return this.controlState.isSprinting() ? 0.13000001F : walkSpeed;
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

    public boolean moveCollide(double dx, double dy, double dz) {
        LevelState level = getLevelSafe();

        // Store initial values
        double originalDx = dx;
        double originalDy = dy;
        double originalDz = dz;

        // Do not walking off edges when sneaking
        if (this.onGround && this.controlState.isSneaking()) {
            for (; dx != 0.0D && level.getCollisionBoxes(this.boundingBox.offset(dx, -STEP_HEIGHT, 0.0D)).isEmpty(); originalDx = dx) {
                if (dx < 0.05D && dx >= -0.05D) {
                    dx = 0.0D;
                } else if (dx > 0.0D) {
                    dx -= 0.05D;
                } else {
                    dx += 0.05D;
                }
            }

            for (; dz != 0.0D && level.getCollisionBoxes(this.boundingBox.offset(0.0D, -STEP_HEIGHT, dz)).isEmpty(); originalDz = dz) {
                if (dz < 0.05D && dz >= -0.05D) {
                    dz = 0.0D;
                } else if (dz > 0.0D) {
                    dz -= 0.05D;
                } else {
                    dz += 0.05D;
                }
            }

            for (; dx != 0.0D && dz != 0.0D && level.getCollisionBoxes(this.boundingBox.offset(dx, -STEP_HEIGHT, dz)).isEmpty(); originalDz = dz) {
                if (dx < 0.05D && dx >= -0.05D) {
                    dx = 0.0D;
                } else if (dx > 0.0D) {
                    dx -= 0.05D;
                } else {
                    dx += 0.05D;
                }

                originalDx = dx;

                if (dz < 0.05D && dz >= -0.05D) {
                    dz = 0.0D;
                } else if (dz > 0.0D) {
                    dz -= 0.05D;
                } else {
                    dz += 0.05D;
                }
            }
        }

        // Stop the motion when sneaking on an edge
        if (originalDx != dx) {
            this.motionX = 0.0F;
        }

        if (originalDz != dz) {
            this.motionZ = 0.0F;
        }

        // Check for collisions and calculate collisions based on that
        List<BoundingBox> collisionBoxes = level.getCollisionBoxes(this.boundingBox.expand(dx, dy, dz));
        BestXZMoveData bestXZMoveData = getBestMove(collisionBoxes, this.boundingBox, dx, dz);

        // Check if walking up solves the collisions, and thus we'll be able to walk upstairs
        if (this.onGround && !collisionBoxes.isEmpty()) {
            double highestCollision = collisionBoxes.stream().map(b -> b.maxY).max(Comparator.naturalOrder()).orElse(0.0D);
            double highestDeltaY =  highestCollision - this.boundingBox.minY;
            double stepHeight = STEP_HEIGHT;
            if (highestDeltaY > 0.0D && highestDeltaY < stepHeight) {
                stepHeight = highestDeltaY;
            }

            BoundingBox stepBoundingBox = this.boundingBox.offset(0.0D, stepHeight, 0.0D);
            List<BoundingBox> stepCollisionBoxes = level.getCollisionBoxes(stepBoundingBox.expand(dx, 0, dz));

            boolean canWalkUp = true;
            for (BoundingBox aABB : stepCollisionBoxes) {
                double dyCollision = aABB.clipYCollide(stepBoundingBox, dy);
                if (dyCollision != dy) {
                    canWalkUp = false;
                    break;
                }
            }

            if (canWalkUp) {
                BestXZMoveData bestStepXZMoveData = getBestMove(stepCollisionBoxes, stepBoundingBox, dx, dz);
                if (bestStepXZMoveData.totalMotion > bestXZMoveData.totalMotion) {
                    bestXZMoveData = bestStepXZMoveData;
                    collisionBoxes = stepCollisionBoxes;
                    dy = stepHeight;
                }
            }
        }

        dx = bestXZMoveData.dx;
        dz = bestXZMoveData.dz;

        this.boundingBox.move(dx, 0.0F, dz);

        for (BoundingBox aABB : collisionBoxes) {
            dy = aABB.clipYCollide(this.boundingBox, dy);
        }
        this.boundingBox.move(0.0F, dy, 0.0F);

        this.onGround = originalDy != dy && originalDy < 0.0F;

        // Update position
        this.x = ((this.boundingBox.minX + this.boundingBox.maxX) / 2.0F);
        this.y = this.boundingBox.minY;
        this.z = ((this.boundingBox.minZ + this.boundingBox.maxZ) / 2.0F);

        // Horizontal collision?
        return originalDx != dx || originalDz != dz;
    }

    private BestXZMoveData getBestMove(List<BoundingBox> collisionBoxes, BoundingBox boundingBox, double dx, double dz) {
        BoundingBox cornerCheck = boundingBox.clone();
        double targetXCollision = dx;
        double targetZCollision = dz;

        for (BoundingBox aABB : collisionBoxes) {
            targetXCollision = aABB.clipXCollide(cornerCheck, targetXCollision);
        }
        cornerCheck.move(targetXCollision, 0.0F, 0.0F);

        for (BoundingBox aABB : collisionBoxes) {
            targetZCollision = aABB.clipZCollide(cornerCheck, targetZCollision);
        }

        BoundingBox cornerCheck2 = boundingBox.clone();
        double targetZCollision2 = dz;
        double targetXCollision2 = dx;

        for (BoundingBox aABB : collisionBoxes) {
            targetZCollision2 = aABB.clipZCollide(cornerCheck2, targetZCollision2);
        }

        cornerCheck2.move(0.0F, 0.0F, targetZCollision2);

        for (BoundingBox aABB : collisionBoxes) {
            targetXCollision2 = aABB.clipXCollide(cornerCheck2, targetXCollision2);
        }

        // We did this to check if you can get further with moving first "X and then Z" or first "Z and then X"
        // We do this to allow walking around corners
        double totalCollision = Math.abs(targetXCollision) + Math.abs(targetZCollision);
        double totalCollision2 = Math.abs(targetXCollision2) + Math.abs(targetZCollision2);

        if (totalCollision >= totalCollision2) {
            return new BestXZMoveData(totalCollision, targetXCollision, targetZCollision);
        } else {
            return new BestXZMoveData(totalCollision2, targetXCollision2, targetZCollision2);
        }
    }

    public float getEyeHeight() {
        return this.controlState.isSneaking() ? 1.50F : 1.62F;
    }

    public float getBoundingBoxWidth() {
        return 0.6F;
    }

    public float getBoundingBoxHeight() {
        return this.controlState.isSneaking() ? 1.5F : 1.8F;
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
        return controlState.isSprinting() ? this.abilitiesFlySpeed * 2.0F : this.abilitiesFlySpeed;
    }

    private LevelState getLevelSafe() {
        // SessionDataManager ensures that the current level is never null
        LevelState level = dataManager.getCurrentLevel();
        assert level != null;
        return level;
    }

    private record BestXZMoveData(double totalMotion, double dx, double dz) {
    }
}
