package net.pistonmaster.serverwrecker.data;

import java.util.ArrayList;
import java.util.List;

public record ItemType(int id, String name, String displayName, int stackSize, List<String> enchantCategories,
                       List<String> repairWith, int maxDurability) {
    public static final List<ItemType> VALUES = new ArrayList<>();

    // VALUES REPLACE

    public static ItemType register(ItemType itemType) {
        VALUES.add(itemType);
        return itemType;
    }

    public static ItemType getById(int id) {
        for (ItemType itemType : VALUES) {
            if (itemType.id() == id) {
                return itemType;
            }
        }

        return null;
    }
}
