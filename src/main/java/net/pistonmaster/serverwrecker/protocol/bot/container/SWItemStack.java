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
import lombok.Getter;
import net.pistonmaster.serverwrecker.data.ItemType;

@Getter
public class SWItemStack extends ItemStack {
    private final ItemType type;

    private SWItemStack(ItemStack itemStack) {
        super(itemStack.getId(), itemStack.getAmount(), itemStack.getNbt());
        this.type = ItemType.getById(itemStack.getId());
    }

    public static SWItemStack from(ItemStack itemStack) {
        if (itemStack == null) {
            return null;
        }

        return new SWItemStack(itemStack);
    }
}
