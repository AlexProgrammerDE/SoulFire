/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.protocol.bot.container;

import com.soulfiremc.server.data.ItemType;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.stream.Stream;

@Getter
public class Container {
  private final @Nonnull ContainerSlot[] slots;
  private final int id;
  private Int2IntMap properties;

  public Container(int slots, int id) {
    this.slots = new ContainerSlot[slots];
    for (var i = 0; i < slots; i++) {
      this.slots[i] = new ContainerSlot(i, null);
    }
    this.id = id;
  }

  public void setSlot(int slot, @Nullable SFItemStack item) {
    slots[slot].setItem(item);
  }

  public @Nonnull ContainerSlot getSlot(int slot) {
    return slots[slot];
  }

  public ContainerSlot[] getSlots(int start, int end) {
    var items = new ContainerSlot[end - start + 1];

    if (end + 1 - start >= 0) {
      System.arraycopy(slots, start, items, 0, end + 1 - start);
    }

    return items;
  }

  public void setProperty(int property, int value) {
    // Lazy init to save a little memory
    if (properties == null) {
      properties = new Int2IntOpenHashMap();
    }

    properties.put(property, value);
  }

  public int getProperty(int property) {
    if (properties == null) {
      return 0;
    }

    return properties.getOrDefault(property, 0);
  }

  public Stream<ContainerSlot> stream() {
    return Stream.of(slots);
  }

  public int countItems(ItemType itemType) {
    return stream()
      .filter(slot -> slot.item() != null && slot.item().type() == itemType)
      .mapToInt(slot -> slot.item().getAmount())
      .sum();
  }
}
