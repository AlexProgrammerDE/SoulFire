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

import com.soulfiremc.server.data.EquipmentSlot;
import com.soulfiremc.server.data.MenuType;
import com.soulfiremc.server.protocol.bot.state.entity.Player;
import lombok.Getter;

import java.util.Optional;
import java.util.function.Predicate;

@Getter
public class PlayerInventoryMenu extends ViewableContainer {
  private static final int HOTBAR_START = 36;
  private final ContainerSlot craftingResult = getSlot(0);
  private final ContainerSlot[] craftingGrid = getSlots(1, 4);
  private final ContainerSlot[] mainInventory = getSlots(9, 35);
  private final ContainerSlot[] hotbar = getSlots(36, 44);
  /**
   * Retrieves the storage slots of the container. This includes the main inventory and the hotbar.
   */
  private final ContainerSlot[] storage = getSlots(9, 44);
  private final ContainerSlot[] armor = getSlots(5, 8);
  private final ContainerSlot offhand = getSlot(45);
  private final PlayerInventory playerInventory;

  public PlayerInventoryMenu(Player player, PlayerInventory playerInventory) {
    super(player, MenuType.SOULFIRE_INVENTORY_MENU.slots(), 0);
    this.playerInventory = playerInventory;
    for (var slotEntry : MenuType.SOULFIRE_INVENTORY_MENU.playerInventory().entrySet()) {
      this.getSlot(slotEntry.getKey()).setStorageFrom(player.inventory().getSlot(slotEntry.getValue()));
    }
    for (var slotEntry : MenuType.SOULFIRE_INVENTORY_MENU.maxStackSize().entrySet()) {
      this.getSlot(slotEntry.getKey()).setContainerMaxStackSize(slotEntry.getValue());
    }
  }

  public ContainerSlot getEquipmentSlot(EquipmentSlot slot) {
    return switch (slot) {
      case MAINHAND -> getSelectedSlot();
      case OFFHAND -> offhand;
      case FEET -> armor[3];
      case LEGS -> armor[2];
      case CHEST -> armor[1];
      case HEAD -> armor[0];
      case BODY -> throw new IllegalArgumentException("Cannot get body slot on player");
    };
  }

  public static boolean isHotbarSlot(ContainerSlot slot) {
    return slot.slot() >= 36 && slot.slot() < 45;
  }

  public static int toHotbarIndex(ContainerSlot slot) {
    return slot.slot() - HOTBAR_START;
  }

  public static boolean isMainInventory(ContainerSlot slot) {
    return slot.slot() >= 9 && slot.slot() < 36;
  }

  public ContainerSlot getSelectedSlot() {
    return getSlot(HOTBAR_START + playerInventory.selected);
  }

  public boolean isHeldItem(ContainerSlot slot) {
    return slot == getSelectedSlot();
  }

  @Override
  public Optional<ContainerSlot> findMatchingSlotForAction(Predicate<ContainerSlot> predicate) {
    var heldItem = getSelectedSlot();
    if (predicate.test(heldItem)) {
      return Optional.of(heldItem);
    }

    for (var hotbarSlot : hotbar()) {
      if (hotbarSlot == heldItem) {
        continue;
      }

      if (predicate.test(hotbarSlot)) {
        return Optional.of(hotbarSlot);
      }
    }

    for (var slot : mainInventory()) {
      if (predicate.test(slot)) {
        return Optional.of(slot);
      }
    }

    return Optional.empty();
  }
}
