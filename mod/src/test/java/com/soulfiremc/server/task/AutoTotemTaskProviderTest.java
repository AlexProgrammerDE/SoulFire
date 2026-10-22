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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AutoTotemTaskProviderTest {
  @BeforeAll
  static void bootstrapMinecraft() {
    TestBootstrap.bootstrapForTest();
  }

  @Test
  void countsAHeldTotemOrANewServerConfirmedUse() {
    var previous = new ItemStack(Holder.direct(Items.TOTEM_OF_UNDYING, DataComponentMap.EMPTY));
    var consumed = previous.copy();

    assertTrue(AutoTotemTaskProvider.confirmsTotemEquip(previous, null, null));
    assertTrue(AutoTotemTaskProvider.confirmsTotemEquip(ItemStack.EMPTY, previous, consumed));
  }

  @Test
  void doesNotCountAnOldActivationOrAnotherItem() {
    var previous = new ItemStack(Holder.direct(Items.TOTEM_OF_UNDYING, DataComponentMap.EMPTY));

    assertFalse(AutoTotemTaskProvider.confirmsTotemEquip(ItemStack.EMPTY, previous, previous));
    assertFalse(AutoTotemTaskProvider.confirmsTotemEquip(ItemStack.EMPTY, previous, null));
    var other = new ItemStack(Holder.direct(Items.DIAMOND, DataComponentMap.EMPTY));
    assertFalse(AutoTotemTaskProvider.confirmsTotemEquip(ItemStack.EMPTY, null, other));
  }
}
