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

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.OptionalInt;
import java.util.Set;

@Getter
@RequiredArgsConstructor
public enum TierType {
    WOOD(0, 2, Set.of(ItemType.WOODEN_PICKAXE, ItemType.WOODEN_SHOVEL, ItemType.WOODEN_AXE, ItemType.WOODEN_HOE, ItemType.WOODEN_SWORD)),
    STONE(1, 4, Set.of(ItemType.STONE_PICKAXE, ItemType.STONE_SHOVEL, ItemType.STONE_AXE, ItemType.STONE_HOE, ItemType.STONE_SWORD)),
    IRON(2, 6, Set.of(ItemType.IRON_PICKAXE, ItemType.IRON_SHOVEL, ItemType.IRON_AXE, ItemType.IRON_HOE, ItemType.IRON_SWORD)),
    DIAMOND(3, 8, Set.of(ItemType.DIAMOND_PICKAXE, ItemType.DIAMOND_SHOVEL, ItemType.DIAMOND_AXE, ItemType.DIAMOND_HOE, ItemType.DIAMOND_SWORD)),
    GOLD(0, 12, Set.of(ItemType.GOLDEN_PICKAXE, ItemType.GOLDEN_SHOVEL, ItemType.GOLDEN_AXE, ItemType.GOLDEN_HOE, ItemType.GOLDEN_SWORD)),
    NETHERITE(4, 9, Set.of(ItemType.NETHERITE_PICKAXE, ItemType.NETHERITE_SHOVEL, ItemType.NETHERITE_AXE, ItemType.NETHERITE_HOE, ItemType.NETHERITE_SWORD));

    public static final TierType[] VALUES = values();

    private final int level;
    private final float miningSpeed;
    private final Set<ItemType> tools;

    public static OptionalInt getTier(ItemType itemType) {
        for (var tierType : VALUES) {
            // Loop instead of contains because we only need to do a == check
            for (var tool : tierType.tools) {
                if (tool == itemType) {
                    return OptionalInt.of(tierType.level);
                }
            }
        }

        return OptionalInt.empty();
    }
}
