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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
public final class ContainerSlot {
  private final int slot;
  @NonNull
  private SlotStorage storage;

  public ContainerSlot(int slot) {
    this.slot = slot;
    this.storage = new SlotStorage(SFItemStack.EMPTY);
  }

  public int slot() {
    return slot;
  }

  public @NonNull SFItemStack item() {
    return storage.item;
  }

  public void setItem(@NonNull SFItemStack item) {
    this.storage.item = item;
  }

  public int getMaxStackSize() {
    return Math.min(storage.item.getMaxStackSize(), getContainerMaxStackSize());
  }

  public int getContainerMaxStackSize() {
    return storage.containerMaxStackSize;
  }

  public void setContainerMaxStackSize(int containerMaxStackSize) {
    this.storage.containerMaxStackSize = containerMaxStackSize;
  }

  void setStorageFrom(@NonNull ContainerSlot slot) {
    this.storage = slot.storage;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof ContainerSlot that)) {
      return false;
    }
    return this.slot == that.slot && Objects.equals(this.storage, that.storage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(slot, storage);
  }

  @Override
  public String toString() {
    return "ContainerSlot[slot=%d, storage=%s]".formatted(slot, storage);
  }

  @AllArgsConstructor(access = AccessLevel.PACKAGE)
  private static class SlotStorage {
    @NonNull
    private SFItemStack item;
    private int containerMaxStackSize = 99;

    SlotStorage(SFItemStack item) {
      this.item = item;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof SlotStorage that)) {
        return false;
      }
      return Objects.equals(this.item, that.item);
    }

    @Override
    public int hashCode() {
      return Objects.hash(item);
    }

    @Override
    public String toString() {
      return "SlotStorage[item=%s]".formatted(item);
    }
  }
}
