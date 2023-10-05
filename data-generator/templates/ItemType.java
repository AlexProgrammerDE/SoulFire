package net.pistonmaster.serverwrecker.data;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public record ItemType(int id, String name, String displayName, int stackSize, List<String> enchantCategories,
                       List<String> repairWith, int maxDurability) {
    public static final List<ItemType> VALUES = new ArrayList<>();

    // VALUES REPLACE

    public static ItemType register(ItemType itemType) {
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
}
