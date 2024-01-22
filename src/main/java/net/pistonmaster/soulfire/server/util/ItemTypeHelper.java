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
package net.pistonmaster.soulfire.server.util;

import net.pistonmaster.soulfire.server.data.BlockItems;
import net.pistonmaster.soulfire.server.data.ItemType;
import net.pistonmaster.soulfire.server.data.TierType;

public class ItemTypeHelper {
    private ItemTypeHelper() {
    }

    public static boolean isSafeFullBlockItem(ItemType type) {
        return BlockItems.getBlockType(type).isPresent() && !isUnsafeToPlace(type);
    }

    public static boolean isTool(ItemType type) {
        return TierType.getTier(type).isPresent() || type == ItemType.SHEARS;
    }

    public static boolean isUnsafeToPlace(ItemType type) {
        return type == ItemType.SAND
                || type == ItemType.GRAVEL;
    }

    public static boolean isGoodEdibleFood(ItemType type) {
        return type.foodProperties() != null && !type.foodProperties().possiblyHarmful();
    }
}
