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

import com.viaversion.viaversion.libs.fastutil.ints.Int2IntMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2IntOpenHashMap;
import lombok.Getter;

import javax.annotation.Nonnull;

@Getter
public class Container {
    private final @Nonnull ContainerSlot[] slots;
    private final int id;
    private final Int2IntMap properties = new Int2IntOpenHashMap();

    public Container(int slots, int id) {
        this.slots = new ContainerSlot[slots];
        for (int i = 0; i < slots; i++) {
            this.slots[i] = new ContainerSlot(i, null);
        }
        this.id = id;
    }

    public void setSlot(int slot, SWItemStack item) {
        slots[slot] = new ContainerSlot(slot, item);
    }

    public @Nonnull ContainerSlot getSlot(int slot) {
        return slots[slot];
    }

    public ContainerSlot[] getSlots(int start, int end) {
        ContainerSlot[] items = new ContainerSlot[end - start + 1];

        if (end + 1 - start >= 0) {
            System.arraycopy(slots, start, items, 0, end + 1 - start);
        }

        return items;
    }

    public void setProperty(int property, int value) {
        properties.put(property, value);
    }

    public int getProperty(int property) {
        return properties.getOrDefault(property, 0);
    }
}
