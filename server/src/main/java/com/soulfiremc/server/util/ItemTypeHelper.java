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
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;

public class ItemTypeHelper {
  private ItemTypeHelper() {}

  public static boolean isSafeFullBlockItem(SFItemStack itemStack) {
    var blockType = BlockItems.getBlockType(itemStack.type());
    return blockType.isPresent() && !blockType.get().fallingBlock();
  }

  public static boolean isTool(SFItemStack itemStack) {
    return itemStack.components().getOptional(DataComponentType.TOOL).isPresent();
  }

  public static boolean isGoodEdibleFood(SFItemStack itemStack) {
    var components = itemStack.components();
    return components.getOptional(DataComponentType.FOOD).map(f -> {
      for (var effect : f.getEffects()) {
        if (EffectType.REGISTRY.getById(effect.getEffect().getEffect().ordinal()).category()
          == EffectType.EffectCategory.HARMFUL) {
          return false;
        }
      }

      return true;
    }).orElse(false);
  }
}
