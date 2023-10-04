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
package net.pistonmaster.serverwrecker.data;

import net.pistonmaster.serverwrecker.protocol.bot.state.tag.TagsState;

import java.util.Set;

import static net.pistonmaster.serverwrecker.data.ItemType.*;

public class ToolSpeedType {
    private static final Set<ItemType> SWORDS = Set.of(WOODEN_SWORD, STONE_SWORD, IRON_SWORD, GOLDEN_SWORD, DIAMOND_SWORD, NETHERITE_SWORD);

    public static float getBlockToolSpeed(TagsState tagsState, ItemType itemType, BlockType blockType) {
        if (SWORDS.contains(itemType)) {
            if (blockType == BlockType.COBWEB) {
                return 15;
                // TODO: Also handle plant, replaceable_plant and vegetable here in the future.
                // Skipped adding it because we don't have the data of block inherits what Material.
            } else if (tagsState.isBlockInTag(blockType, "leaves")) {
                return 1;
            } else {
                return 1.5F;
            }
        } else if (itemType == SHEARS) {
            if (blockType == BlockType.COBWEB || tagsState.isBlockInTag(blockType, "leaves")) {
                return 15;
            } else if (tagsState.isBlockInTag(blockType, "wool")) {
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
            if (!toolSpeedType.getTools().contains(itemType)) {
                continue;
            }

            var tagName = MineableType.getFromTool(itemType).orElseThrow().getTagName();
            if (tagsState.isBlockInTag(blockType, tagName)) {
                return toolSpeedType.getMiningSpeed();
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
        } else if (itemType == SHEARS) {
            return blockType == BlockType.COBWEB || blockType == BlockType.REDSTONE_WIRE || blockType == BlockType.TRIPWIRE;
        } else {
            var tier = TierType.getTier(itemType);
            if (tier.isEmpty()) {
                return false;
            }

            var i = tier.getAsInt();
            if (i < 3 && tagsState.isBlockInTag(blockType, "needs_diamond_tool")) {
                return false;
            } else if (i < 2 && tagsState.isBlockInTag(blockType, "needs_iron_tool")) {
                return false;
            } else if (i < 1 && tagsState.isBlockInTag(blockType, "needs_stone_tool")) {
                return false;
            }

            return MineableType.getFromTool(itemType).filter(type ->
                    tagsState.isBlockInTag(blockType, type.getTagName())).isPresent();
        }
    }
}
