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
package net.pistonmaster.soulfire.data;

import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;

import java.util.List;

@SuppressWarnings("unused")
public record ItemType(int id, String name, int stackSize,
                       List<String> enchantCategories,
                       DepletionData depletionData,
                       FoodProperties foodProperties) {
    public static final Int2ReferenceMap<ItemType> FROM_ID = new Int2ReferenceOpenHashMap<>();

    // VALUES REPLACE

    public static ItemType register(String name) {
        var itemType = GsonDataHelper.fromJson("/minecraft/items.json", name, ItemType.class);
        FROM_ID.put(itemType.id(), itemType);
        return itemType;
    }

    public static ItemType getById(int id) {
        return FROM_ID.get(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemType itemType)) return false;
        return id == itemType.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    public record DepletionData(List<String> repairWith, int maxDamage) {
    }

    public record FoodProperties(int nutrition, float saturationModifier,
                                 boolean fastFood, boolean isMeat,
                                 boolean canAlwaysEat, boolean possiblyHarmful) {
    }
}
