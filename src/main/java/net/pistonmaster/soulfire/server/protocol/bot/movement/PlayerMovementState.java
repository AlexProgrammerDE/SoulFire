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
package net.pistonmaster.soulfire.server.protocol.bot.movement;

import com.github.steveice10.mc.protocol.data.game.entity.Effect;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.pistonmaster.soulfire.server.data.ItemType;
import net.pistonmaster.soulfire.server.protocol.bot.container.PlayerInventoryContainer;
import net.pistonmaster.soulfire.server.protocol.bot.model.EffectData;
import net.pistonmaster.soulfire.server.protocol.bot.state.entity.ClientEntity;

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

    // Flying (creative)
    public boolean flying;

    public void updateData() {
        pos = new MutableVector3d(
                entity.x(),
                entity.y(),
                entity.z()
        );

        vel = new MutableVector3d(
                entity.motionX(),
                entity.motionY(),
                entity.motionZ()
        );

        var effectState = entity.effectState();
        jumpBoost = effectState.getEffect(Effect.JUMP_BOOST).map(EffectData::amplifier).orElse(0);
        speed = effectState.getEffect(Effect.SPEED).map(EffectData::amplifier).orElse(0);
        slowness = effectState.getEffect(Effect.SLOWNESS).map(EffectData::amplifier).orElse(0);
        dolphinsGrace = effectState.getEffect(Effect.DOLPHINS_GRACE).map(EffectData::amplifier).orElse(0);
        slowFalling = effectState.getEffect(Effect.SLOW_FALLING).map(EffectData::amplifier).orElse(0);
        levitation = effectState.getEffect(Effect.LEVITATION).map(EffectData::amplifier).orElse(0);

        var bootsItem = inventoryContainer.getBoots().item();
        depthStrider = bootsItem == null ? 0 : bootsItem.getEnchantmentLevel("minecraft:depth_strider");

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
