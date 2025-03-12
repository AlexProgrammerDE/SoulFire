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

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public abstract class Container {
  @Getter
  private final @Nonnull ContainerSlot[] slots;
  private Int2IntMap properties;
  private SFItemStack carried = SFItemStack.EMPTY;
  private int stateId;

  public Container(int slots) {
    this.slots = new ContainerSlot[slots];
    for (var i = 0; i < slots; i++) {
      this.slots[i] = new ContainerSlot(i, new SlotStorage(SFItemStack.EMPTY));
    }
  }

  public void setSlot(int slot, @NonNull SFItemStack item) {
    slots[slot].setItem(item);
  }

  public @Nonnull ContainerSlot getSlot(int slot) {
    return slots[slot];
  }

  public ContainerSlot[] getSlots(int start, int end) {
    var length = end - start + 1;
    var items = new ContainerSlot[length];

    if (end + 1 - start >= 0) {
      System.arraycopy(slots, start, items, 0, length);
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

  public IntSet propertyKeys() {
    return properties == null ? IntSet.of() : properties.keySet();
  }

  public void setItem(int slotId, int stateId, SFItemStack stack) {
    this.getSlot(slotId).setItem(stack);
    this.stateId = stateId;
  }

  public void initializeContents(int stateId, List<SFItemStack> items, SFItemStack carried) {
    for (var i = 0; i < items.size(); i++) {
      this.getSlot(i).setItem(items.get(i));
    }

    this.carried = carried;
    this.stateId = stateId;
  }

  public SFItemStack getCarried() {
    return this.carried;
  }

  public void setCarried(SFItemStack stack) {
    this.carried = stack;
  }

  public int getStateId() {
    return this.stateId;
  }

  public Optional<ContainerSlot> findMatchingSlotForAction(Predicate<ContainerSlot> predicate) {
    return Arrays.stream(slots).filter(predicate).findFirst();
  }
}
