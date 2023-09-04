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

import lombok.Getter;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Getter
public enum ToolType {
    PICKAXE(ItemType.WOODEN_PICKAXE, ItemType.STONE_PICKAXE, ItemType.IRON_PICKAXE, ItemType.GOLDEN_PICKAXE, ItemType.DIAMOND_PICKAXE, ItemType.NETHERITE_PICKAXE),
    AXE(ItemType.WOODEN_AXE, ItemType.STONE_AXE, ItemType.IRON_AXE, ItemType.GOLDEN_AXE, ItemType.DIAMOND_AXE, ItemType.NETHERITE_AXE),
    SHOVEL(ItemType.WOODEN_SHOVEL, ItemType.STONE_SHOVEL, ItemType.IRON_SHOVEL, ItemType.GOLDEN_SHOVEL, ItemType.DIAMOND_SHOVEL, ItemType.NETHERITE_SHOVEL),
    HOE(ItemType.WOODEN_HOE, ItemType.STONE_HOE, ItemType.IRON_HOE, ItemType.GOLDEN_HOE, ItemType.DIAMOND_HOE, ItemType.NETHERITE_HOE),
    SWORD(ItemType.WOODEN_SWORD, ItemType.STONE_SWORD, ItemType.IRON_SWORD, ItemType.GOLDEN_SWORD, ItemType.DIAMOND_SWORD, ItemType.NETHERITE_SWORD),
    SHEARS(ItemType.SHEARS);

    private final String mineableId = "mineable/" + name().toLowerCase(Locale.ENGLISH);
    private final Set<ItemType> itemTypes;

    ToolType(ItemType... itemTypes) {
        this.itemTypes = Set.of(itemTypes);
    }

    public static boolean isToolFor(ItemType itemType, BlockType blockType) {
        if (!blockType.diggable()) {
            return false;
        }

        String material = blockType.material();
        String[] materials = material.split(";");

        Optional<ToolType> toolType = matchToolFor(itemType);

        if (toolType.isPresent()) {
            for (String materialType : materials) {
                if (toolType.get().mineableId.equals(materialType)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static Optional<ToolType> matchToolFor(ItemType itemType) {
        for (ToolType toolType : values()) {
            if (toolType.itemTypes.contains(itemType)) {
                return Optional.of(toolType);
            }
        }

        return Optional.empty();
    }
}
