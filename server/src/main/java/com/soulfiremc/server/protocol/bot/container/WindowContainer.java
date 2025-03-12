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

import com.soulfiremc.server.data.MenuType;
import com.soulfiremc.server.protocol.bot.state.entity.Player;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerType;

@Getter
public final class WindowContainer extends ViewableContainer {
  private final ContainerType containerType;
  private final Component title;

  public WindowContainer(Player player, ContainerType containerType, Component title, int containerId) {
    super(
      player,
      MenuType.REGISTRY.getById(containerType.ordinal()).slots(),
      containerId);
    this.containerType = containerType;
    this.title = title;
    for (var slotEntry : MenuType.REGISTRY.getById(containerType.ordinal()).playerInventory().entrySet()) {
      this.getSlot(slotEntry.getKey()).setStorageFrom(player.inventory().getSlot(slotEntry.getValue()));
    }
  }
}
