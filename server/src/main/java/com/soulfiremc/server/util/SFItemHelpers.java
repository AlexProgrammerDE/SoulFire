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
package com.soulfiremc.server.util;

import com.soulfiremc.server.data.BlockItems;
import com.soulfiremc.server.data.EffectType;
import com.soulfiremc.server.protocol.bot.container.SFItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ConsumeEffect;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;

public class SFItemHelpers {
  private SFItemHelpers() {}

  public static boolean isSafeFullBlockItem(SFItemStack itemStack) {
    var blockType = BlockItems.getBlockType(itemStack.type());
    return blockType.isPresent() && !blockType.get().fallingBlock();
  }

  public static boolean isTool(SFItemStack itemStack) {
    return itemStack.getDataComponents().getOptional(DataComponentTypes.TOOL).isPresent();
  }

  public static boolean isGoodEdibleFood(SFItemStack itemStack) {
    var components = itemStack.getDataComponents();
    return components.getOptional(DataComponentTypes.CONSUMABLE).map(f -> {
      for (var consumeEffects : f.onConsumeEffects()) {
        if (!(consumeEffects instanceof ConsumeEffect.ApplyEffects applyEffects)) {
          continue;
        }

        for (var mobEffect : applyEffects.effects()) {
          if (EffectType.REGISTRY.getById(mobEffect.getEffect().ordinal()).category()
            == EffectType.EffectCategory.HARMFUL) {
            return false;
          }
        }
      }

      return true;
    }).orElse(false);
  }
}
