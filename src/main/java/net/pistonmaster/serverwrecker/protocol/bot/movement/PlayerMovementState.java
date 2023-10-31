package net.pistonmaster.serverwrecker.protocol.bot.movement;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.protocol.bot.state.EntityAttributesState;

@RequiredArgsConstructor
public class PlayerMovementState {
    @Getter
    private final EntityAttributesState attributesState;
    public MutableVector3d pos;
    public MutableVector3d vel;
    public boolean onGround;
    public boolean isInWater;
    public boolean isInLava;
    public boolean isInWeb;
    public boolean isCollidedHorizontally;
    public boolean isCollidedVertically;
    public boolean elytraFlying;
    public int jumpTicks;
    public boolean jumpQueued;
    public int fireworkRocketDuration;
    public float yaw;
    public float pitch;
    public ControlState control;
    public int jumpBoost;
    public int speed;
    public int slowness;
    public int dolphinsGrace;
    public int slowFalling;
    public int levitation;
    public int depthStrider;
    public boolean elytraEquipped;
}
