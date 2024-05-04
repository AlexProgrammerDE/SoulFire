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
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType;

@Getter
public class WindowContainer extends Container {
  /**
   * The slots a normal player inventory on the bottom of a window has. 27 slots for the main
   * inventory and 9 slots for the hotbar.
   */
  private static final int INVENTORY_SIZE = 27 + 9;

  private final ContainerType containerType;
  private final Component title;

  public WindowContainer(ContainerType containerType, Component title, int id) {
    super(
      switch (containerType) {
        case GENERIC_9X1, GENERIC_3X3 -> 9 + INVENTORY_SIZE;
        case GENERIC_9X2 -> 18 + INVENTORY_SIZE;
        case GENERIC_9X3, SHULKER_BOX -> 27 + INVENTORY_SIZE;
        case GENERIC_9X4 -> 36 + INVENTORY_SIZE;
        case GENERIC_9X5 -> 45 + INVENTORY_SIZE;
        case GENERIC_9X6 -> 54 + INVENTORY_SIZE;
        case CRAFTER_3x3, CRAFTING -> 10 + INVENTORY_SIZE;
        case ANVIL, CARTOGRAPHY, FURNACE, BLAST_FURNACE, GRINDSTONE, MERCHANT, SMOKER -> 3 + INVENTORY_SIZE;
        case BEACON -> 1 + INVENTORY_SIZE;
        case BREWING_STAND, HOPPER -> 5 + INVENTORY_SIZE;
        case ENCHANTMENT, STONECUTTER -> 2 + INVENTORY_SIZE;
        case LECTERN -> 1; // Only one without a bottom inventory
        case LOOM, SMITHING -> 4 + INVENTORY_SIZE;
      },
      id);
    this.containerType = containerType;
    this.title = title;
  }
}
