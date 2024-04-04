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
package com.soulfiremc.server.data;

import com.soulfiremc.server.protocol.bot.state.TagsState;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ToolSpeedType {
  public static float getBlockToolSpeed(
    TagsState tagsState, ItemType itemType, BlockType blockType) {
    if (tagsState.isItemInTag(itemType, ItemTags.SWORDS)) {
      if (blockType == BlockType.COBWEB) {
        return 15;
      } else if (tagsState.isBlockInTag(blockType, BlockTags.SWORD_EFFICIENT)) {
        return 1.5F;
      } else {
        return 1;
      }
    } else if (itemType == ItemType.SHEARS) {
      if (blockType == BlockType.COBWEB || tagsState.isBlockInTag(blockType, BlockTags.LEAVES)) {
        return 15;
      } else if (tagsState.isBlockInTag(blockType, BlockTags.WOOL)) {
        return 5;
      } else if (blockType == BlockType.VINE || blockType == BlockType.GLOW_LICHEN) {
        return 1;
      } else {
        return 2;
      }
    } else {
      return getTierToolSpeed(tagsState, itemType, blockType);
    }
  }

  private static float getTierToolSpeed(
    TagsState tagsState, ItemType itemType, BlockType blockType) {
    var tierType = itemType.tierType();
    if (tierType != null) {
      var tagName = MineableType.getFromTool(tagsState, itemType);
      if (tagName.isPresent() && tagsState.isBlockInTag(blockType, tagName.get().blockTagKey())) {
        return tierType.miningSpeed();
      }
    }

    // Base speed
    return 1;
  }

  public static boolean isRightToolFor(
    TagsState tagsState, ItemType itemType, BlockType blockType) {
    if (tagsState.isItemInTag(itemType, ItemTags.SWORDS)) {
      return blockType == BlockType.COBWEB;
    } else if (itemType == ItemType.SHEARS) {
      return blockType == BlockType.COBWEB
        || blockType == BlockType.REDSTONE_WIRE
        || blockType == BlockType.TRIPWIRE;
    } else {
      var tier = itemType.tierType();
      if (tier == null) {
        return false;
      }

      var level = tier.level();
      if (level < 3 && tagsState.isBlockInTag(blockType, BlockTags.NEEDS_DIAMOND_TOOL)) {
        return false;
      } else if (level < 2 && tagsState.isBlockInTag(blockType, BlockTags.NEEDS_IRON_TOOL)) {
        return false;
      } else if (level < 1 && tagsState.isBlockInTag(blockType, BlockTags.NEEDS_STONE_TOOL)) {
        return false;
      }

      return MineableType.getFromTool(tagsState, itemType)
        .filter(type -> tagsState.isBlockInTag(blockType, type.blockTagKey()))
        .isPresent();
    }
  }
}
