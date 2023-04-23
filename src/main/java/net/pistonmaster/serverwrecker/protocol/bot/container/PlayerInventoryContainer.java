package net.pistonmaster.serverwrecker.protocol.bot.container;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;

public class PlayerInventoryContainer extends Container {
    public PlayerInventoryContainer() {
        super(46, 0);
    }

    public ItemStack[] getMainInventory() {
        return getSlots(9, 35);
    }

    public ItemStack[] getHotbar() {
        return getSlots(36, 44);
    }

    public ItemStack getOffhand() {
        return getSlot(45);
    }

    public ItemStack getHelmet() {
        return getSlot(5);
    }

    public ItemStack getChestplate() {
        return getSlot(6);
    }

    public ItemStack getLeggings() {
        return getSlot(7);
    }

    public ItemStack getBoots() {
        return getSlot(8);
    }

    public ItemStack getCraftingResult() {
        return getSlot(0);
    }

    public ItemStack[] getCraftingGrid() {
        return getSlots(1, 4);
    }

    public ItemStack[] getArmor() {
        return getSlots(5, 8);
    }
}
