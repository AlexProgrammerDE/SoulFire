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
package com.soulfiremc.server.pathfinding.execution;

import com.soulfiremc.server.pathfinding.Costs;
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.util.BlockItems;
import com.soulfiremc.server.util.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.util.OptionalInt;
import java.util.concurrent.TimeUnit;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.IntStream;

@Slf4j
public final class ItemPlaceHelper {
  private static final int HOTBAR_START = 36;

  private ItemPlaceHelper() {
  }

  public static OptionalInt findMatchingSlotForAction(Inventory inventory, InventoryMenu menu, Predicate<ItemStack> predicate) {
    var intPredicate = (IntPredicate) i -> predicate.test(menu.getSlot(i).getItem());
    int selectedIndex = HOTBAR_START + inventory.getSelectedSlot();

    // 1. Held item
    if (intPredicate.test(selectedIndex)) {
      return OptionalInt.of(selectedIndex);
    }

    // 2. Offhand
    if (intPredicate.test(45)) {
      return OptionalInt.of(45);
    }

    // 3. Remaining hotbar slots (36–44) except the selected slot
    var hotbarMatch = IntStream.range(HOTBAR_START, HOTBAR_START + 9)
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

  public static boolean placeBestBlockInHand(BotConnection connection) {
    var player = connection.minecraft().player;
    var playerInventory = player.inventoryMenu;

    Item leastHardItem = null;
    var leastDestroyTime = 0F;
    for (var slot : playerInventory.slots) {
      var slotItemStack = slot.getItem();
      if (slotItemStack.isEmpty()) {
        continue;
      }

      var slotItem = slotItemStack.getItem();
      var blockType = BlockItems.getBlock(slotItem);
      if (blockType.isEmpty()) {
        continue;
      }

      var destroyTime = blockType.get().defaultDestroyTime();
      if (leastHardItem == null || destroyTime < leastDestroyTime) {
        leastHardItem = slotItem;
        leastDestroyTime = destroyTime;
      }
    }

    if (leastHardItem == null) {
      throw new IllegalStateException("Failed to find item stack to place");
    }

    var finalLeastHardItem = leastHardItem;
    return placeInHand(connection.minecraft().gameMode, player,
      findMatchingSlotForAction(player.getInventory(), playerInventory,
        slot -> slot.getItem() == finalLeastHardItem)
        .orElseThrow(() -> new IllegalStateException("Failed to find item stack to use")));
  }

  public static boolean placeBestToolInHand(BotConnection connection, SFVec3i blockPosition) {
    var player = connection.minecraft().player;
    var playerInventory = player.inventoryMenu;
    var level = connection.minecraft().level;

    ItemStack bestItemStack = null;
    var bestCost = Integer.MAX_VALUE;
    var sawEmpty = false;
    for (var slot : playerInventory.slots) {
      var slotItem = slot.getItem();
      if (slotItem.isEmpty()) {
        if (sawEmpty) {
          continue;
        }

        sawEmpty = true;
      }

      var optionalBlock = level.getBlockState(blockPosition.toBlockPos());
      if (optionalBlock.getBlock() == Blocks.VOID_AIR) {
        throw new IllegalStateException("Block at %s is not loaded".formatted(blockPosition));
      }

      var cost =
        Costs.getRequiredMiningTicks(player, slotItem, optionalBlock)
          .ticks();

      if (cost < bestCost || (slotItem.isEmpty() && cost == bestCost)) {
        bestCost = cost;
        bestItemStack = slotItem;
      }
    }

    // Our hand is the best tool
    if (bestItemStack == null) {
      return true;
    }

    var finalBestItemStack = bestItemStack;
    return placeInHand(connection.minecraft().gameMode, player,
      findMatchingSlotForAction(player.getInventory(), playerInventory,
        slot -> ItemStack.isSameItemSameComponents(slot, finalBestItemStack))
        .orElseThrow(() -> new IllegalStateException("Failed to find item stack to use")));
  }

  public static int toHotbarIndex(int slot) {
    return slot - HOTBAR_START;
  }

  public static int getSelectedSlot(Inventory inventory) {
    return inventory.getSelectedSlot() + HOTBAR_START;
  }

  private static boolean placeInHand(MultiPlayerGameMode gameMode, LocalPlayer player, int slot) {
    if (player.hasContainerOpen()) {
      log.warn("Cannot place item in hand while looking at a foreign container");
      return false;
    }

    if (getSelectedSlot(player.getInventory()) == slot) {
      return true;
    } else if (InventoryMenu.isHotbarSlot(slot)) {
      player.getInventory().setSelectedSlot(toHotbarIndex(slot));
      return true;
    } else {
      player.sendOpenInventory();
      gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, slot, 0, ClickType.PICKUP, player);
      TimeUtil.waitTime(50, TimeUnit.MILLISECONDS);
      gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, getSelectedSlot(player.getInventory()), 0, ClickType.PICKUP, player);
      TimeUtil.waitTime(50, TimeUnit.MILLISECONDS);

      if (!player.inventoryMenu.getCarried().isEmpty()) {
        gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, slot, 0, ClickType.PICKUP, player);
        TimeUtil.waitTime(50, TimeUnit.MILLISECONDS);
      }

      player.closeContainer();
      return true;
    }
  }
}
