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

import com.soulfiremc.server.bot.BotConnection;
import com.soulfiremc.server.bot.ControlPriority;
import com.soulfiremc.server.bot.ControlStopReason;
import com.soulfiremc.test.utils.TestBootstrap;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

  @ParameterizedTest
  @CsvSource({"true, true", "true, false", "false, true", "false, false"})
  void handoffChecksConsumptionBeforeTheNextTaskTick(boolean consumed, boolean suspended) throws Exception {
    var bot = mock(BotConnection.class);
    var minecraft = mock(Minecraft.class);
    var player = mock(LocalPlayer.class);
    var gameMode = mock(MultiPlayerGameMode.class);
    var options = mock(Options.class);
    var inventory = mock(Inventory.class);
    var menu = mock(InventoryMenu.class);
    var slot = mock(Slot.class);
    var food = mock(FoodData.class);
    var held = stack(Items.DRIED_KELP, 3);
    minecraft.player = player;
    minecraft.gameMode = gameMode;
    Minecraft.class.getField("options").set(minecraft, options);
    var keyUse = Options.class.getField("keyUse");
    keyUse.setAccessible(true);
    keyUse.set(options, mock(KeyMapping.class));
    var inventoryMenu = LocalPlayer.class.getField("inventoryMenu");
    inventoryMenu.setAccessible(true);
    inventoryMenu.set(player, menu);
    when(bot.minecraft()).thenReturn(minecraft);
    when(player.getInventory()).thenReturn(inventory);
    when(inventory.getSelectedSlot()).thenReturn(0);
    when(menu.getSlot(36)).thenReturn(slot);
    when(slot.getItem()).thenReturn(held);
    when(player.getItemInHand(InteractionHand.MAIN_HAND)).thenReturn(held);
    when(player.getFoodData()).thenReturn(food);
    when(food.getFoodLevel()).thenReturn(10);
    when(gameMode.useItem(player, InteractionHand.MAIN_HAND)).thenReturn(InteractionResult.SUCCESS);

    var task = new EatMealTask(bot, 36, true, ControlPriority.HIGH);
    task.tick();
    assertFalse(task.consumed());
    if (consumed) held.setCount(2);
    if (suspended) task.onSuspended();
    else task.onStopped(ControlStopReason.REPLACED, null);

    assertEquals(consumed, task.consumed());
  }

  private static ItemStack stack(Item item, int count) {
    return new ItemStack(Holder.direct(item, DataComponentMap.EMPTY), count);
  }
}
