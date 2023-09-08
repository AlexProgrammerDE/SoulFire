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

import java.util.List;

/**
 * Food to avoid eating or else you may get hurt!
 */
public class DangerFood {
    public static final List<FoodType> VALUES = List.of(
            FoodType.POISONOUS_POTATO, // Poison
            FoodType.CHORUS_FRUIT, // Teleports you randomly
            FoodType.SUSPICIOUS_STEW, // Random effect can give bad stuff like wither and poison
            FoodType.SPIDER_EYE, // Poison
            FoodType.ROTTEN_FLESH, // Hunger
            FoodType.PUFFERFISH, // Poison, Nausea and Hunger
            FoodType.CHICKEN // Hunger (Raw chicken)
    );

    public static boolean isDangerFood(FoodType foodType) {
        return VALUES.contains(foodType);
    }
}
