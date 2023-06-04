package net.pistonmaster.serverwrecker.util;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import net.pistonmaster.serverwrecker.data.ItemType;

public class ItemUtils {
    public static ItemType getTypeOfStack(ItemStack itemStack) {
        return ItemType.getById(itemStack.getId());
    }
}
