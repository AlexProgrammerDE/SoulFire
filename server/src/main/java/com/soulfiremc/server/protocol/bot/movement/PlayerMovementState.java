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

import com.soulfiremc.server.data.EnchantmentType;
import com.soulfiremc.server.data.ItemType;
import com.soulfiremc.server.protocol.bot.container.PlayerInventoryContainer;
import com.soulfiremc.server.protocol.bot.state.entity.ClientEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;

@Getter
@Setter
@RequiredArgsConstructor
public class PlayerMovementState {
  private final ClientEntity entity;
  private final PlayerInventoryContainer inventoryContainer;

  // Position
  public MutableVector3d pos;

  // Motion
  public MutableVector3d vel;

  // Collision
  public boolean onGround;
  public boolean isCollidedHorizontally;
  public boolean isCollidedVertically;
  public boolean isInWater;
  public boolean isInLava;
  public boolean isInWeb;

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

  public void updateData() {
    pos = new MutableVector3d(entity.x(), entity.y(), entity.z());
    vel = new MutableVector3d(entity.motionX(), entity.motionY(), entity.motionZ());

    var effectState = entity.effectState();
    jumpBoost = effectState.getEffectAmplifier(Effect.JUMP_BOOST);
    speed = effectState.getEffectAmplifier(Effect.SPEED);
    slowness = effectState.getEffectAmplifier(Effect.SLOWNESS);
    dolphinsGrace = effectState.getEffectAmplifier(Effect.DOLPHINS_GRACE);
    slowFalling = effectState.getEffectAmplifier(Effect.SLOW_FALLING);
    levitation = effectState.getEffectAmplifier(Effect.LEVITATION);

    depthStrider = inventoryContainer.getEnchantmentLevel(EnchantmentType.DEPTH_STRIDER);

    var chestItem = inventoryContainer.getChestplate().item();
    elytraEquipped = chestItem != null && chestItem.type() == ItemType.ELYTRA;
  }

  public void applyData() {
    entity.x(pos.x);
    entity.y(pos.y);
    entity.z(pos.z);

    entity.onGround(onGround);

    entity.motionX(vel.x);
    entity.motionY(vel.y);
    entity.motionZ(vel.z);
  }
}
