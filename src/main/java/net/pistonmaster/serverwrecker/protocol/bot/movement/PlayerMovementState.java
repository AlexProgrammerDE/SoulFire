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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.pistonmaster.serverwrecker.protocol.bot.state.EntityAttributesState;

@Getter
@Setter
@RequiredArgsConstructor
public class PlayerMovementState {
    private final EntityAttributesState attributesState;

    // Position
    public MutableVector3d pos;

    // Rotation
    public float yaw;
    public float pitch;

    // Motion
    public MutableVector3d vel = new MutableVector3d(0, 0, 0);

    // Collision
    public boolean onGround;
    public boolean isInWater;
    public boolean isInLava;
    public boolean isInWeb;
    public boolean isCollidedHorizontally;
    public boolean isCollidedVertically;

    // State
    public boolean elytraFlying;
    public int jumpTicks;
    public boolean jumpQueued;
    public int fireworkRocketDuration;

    // Effects
    public int jumpBoost;
    public int speed;
    public int slowness;
    public int dolphinsGrace;
    public int slowFalling;
    public int levitation;
    public int depthStrider;

    // Inventory
    public boolean elytraEquipped;
}
