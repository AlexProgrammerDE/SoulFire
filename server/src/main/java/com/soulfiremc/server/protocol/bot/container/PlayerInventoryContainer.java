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
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Predicate;

@Getter
public class PlayerInventoryContainer extends Container {
  private final ContainerSlot[] mainInventory = getSlots(9, 35);
  private final ContainerSlot[] hotbar = getSlots(36, 44);

  /**
   * Retrieves the storage slots of the container. This includes the main inventory and the hotbar.
   */
  private final ContainerSlot[] storage = getSlots(9, 44);

  @Getter
  private final ContainerSlot[] armor = getSlots(5, 8);
  @Getter
  private final ContainerSlot[] craftingGrid = getSlots(1, 4);
  public int selected;

  public PlayerInventoryContainer() {
    super(46, 0);
  }

  public Optional<SFItemStack> getEquipmentSlotItem(EquipmentSlot slot) {
    return getEquipmentSlot(slot).map(ContainerSlot::item);
  }

  public Optional<ContainerSlot> getEquipmentSlot(EquipmentSlot slot) {
    return switch (slot) {
      case MAINHAND -> Optional.of(getHeldItem());
      case OFFHAND -> Optional.of(getOffhand());
      case HEAD -> Optional.of(getHelmet());
      case CHEST -> Optional.of(getChestplate());
      case LEGS -> Optional.of(getLeggings());
      case FEET -> Optional.of(getBoots());
      case BODY -> Optional.empty();
    };
  }

  public void setEquipmentSlotItem(EquipmentSlot slot, @Nullable SFItemStack item) {
    getEquipmentSlot(slot).ifPresent(containerSlot -> containerSlot.setItem(item));
  }

  public ContainerSlot getHeldItem() {
    return hotbarSlot(selected);
  }

  public boolean isHeldItem(ContainerSlot slot) {
    return slot == getHeldItem();
  }

  public ContainerSlot hotbarSlot(int slot) {
    return getSlot(36 + slot);
  }

  public ContainerSlot getOffhand() {
    return getSlot(45);
  }

  public ContainerSlot getHelmet() {
    return getSlot(5);
  }

  public ContainerSlot getChestplate() {
    return getSlot(6);
  }

  public ContainerSlot getLeggings() {
    return getSlot(7);
  }

  public ContainerSlot getBoots() {
    return getSlot(8);
  }

  public ContainerSlot getCraftingResult() {
    return getSlot(0);
  }

  public boolean isHotbar(ContainerSlot slot) {
    return isHotbar(slot.slot());
  }

  public boolean isHotbar(int slot) {
    return slot >= 36 && slot <= 44;
  }

  public int toHotbarIndex(ContainerSlot slot) {
    return toHotbarIndex(slot.slot());
  }

  public int toHotbarIndex(int slot) {
    return slot - 36;
  }

  public boolean isMainInventory(ContainerSlot slot) {
    return isMainInventory(slot.slot());
  }

  public boolean isMainInventory(int slot) {
    return slot >= 9 && slot <= 35;
  }

  public Optional<ContainerSlot> findMatchingSlotForAction(Predicate<ContainerSlot> predicate) {
    var heldItem = getHeldItem();
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
