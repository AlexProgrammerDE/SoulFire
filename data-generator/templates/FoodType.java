package net.pistonmaster.serverwrecker.data;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public record FoodType(ItemType itemType, int foodPoints, double saturation, double effectiveQuality, double saturationRatio) {
    public static final List<FoodType> VALUES = new ArrayList<>();

    // VALUES REPLACE

    public static FoodType register(FoodType foodType) {
        VALUES.add(foodType);
        return foodType;
    }
}
