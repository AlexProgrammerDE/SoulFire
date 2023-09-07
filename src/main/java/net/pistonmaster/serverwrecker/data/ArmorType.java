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

import java.util.Arrays;
import java.util.List;

@Getter
public enum ArmorType {
    HELMET(ItemType.LEATHER_HELMET, ItemType.CHAINMAIL_HELMET, ItemType.IRON_HELMET, ItemType.GOLDEN_HELMET, ItemType.DIAMOND_HELMET, ItemType.NETHERITE_HELMET),
    CHESTPLATE(ItemType.LEATHER_CHESTPLATE, ItemType.CHAINMAIL_CHESTPLATE, ItemType.IRON_CHESTPLATE, ItemType.GOLDEN_CHESTPLATE, ItemType.DIAMOND_CHESTPLATE, ItemType.NETHERITE_CHESTPLATE),
    LEGGINGS(ItemType.LEATHER_LEGGINGS, ItemType.CHAINMAIL_LEGGINGS, ItemType.IRON_LEGGINGS, ItemType.GOLDEN_LEGGINGS, ItemType.DIAMOND_LEGGINGS, ItemType.NETHERITE_LEGGINGS),
    BOOTS(ItemType.LEATHER_BOOTS, ItemType.CHAINMAIL_BOOTS, ItemType.IRON_BOOTS, ItemType.GOLDEN_BOOTS, ItemType.DIAMOND_BOOTS, ItemType.NETHERITE_BOOTS);

    private final List<ItemType> itemTypes;

    ArmorType(ItemType... itemTypes) {
        this.itemTypes = Arrays.asList(itemTypes);
    }
}
