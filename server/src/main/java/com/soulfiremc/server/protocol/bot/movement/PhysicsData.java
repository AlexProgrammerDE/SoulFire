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
import com.soulfiremc.server.data.ModifierOperation;
import lombok.Data;
import net.kyori.adventure.key.Key;

@Data
public class PhysicsData {
  // From LivingEntity.java
  public static final Key SPRINTING_MODIFIER_ID = Key.key("sprinting");
  public static final Attribute.Modifier SPEED_MODIFIER_SPRINTING = new Attribute.Modifier(
    SPRINTING_MODIFIER_ID, 0.3F, ModifierOperation.ADD_MULTIPLIED_TOTAL
  );
  public double gravity = 0.08;
  public float airdrag = 0.98F;
  public double sprintSpeed = 0.3F;
  public double sneakSpeed = 0.3;
  public double stepHeight = 0.6F;
  public double negligeableVelocity = 0.003;
  public double soulsandSpeed = 0.4;
  public double honeyblockSpeed = 0.4;
  public double honeyblockJumpSpeed = 0.4;
  public double climbMaxSpeed = 0.15;
  public double climbSpeed = 0.2;
  public float waterSpeed = 0.8F;
  public float lavaSpeed = 0.5F;
  public float liquidSpeed = 0.02F;
  public float defaultSlipperiness = 0.6F;
  public double outOfLiquidImpulse = 0.3;
  public int autojumpCooldown = 10;
  public BubbleColumnDrag bubbleColumnSurfaceDrag = new BubbleColumnDrag(0.03, -0.9, 0.1, 1.8);
  public BubbleColumnDrag bubbleColumnDrag = new BubbleColumnDrag(0.03, -0.3, 0.06, 0.7);
  public double slowFalling = 0.125;
  public double waterGravity = gravity / 16;
  public double lavaGravity = gravity / 4;
}
