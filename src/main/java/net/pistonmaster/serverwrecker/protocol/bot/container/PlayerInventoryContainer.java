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
package net.pistonmaster.serverwrecker.protocol.bot.container;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import net.pistonmaster.serverwrecker.protocol.BotConnection;

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
