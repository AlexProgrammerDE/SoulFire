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

import com.github.steveice10.mc.protocol.data.game.entity.Effect;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.pistonmaster.serverwrecker.data.ItemType;
import net.pistonmaster.serverwrecker.protocol.bot.container.PlayerInventoryContainer;
import net.pistonmaster.serverwrecker.protocol.bot.model.EffectData;
import net.pistonmaster.serverwrecker.protocol.bot.state.EntityAttributesState;
import net.pistonmaster.serverwrecker.protocol.bot.state.EntityEffectState;

@Getter
@Setter
@RequiredArgsConstructor
public class PlayerMovementState {
    private final EntityAttributesState attributesState;
    private final EntityEffectState effectState;
    private final PlayerInventoryContainer inventoryContainer;

    // Position
    public MutableVector3d pos;

    // Rotation
    public float yaw;
    public float pitch;

    // Motion
    public MutableVector3d vel = new MutableVector3d(0, 0, 0);

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
        jumpBoost = effectState.getEffect(Effect.JUMP_BOOST).map(EffectData::amplifier).orElse(0);
        speed = effectState.getEffect(Effect.SPEED).map(EffectData::amplifier).orElse(0);
        slowness = effectState.getEffect(Effect.SLOWNESS).map(EffectData::amplifier).orElse(0);
        dolphinsGrace = effectState.getEffect(Effect.DOLPHINS_GRACE).map(EffectData::amplifier).orElse(0);
        slowFalling = effectState.getEffect(Effect.SLOW_FALLING).map(EffectData::amplifier).orElse(0);
        levitation = effectState.getEffect(Effect.LEVITATION).map(EffectData::amplifier).orElse(0);

        var bootsItem = inventoryContainer.getBoots().item();
        depthStrider = bootsItem == null ? 0 : bootsItem.getEnchantmentLevel("minecraft:depth_strider");

        var chestItem = inventoryContainer.getChestplate().item();
        elytraEquipped = chestItem != null && chestItem.getType() == ItemType.ELYTRA;
    }
}
