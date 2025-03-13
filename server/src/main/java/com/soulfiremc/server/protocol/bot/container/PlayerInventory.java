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

import lombok.Getter;

@Getter
public final class PlayerInventory extends Container {
  private final ContainerSlot[] mainInventory = getSlots(9, 35);
  private final ContainerSlot[] hotbar = getSlots(0, 8);
  /**
   * Retrieves the storage slots of the container. This includes the main inventory and the hotbar.
   */
  private final ContainerSlot[] storage = getSlots(0, 35);
  private final ContainerSlot[] armor = getSlots(36, 39);
  private final ContainerSlot offhand = getSlot(40);
  public int selected;

  public PlayerInventory() {
    super(41);
  }

  public static boolean isHotbarSlot(int index) {
    return index >= 0 && index < 9;
  }

  public SFItemStack getSelected() {
    return isHotbarSlot(this.selected) ? getSlot(this.selected).item() : SFItemStack.EMPTY;
  }

  public ContainerSlot getSelectedSlot() {
    return getSlot(selected);
  }
}
