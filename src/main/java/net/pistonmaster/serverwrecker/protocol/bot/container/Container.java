package net.pistonmaster.serverwrecker.protocol.bot.container;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import lombok.Getter;

@Getter
public class Container {
    private final ItemStack[] slots;
    private final int id;

    public Container(int slots, int id) {
        this.slots = new ItemStack[slots];
        this.id = id;
    }

    public void setSlot(int slot, ItemStack item) {
        slots[slot] = item;
    }

    public ItemStack getSlot(int slot) {
        return slots[slot];
    }

    public ItemStack[] getSlots(int start, int end) {
        ItemStack[] items = new ItemStack[end - start + 1];

        if (end + 1 - start >= 0) {
            System.arraycopy(slots, start, items, 0, end + 1 - start);
        }

        return items;
    }
}
