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
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.util.concurrent.TimeUnit;

@Slf4j
public final class ItemPlaceHelper {
  private ItemPlaceHelper() {
  }

  public static boolean placeBestBlockInHand(BotConnection connection) {
    var player = connection.minecraft().player;
    var playerInventory = player.inventoryMenu;

    Item leastHardItem = null;
    var leastDestroyTime = 0F;
    for (var slot : playerInventory.storage()) {
      var slotItem = slot.item();
      if (slotItem.isEmpty()) {
        continue;
      }

      var slotItem = slotItem.type();
      var blockType = BlockItems.getBlock(slotItem);
      if (blockType.isEmpty()) {
        continue;
      }

      var destroyTime = blockType.get().destroyTime();
      if (leastHardItem == null || destroyTime < leastDestroyTime) {
        leastHardItem = slotItem;
        leastDestroyTime = destroyTime;
      }
    }

    if (leastHardItem == null) {
      throw new IllegalStateException("Failed to find item stack to place");
    }

    var finalLeastHardItem = leastHardItem;
    return placeInHand(player, playerInventory,
      playerInventory.findMatchingSlotForAction(
          slot -> slot.item().type() == finalLeastHardItem)
        .orElseThrow(() -> new IllegalStateException("Failed to find item stack to use")));
  }

  public static boolean placeBestToolInHand(LocalPlayer player, ClientLevel level, SFVec3i blockPosition) {
    var playerInventory = player.inventoryMenu;

    ItemStack bestItemStack = null;
    var bestCost = Integer.MAX_VALUE;
    var sawEmpty = false;
    for (var slot : playerInventory.storage()) {
      var slotItem = slot.item();
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
    return placeInHand(player, playerInventory,
      playerInventory.findMatchingSlotForAction(
          slot -> ItemStack.isSameItemSameComponents(slot.item(), finalBestItemStack))
        .orElseThrow(() -> new IllegalStateException("Failed to find item stack to use")));
  }

  private static boolean placeInHand(LocalPlayer player, InventoryMenu playerInventory,
                                     int slot) {
    if (player.hasContainerOpen()) {
      log.warn("Cannot place item in hand while looking at a foreign container");
      return false;
    }

    if (playerInventory.isHeldItem(slot)) {
      return true;
    } else if (InventoryMenu.isHotbarSlot(slot)) {
      player.getInventory().getSelectedSlot() = PlayerInventoryMenu.toHotbarIndex(slot);
      return true;
    } else {
      player.sendOpenInventory();
      player.inventoryMenu.leftClick(slot);
      TimeUtil.waitTime(50, TimeUnit.MILLISECONDS);
      player.inventoryMenu.leftClick(playerInventory.getSelectedSlot());
      TimeUtil.waitTime(50, TimeUnit.MILLISECONDS);

      if (!playerInventory.getCarried().isEmpty()) {
        player.inventoryMenu.leftClick(slot);
        TimeUtil.waitTime(50, TimeUnit.MILLISECONDS);
      }

      player.closeContainer();
      return true;
    }
  }
}
