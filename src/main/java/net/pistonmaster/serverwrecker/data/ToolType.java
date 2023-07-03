package net.pistonmaster.serverwrecker.data;

import lombok.Getter;

import java.util.Locale;
import java.util.Set;

@Getter
public enum ToolType {
    PICKAXE(ItemType.WOODEN_PICKAXE, ItemType.STONE_PICKAXE, ItemType.IRON_PICKAXE, ItemType.GOLDEN_PICKAXE, ItemType.DIAMOND_PICKAXE, ItemType.NETHERITE_PICKAXE),
    AXE(ItemType.WOODEN_AXE, ItemType.STONE_AXE, ItemType.IRON_AXE, ItemType.GOLDEN_AXE, ItemType.DIAMOND_AXE, ItemType.NETHERITE_AXE),
    SHOVEL(ItemType.WOODEN_SHOVEL, ItemType.STONE_SHOVEL, ItemType.IRON_SHOVEL, ItemType.GOLDEN_SHOVEL, ItemType.DIAMOND_SHOVEL, ItemType.NETHERITE_SHOVEL),
    HOE(ItemType.WOODEN_HOE, ItemType.STONE_HOE, ItemType.IRON_HOE, ItemType.GOLDEN_HOE, ItemType.DIAMOND_HOE, ItemType.NETHERITE_HOE),
    SWORD(ItemType.WOODEN_SWORD, ItemType.STONE_SWORD, ItemType.IRON_SWORD, ItemType.GOLDEN_SWORD, ItemType.DIAMOND_SWORD, ItemType.NETHERITE_SWORD),
    SHEARS(ItemType.SHEARS);

    private final String mineableId = "mineable/" + name().toLowerCase(Locale.ENGLISH);
    private final Set<ItemType> itemTypes;

    ToolType(ItemType... itemTypes) {
        this.itemTypes = Set.of(itemTypes);
    }
}
