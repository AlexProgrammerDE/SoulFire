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
package com.soulfiremc.server.task;

import com.soulfiremc.test.utils.TestBootstrap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EatMealTaskTest {
  @BeforeAll
  static void bootstrapMinecraft() {
    TestBootstrap.bootstrapForTest();
  }

  @Test
  void noticesItemConsumptionEvenBeforeHungerIsUpdated() {
    assertTrue(EatMealTask.consumptionConfirmed(stack(Items.DRIED_KELP, 3), stack(Items.DRIED_KELP, 2), 10, 10));
    assertTrue(EatMealTask.consumptionConfirmed(stack(Items.BREAD, 1), ItemStack.EMPTY, 10, 10));
    assertTrue(EatMealTask.consumptionConfirmed(stack(Items.MUSHROOM_STEW, 1), stack(Items.BOWL, 1), 10, 10));
  }

  @Test
  void waitsForAChangedStackOrConfirmedNutrition() {
    assertFalse(EatMealTask.consumptionConfirmed(stack(Items.BREAD, 3), stack(Items.BREAD, 3), 10, 10));
    assertFalse(EatMealTask.consumptionConfirmed(stack(Items.BREAD, 3), stack(Items.BREAD, 4), 10, 9));
    assertTrue(EatMealTask.consumptionConfirmed(stack(Items.BREAD, 3), stack(Items.BREAD, 3), 10, 15));
  }

  private static ItemStack stack(Item item, int count) {
    return new ItemStack(Holder.direct(item, DataComponentMap.EMPTY), count);
  }
}
