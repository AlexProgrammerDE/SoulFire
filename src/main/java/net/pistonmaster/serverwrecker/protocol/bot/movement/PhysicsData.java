package net.pistonmaster.serverwrecker.protocol.bot.movement;

import java.util.UUID;

/**
 * gravity:0.08, // blocks/tick^2 https://minecraft.gamepedia.com/Entity#Motion_of_entities
 * airdrag;:Math.fround(1 - 0.02), // actually (1 - drag)
 * yawSpeed;:3.0,
 * pitchSpeed;:3.0,
 * playerSpeed;:0.1,
 * sprintSpeed;:0.3,
 * sneakSpeed;:0.3,
 * stepHeight;:0.6, // how much height can the bot step on without jump
 * negligeableVelocity;:0.003, // actually 0.005 for 1.8, but seems fine
 * soulsandSpeed;:0.4,
 * honeyblockSpeed;:0.4,
 * honeyblockJumpSpeed;:0.4,
 * ladderMaxSpeed;:0.15,
 * ladderClimbSpeed;:0.2,
 * playerHalfWidth;:0.3,
 * playerHeight;:1.8,
 * waterInertia;:0.8,
 * lavaInertia;:0.5,
 * liquidAcceleration;:0.02,
 * airborneInertia;:0.91,
 * airborneAcceleration;:0.02,
 * defaultSlipperiness;:0.6,
 * outOfLiquidImpulse;:0.3,
 * autojumpCooldown;:10, // ticks (0.5s)
 * bubbleColumnSurfaceDrag;:{
 * down:
 * 0.03,
 * maxDown;:-0.9,
 * up;:0.1,
 * maxUp;:1.8
 * },
 * bubbleColumnDrag:
 * {
 * down:
 * 0.03,
 * maxDown;:-0.3,
 * up;:0.06,
 * maxUp;:0.7
 * },
 * slowFalling:
 * 0.125,
 * movementSpeedAttribute;:mcData.attributesByName.movementSpeed.resource,
 * sprintingUUID;:
 * '662a6b8d-da3e-4c1c-8813-96ea6097278d' // SPEED_MODIFIER_SPRINTING_UUID is from LivingEntity.java
 * }
 * <p>
 * physics.waterGravity = physics.gravity / 16;
 * physics.lavaGravity = physics.gravity / 4;
 */
public class PhysicsData {
    public double gravity = 0.08;
    public float airdrag = (float) (1 - 0.02);
    public double yawSpeed = 3.0;
    public double pitchSpeed = 3.0;
    public double playerSpeed = 0.1;
    public double sprintSpeed = 0.3;
    public double sneakSpeed = 0.3;
    public double stepHeight = 0.6;
    public double negligeableVelocity = 0.003;
    public double soulsandSpeed = 0.4;
    public double honeyblockSpeed = 0.4;
    public double honeyblockJumpSpeed = 0.4;
    public double ladderMaxSpeed = 0.15;
    public double ladderClimbSpeed = 0.2;
    public double playerHalfWidth = 0.3;
    public double playerHeight = 1.8;
    public double waterInertia = 0.8;
    public double lavaInertia = 0.5;
    public double liquidAcceleration = 0.02;
    public double airborneInertia = 0.91;
    public double airborneAcceleration = 0.02;
    public double defaultSlipperiness = 0.6;
    public double outOfLiquidImpulse = 0.3;
    public int autojumpCooldown = 10;
    public BubbleColumnDrag bubbleColumnSurfaceDrag = new BubbleColumnDrag(0.03, -0.9, 0.1, 1.8);
    public BubbleColumnDrag bubbleColumnDrag = new BubbleColumnDrag(0.03, -0.3, 0.06, 0.7);
    public double slowFalling = 0.125;
    public double movementSpeedAttribute = 0.125;
    // SPEED_MODIFIER_SPRINTING_UUID is from LivingEntity.java
    public UUID sprintingUUID = UUID.fromString("662a6b8d-da3e-4c1c-8813-96ea6097278d");
    public double waterGravity = gravity / 16;
    public double lavaGravity = gravity / 4;
}
