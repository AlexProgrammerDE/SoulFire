package net.pistonmaster.serverwrecker.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@RequiredArgsConstructor
public final class ItemType {
    public static final List<ItemType> VALUES = new ArrayList<>();

    // VALUES REPLACE

    private final int id;
    private final String name;
    private final String displayName;
    private final int stackSize;
    private final List<String> enchantCategories;
    private final List<String> repairWith;
    private final int maxDurability;

    public static ItemType register(ItemType itemType) {
        VALUES.add(itemType);
        return itemType;
    }

    public static ItemType getById(int id) {
        for (ItemType itemType : VALUES) {
            if (itemType.getId() == id) {
                return itemType;
            }
        }

        return null;
    }
}
