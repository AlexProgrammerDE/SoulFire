package net.pistonmaster.serverwrecker.data;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public record ItemType(int id, String name, int stackSize,
                       List<String> enchantCategories,
                       DepletionData depletionData,
                       FoodProperties foodProperties) {
    public static final List<ItemType> VALUES = new ArrayList<>();

    // VALUES REPLACE

    public static ItemType register(String name) {
        var itemType = GsonDataHelper.fromJson("/minecraft/items.json", name, ItemType.class);
        VALUES.add(itemType);
        return itemType;
    }

    public static ItemType getById(int id) {
        for (var itemType : VALUES) {
            if (itemType.id() == id) {
                return itemType;
            }
        }

        return null;
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
