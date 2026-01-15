/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.util;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;

import java.util.OptionalInt;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.IntStream;

@Slf4j
public final class SFInventoryHelpers {
  private SFInventoryHelpers() {
  }

  public static boolean isSelectableHotbarSlot(int slot) {
    return slot >= InventoryMenu.USE_ROW_SLOT_START && slot < InventoryMenu.USE_ROW_SLOT_END;
  }

  public static int toHotbarIndex(int slot) {
    return slot - InventoryMenu.USE_ROW_SLOT_START;
  }

  public static int getSelectedSlot(Inventory inventory) {
    return inventory.getSelectedSlot() + InventoryMenu.USE_ROW_SLOT_START;
  }

  public static OptionalInt findMatchingSlotForAction(Inventory inventory, InventoryMenu menu, Predicate<ItemStack> predicate) {
    var intPredicate = (IntPredicate) i -> predicate.test(menu.getSlot(i).getItem());
    int selectedIndex = InventoryMenu.USE_ROW_SLOT_START + inventory.getSelectedSlot();

    // 1. Held item
    if (intPredicate.test(selectedIndex)) {
      return OptionalInt.of(selectedIndex);
    }

    // 2. Offhand
    if (intPredicate.test(45)) {
      return OptionalInt.of(45);
    }

    // 3. Remaining hotbar slots (36–44) except the selected slot
    var hotbarMatch = IntStream.range(InventoryMenu.USE_ROW_SLOT_START, InventoryMenu.USE_ROW_SLOT_END)
      .filter(i -> i != selectedIndex)
      .filter(intPredicate)
      .findFirst();
    if (hotbarMatch.isPresent()) {
      return hotbarMatch;
    }

    // 4. Main inventory slots (9–35)
    var mainMatch = IntStream.range(9, 36)
      .filter(intPredicate)
      .findFirst();
    if (mainMatch.isPresent()) {
      return mainMatch;
    }

    // 5. Armor slots (5–8)
    return IntStream.range(5, 9)
      .filter(intPredicate)
      .findFirst();
  }
}
