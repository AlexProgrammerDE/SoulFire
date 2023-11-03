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

import java.util.UUID;

public class PhysicsData {
    public double gravity = 0.08;
    public double airdrag = 0.9800000190734863D;
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
    public double playerHalfWidth = 0.3D;
    public double playerHeight = 1.8D;
    public double playerSneakHeight = 1.5D;
    public double waterInertia = 0.8;
    public double lavaInertia = 0.5;
    public double liquidAcceleration = 0.02;
    public float airborneAcceleration = 0.02F;
    public float defaultSlipperiness = 0.6F;
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
