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

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Optional;

public final class BlockItems {
  public static final Block[] VALUES = new Block[BuiltInRegistries.ITEM.size()];
  public static final Item[] VALUES_REVERSE = new Item[BuiltInRegistries.BLOCK.size()];

  static {
    for (var itemType : BuiltInRegistries.ITEM.entrySet()) {
      for (var blockType : BuiltInRegistries.BLOCK.entrySet()) {
        // Let's not use bedrock as a building block
        if (SFBlockHelpers.isDiggable(blockType.getValue())
          && itemType.getKey().identifier().equals(blockType.getKey().identifier())
          && SFBlockHelpers.isCollisionShapeFullBlock(blockType.getValue().defaultBlockState())) {
          VALUES[BuiltInRegistries.ITEM.getId(itemType.getValue())] = blockType.getValue();
          VALUES_REVERSE[BuiltInRegistries.BLOCK.getId(blockType.getValue())] = itemType.getValue();
        }
      }
    }
  }

  private BlockItems() {
  }

  public static Optional<Block> getBlock(Item itemType) {
    return Optional.ofNullable(VALUES[BuiltInRegistries.ITEM.getId(itemType)]);
  }

  public static Optional<Item> getItem(Block blockType) {
    return Optional.ofNullable(VALUES_REVERSE[BuiltInRegistries.BLOCK.getId(blockType)]);
  }

  public static boolean hasItem(Block blockType) {
    return VALUES_REVERSE[BuiltInRegistries.BLOCK.getId(blockType)] != null;
  }
}
