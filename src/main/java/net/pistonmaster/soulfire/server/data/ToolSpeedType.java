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
package net.pistonmaster.soulfire.server.data;

import net.pistonmaster.soulfire.server.protocol.bot.state.TagsState;

import java.util.Set;

public class ToolSpeedType {
    private static final Set<ItemType> SWORDS = Set.of(ItemType.WOODEN_SWORD, ItemType.STONE_SWORD, ItemType.IRON_SWORD, ItemType.GOLDEN_SWORD, ItemType.DIAMOND_SWORD, ItemType.NETHERITE_SWORD);

    public static float getBlockToolSpeed(TagsState tagsState, ItemType itemType, BlockType blockType) {
        if (SWORDS.contains(itemType)) {
            if (blockType == BlockType.COBWEB) {
                return 15;
            } else if (tagsState.isBlockInTag(blockType, BlockTags.LEAVES)) {
                return 1;
            } else {
                return 1.5F;
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
            return getEnumToolSpeed(tagsState, itemType, blockType);
        }
    }

    private static float getEnumToolSpeed(TagsState tagsState, ItemType itemType, BlockType blockType) {
        for (var toolSpeedType : TierType.values()) {
            if (!toolSpeedType.tools().contains(itemType)) {
                continue;
            }

            var tagName = MineableType.getFromTool(itemType).orElseThrow().tagName();
            if (tagsState.isBlockInTag(blockType, tagName)) {
                return toolSpeedType.miningSpeed();
            } else {
                return 1;
            }
        }

        // Base speed
        return 1;
    }

    public static boolean isRightToolFor(TagsState tagsState, ItemType itemType, BlockType blockType) {
        if (SWORDS.contains(itemType)) {
            return blockType == BlockType.COBWEB;
        } else if (itemType == ItemType.SHEARS) {
            return blockType == BlockType.COBWEB || blockType == BlockType.REDSTONE_WIRE || blockType == BlockType.TRIPWIRE;
        } else {
            var tier = TierType.getTier(itemType);
            if (tier.isEmpty()) {
                return false;
            }

            var i = tier.getAsInt();
            if (i < 3 && tagsState.isBlockInTag(blockType, BlockTags.NEEDS_DIAMOND_TOOL)) {
                return false;
            } else if (i < 2 && tagsState.isBlockInTag(blockType, BlockTags.NEEDS_IRON_TOOL)) {
                return false;
            } else if (i < 1 && tagsState.isBlockInTag(blockType, BlockTags.NEEDS_STONE_TOOL)) {
                return false;
            }

            return MineableType.getFromTool(itemType).filter(type ->
                    tagsState.isBlockInTag(blockType, type.tagName())).isPresent();
        }
    }
}
