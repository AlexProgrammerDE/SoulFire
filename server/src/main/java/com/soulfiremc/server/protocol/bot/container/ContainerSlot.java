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

import lombok.AllArgsConstructor;

import javax.annotation.Nullable;
import java.util.Objects;

@AllArgsConstructor
public final class ContainerSlot {
  private final int slot;
  private SFItemStack item;

  public int slot() {
    return slot;
  }

  public SFItemStack item() {
    return item;
  }

  void setItem(@Nullable SFItemStack item) {
    this.item = item;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof ContainerSlot that)) {
      return false;
    }
    return this.slot == that.slot && Objects.equals(this.item, that.item);
  }

  @Override
  public int hashCode() {
    return Objects.hash(slot, item);
  }

  @Override
  public String toString() {
    return "ContainerSlot[slot=%d, item=%s]".formatted(slot, item);
  }
}
