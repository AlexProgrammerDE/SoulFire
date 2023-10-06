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
import lombok.RequiredArgsConstructor;

import java.util.OptionalInt;
import java.util.Set;

import static net.pistonmaster.serverwrecker.data.ItemType.*;

@Getter
@RequiredArgsConstructor
public enum TierType {
    WOOD(0, 2, Set.of(WOODEN_PICKAXE, WOODEN_SHOVEL, WOODEN_AXE, WOODEN_HOE, WOODEN_SWORD)),
    STONE(1, 4, Set.of(STONE_PICKAXE, STONE_SHOVEL, STONE_AXE, STONE_HOE, STONE_SWORD)),
    IRON(2, 6, Set.of(IRON_PICKAXE, IRON_SHOVEL, IRON_AXE, IRON_HOE, IRON_SWORD)),
    DIAMOND(3, 8, Set.of(DIAMOND_PICKAXE, DIAMOND_SHOVEL, DIAMOND_AXE, DIAMOND_HOE, DIAMOND_SWORD)),
    GOLD(0, 12, Set.of(GOLDEN_PICKAXE, GOLDEN_SHOVEL, GOLDEN_AXE, GOLDEN_HOE, GOLDEN_SWORD)),
    NETHERITE(4, 9, Set.of(NETHERITE_PICKAXE, NETHERITE_SHOVEL, NETHERITE_AXE, NETHERITE_HOE, NETHERITE_SWORD));

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
