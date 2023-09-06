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

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public record FoodType(ItemType itemType, int foodPoints, double saturation, double effectiveQuality, double saturationRatio) {
    public static final List<FoodType> VALUES = new ArrayList<>();

    public static final FoodType APPLE = register(new FoodType(ItemType.APPLE, 4, 2.4, 6.4, 0.6));
    public static final FoodType MUSHROOM_STEW = register(new FoodType(ItemType.MUSHROOM_STEW, 6, 7.2000003, 13.200001, 1.2));
    public static final FoodType BREAD = register(new FoodType(ItemType.BREAD, 5, 6, 11, 1.2));
    public static final FoodType PORKCHOP = register(new FoodType(ItemType.PORKCHOP, 3, 1.8000001, 4.8, 0.6));
    public static final FoodType COOKED_PORKCHOP = register(new FoodType(ItemType.COOKED_PORKCHOP, 8, 12.8, 20.8, 1.6));
    public static final FoodType GOLDEN_APPLE = register(new FoodType(ItemType.GOLDEN_APPLE, 4, 9.6, 13.6, 2.4));
    public static final FoodType ENCHANTED_GOLDEN_APPLE = register(new FoodType(ItemType.ENCHANTED_GOLDEN_APPLE, 4, 9.6, 13.6, 2.4));
    public static final FoodType COD = register(new FoodType(ItemType.COD, 2, 0.4, 2.4, 0.2));
    public static final FoodType SALMON = register(new FoodType(ItemType.SALMON, 2, 0.4, 2.4, 0.2));
    public static final FoodType TROPICAL_FISH = register(new FoodType(ItemType.TROPICAL_FISH, 1, 0.2, 1.2, 0.2));
    public static final FoodType PUFFERFISH = register(new FoodType(ItemType.PUFFERFISH, 1, 0.2, 1.2, 0.2));
    public static final FoodType COOKED_COD = register(new FoodType(ItemType.COOKED_COD, 5, 6, 11, 1.2));
    public static final FoodType COOKED_SALMON = register(new FoodType(ItemType.COOKED_SALMON, 6, 9.6, 15.6, 1.6));
    public static final FoodType COOKIE = register(new FoodType(ItemType.COOKIE, 2, 0.4, 2.4, 0.2));
    public static final FoodType MELON_SLICE = register(new FoodType(ItemType.MELON_SLICE, 2, 1.2, 3.2, 0.6));
    public static final FoodType DRIED_KELP = register(new FoodType(ItemType.DRIED_KELP, 1, 0.6, 1.6, 0.6));
    public static final FoodType BEEF = register(new FoodType(ItemType.BEEF, 3, 1.8000001, 4.8, 0.6));
    public static final FoodType COOKED_BEEF = register(new FoodType(ItemType.COOKED_BEEF, 8, 12.8, 20.8, 1.6));
    public static final FoodType CHICKEN = register(new FoodType(ItemType.CHICKEN, 2, 1.2, 3.2, 0.6));
    public static final FoodType COOKED_CHICKEN = register(new FoodType(ItemType.COOKED_CHICKEN, 6, 7.2000003, 13.200001, 1.2));
    public static final FoodType ROTTEN_FLESH = register(new FoodType(ItemType.ROTTEN_FLESH, 4, 0.8, 4.8, 0.2));
    public static final FoodType SPIDER_EYE = register(new FoodType(ItemType.SPIDER_EYE, 2, 3.2, 5.2, 1.6));
    public static final FoodType CARROT = register(new FoodType(ItemType.CARROT, 3, 3.6000001, 6.6000004, 1.2));
    public static final FoodType POTATO = register(new FoodType(ItemType.POTATO, 1, 0.6, 1.6, 0.6));
    public static final FoodType BAKED_POTATO = register(new FoodType(ItemType.BAKED_POTATO, 5, 6, 11, 1.2));
    public static final FoodType POISONOUS_POTATO = register(new FoodType(ItemType.POISONOUS_POTATO, 2, 1.2, 3.2, 0.6));
    public static final FoodType GOLDEN_CARROT = register(new FoodType(ItemType.GOLDEN_CARROT, 6, 14.400001, 20.400002, 2.4));
    public static final FoodType PUMPKIN_PIE = register(new FoodType(ItemType.PUMPKIN_PIE, 8, 4.8, 12.8, 0.6));
    public static final FoodType RABBIT = register(new FoodType(ItemType.RABBIT, 3, 1.8000001, 4.8, 0.6));
    public static final FoodType COOKED_RABBIT = register(new FoodType(ItemType.COOKED_RABBIT, 5, 6, 11, 1.2));
    public static final FoodType RABBIT_STEW = register(new FoodType(ItemType.RABBIT_STEW, 10, 12, 22, 1.2));
    public static final FoodType MUTTON = register(new FoodType(ItemType.MUTTON, 2, 1.2, 3.2, 0.6));
    public static final FoodType COOKED_MUTTON = register(new FoodType(ItemType.COOKED_MUTTON, 6, 9.6, 15.6, 1.6));
    public static final FoodType CHORUS_FRUIT = register(new FoodType(ItemType.CHORUS_FRUIT, 4, 2.4, 6.4, 0.6));
    public static final FoodType BEETROOT = register(new FoodType(ItemType.BEETROOT, 1, 1.2, 2.2, 1.2));
    public static final FoodType BEETROOT_SOUP = register(new FoodType(ItemType.BEETROOT_SOUP, 6, 7.2000003, 13.200001, 1.2));
    public static final FoodType SUSPICIOUS_STEW = register(new FoodType(ItemType.SUSPICIOUS_STEW, 6, 7.2000003, 13.200001, 1.2));
    public static final FoodType SWEET_BERRIES = register(new FoodType(ItemType.SWEET_BERRIES, 2, 0.4, 2.4, 0.2));
    public static final FoodType GLOW_BERRIES = register(new FoodType(ItemType.GLOW_BERRIES, 2, 0.4, 2.4, 0.2));
    public static final FoodType HONEY_BOTTLE = register(new FoodType(ItemType.HONEY_BOTTLE, 6, 1.2, 7.2, 0.2));

    public static FoodType register(FoodType foodType) {
        VALUES.add(foodType);
        return foodType;
    }
}
