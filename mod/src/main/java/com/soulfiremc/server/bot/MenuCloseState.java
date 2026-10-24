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

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/// Observes items returned by the server before a local disconnect closes transport.
final class MenuCloseState {
  private final CompletableFuture<Void> completion = new CompletableFuture<>();
  private final List<ReturnedStack> expected = new ArrayList<>();
  private final long inventoryUpdate;

  MenuCloseState(long inventoryUpdate, List<ItemStack> inventory, List<ItemStack> ground, List<ItemStack> returning) {
    this.inventoryUpdate = inventoryUpdate;
    returning.forEach(stack -> expect(inventory, ground, stack));
    if (expected.isEmpty()) {
      completion.complete(null);
    }
  }

  static MenuCloseState capture(LocalPlayer player, long inventoryUpdate) {
    var returning = new ArrayList<ItemStack>();
    var menu = player.containerMenu;
    returning.add(menu.getCarried());
    if (menu instanceof AbstractCraftingMenu crafting) {
      crafting.getInputGridSlots().forEach(slot -> returning.add(slot.getItem()));
    } else {
      var inputs = switch (menu) {
        case SmithingMenu _, LoomMenu _ -> 3;
        case AnvilMenu _, CartographyTableMenu _, EnchantmentMenu _, GrindstoneMenu _, MerchantMenu _ -> 2;
        case BeaconMenu _, StonecutterMenu _ -> 1;
        default -> 0;
      };
      for (var slot = 0; slot < inputs; slot++) {
        returning.add(menu.getSlot(slot).getItem());
      }
    }
    if (menu != player.inventoryMenu) {
      player.inventoryMenu.getInputGridSlots().forEach(slot -> returning.add(slot.getItem()));
    }
    return new MenuCloseState(inventoryUpdate, inventoryItems(player), groundItems(player), returning);
  }

  static MenuCloseState captureCreativeCursor(LocalPlayer player, long inventoryUpdate, ItemStack carried) {
    return new MenuCloseState(inventoryUpdate, inventoryItems(player), groundItems(player), List.of(carried));
  }

  CompletableFuture<Void> completion() {
    return completion;
  }

  void tick(Minecraft minecraft, long update) {
    if (completion.isDone()) {
      return;
    }
    var player = minecraft.player;
    var connection = minecraft.getConnection();
    if (player == null || connection == null || !connection.getConnection().isConnected()) {
      completion.complete(null);
      return;
    }
    if (hasReturnedItems(update, inventoryItems(player), groundItems(player))) {
      completion.complete(null);
    }
  }

  boolean hasReturnedItems(long update, List<ItemStack> inventory, List<ItemStack> ground) {
    var receivedReturn = update != inventoryUpdate || expected.stream()
      .anyMatch(stack -> inventoryCount(ground, stack.item()) > stack.groundCount());
    return expected.isEmpty() || receivedReturn && expected.stream().allMatch(stack ->
      inventoryCount(inventory, stack.item()) + inventoryCount(ground, stack.item()) >= stack.count());
  }

  private void expect(List<ItemStack> inventory, List<ItemStack> ground, ItemStack stack) {
    if (stack.isEmpty()) {
      return;
    }
    for (var index = 0; index < expected.size(); index++) {
      var existing = expected.get(index);
      if (ItemStack.isSameItemSameComponents(existing.item(), stack)) {
        expected.set(index, new ReturnedStack(existing.item(), existing.count() + stack.getCount(), existing.groundCount()));
        return;
      }
    }
    var groundCount = inventoryCount(ground, stack);
    expected.add(new ReturnedStack(stack.copyWithCount(1), inventoryCount(inventory, stack) + groundCount + stack.getCount(), groundCount));
  }

  private static List<ItemStack> groundItems(LocalPlayer player) {
    var items = new ArrayList<ItemStack>();
    for (var entity : ((ClientLevel) player.level()).entitiesForRendering()) {
      if (entity instanceof ItemEntity item) {
        items.add(item.getItem());
      }
    }
    return items;
  }

  private static List<ItemStack> inventoryItems(LocalPlayer player) {
    var items = new ArrayList<>(player.getInventory().getNonEquipmentItems());
    items.add(player.getOffhandItem());
    return items;
  }

  private static int inventoryCount(List<ItemStack> inventory, ItemStack item) {
    var count = 0;
    for (var stack : inventory) {
      if (ItemStack.isSameItemSameComponents(stack, item)) {
        count += stack.getCount();
      }
    }
    return count;
  }

  private record ReturnedStack(ItemStack item, int count, int groundCount) {
  }
}
