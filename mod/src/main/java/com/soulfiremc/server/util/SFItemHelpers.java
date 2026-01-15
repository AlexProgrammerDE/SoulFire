/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.block.FallingBlock;

import java.util.Optional;

public final class SFItemHelpers {
  private SFItemHelpers() {}

  public static boolean isSafeFullBlockItem(ItemStack itemStack) {
    var blockType = BlockItems.getBlock(itemStack.getItem());
    return blockType.isPresent() && !(blockType.get() instanceof FallingBlock);
  }

  public static boolean isTool(ItemStack itemStack) {
    return itemStack.getComponents().get(DataComponents.TOOL) != null;
  }

  public static boolean isGoodEdibleFood(ItemStack itemStack) {
    var components = itemStack.getComponents();
    return Optional.ofNullable(components.get(DataComponents.CONSUMABLE)).map(f -> {
      for (var consumeEffects : f.onConsumeEffects()) {
        if (!(consumeEffects instanceof ApplyStatusEffectsConsumeEffect applyEffects)) {
          continue;
        }

        for (var mobEffect : applyEffects.effects()) {
          if (mobEffect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
            return false;
          }
        }
      }

      return true;
    }).orElse(false);
  }
}
