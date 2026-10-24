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
package com.soulfiremc.server.bot;

import com.soulfiremc.test.utils.TestBootstrap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MenuCloseStateTest {
  @BeforeAll
  static void bootstrapMinecraft() {
    TestBootstrap.bootstrapForTest();
  }

  @Test
  void waitsForServerConfirmationAndAggregatesMatchingReturnedStacks() {
    var state = new MenuCloseState(7,
      List.of(stack(Items.COBBLESTONE, 4), stack(Items.COBBLESTONE, 2)), List.of(),
      List.of(stack(Items.COBBLESTONE, 3), stack(Items.COBBLESTONE, 1), stack(Items.COBBLESTONE, 3)));
    assertFalse(state.completion().isDone());
    assertFalse(state.hasReturnedItems(7, List.of(stack(Items.COBBLESTONE, 13)), List.of()));
    assertFalse(state.hasReturnedItems(8, List.of(stack(Items.COBBLESTONE, 12)), List.of()));
    assertTrue(state.hasReturnedItems(8, List.of(stack(Items.COBBLESTONE, 11), stack(Items.COBBLESTONE, 2)), List.of()));
  }

  @Test
  void preservesComponentsWithoutRetainingMutableMenuStacks() {
    var returning = stack(Items.IRON_SWORD, 1);
    returning.set(DataComponents.DAMAGE, 19);
    var state = new MenuCloseState(4, List.of(stack(Items.IRON_SWORD, 1)), List.of(), List.of(returning));
    returning.set(DataComponents.DAMAGE, 0);
    assertFalse(state.hasReturnedItems(5, List.of(stack(Items.IRON_SWORD, 2)), List.of()));
    var returned = stack(Items.IRON_SWORD, 1);
    returned.set(DataComponents.DAMAGE, 19);
    assertTrue(state.hasReturnedItems(5, List.of(stack(Items.IRON_SWORD, 1), returned), List.of()));
  }

  @Test
  void doesNotWaitWhenThereAreNoPlayerItemsToReturn() {
    var state = new MenuCloseState(0, List.of(stack(Items.COBBLESTONE, 64)), List.of(), List.of(ItemStack.EMPTY));
    assertTrue(state.completion().isDone());
  }

  @Test
  void countsNewServerDropsWithoutConfusingExistingGroundItemsForReturnedItems() {
    var state = new MenuCloseState(7, List.of(stack(Items.DIAMOND, 63)),
      List.of(stack(Items.DIAMOND, 2)), List.of(stack(Items.DIAMOND, 3)));
    assertFalse(state.hasReturnedItems(8, List.of(stack(Items.DIAMOND, 64)), List.of(stack(Items.DIAMOND, 2))));
    assertFalse(state.hasReturnedItems(7, List.of(stack(Items.DIAMOND, 66)), List.of(stack(Items.DIAMOND, 2))));
    assertTrue(state.hasReturnedItems(7, List.of(stack(Items.DIAMOND, 64)), List.of(stack(Items.DIAMOND, 4))));
    assertTrue(state.hasReturnedItems(8, List.of(stack(Items.DIAMOND, 68)), List.of()));
  }

  private static ItemStack stack(Item item, int count) {
    return new ItemStack(Holder.direct(item, DataComponentMap.EMPTY), count);
  }
}
