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

import com.soulfiremc.server.data.BlockItems;
import com.soulfiremc.server.data.BlockType;
import com.soulfiremc.server.data.ItemType;
import com.soulfiremc.server.pathfinding.Costs;
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.protocol.bot.SessionDataManager;
import com.soulfiremc.server.protocol.bot.container.ContainerSlot;
import com.soulfiremc.server.protocol.bot.container.PlayerInventoryMenu;
import com.soulfiremc.server.protocol.bot.container.SFItemStack;
import com.soulfiremc.server.protocol.bot.state.entity.LocalPlayer;
import com.soulfiremc.server.util.TimeUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public final class ItemPlaceHelper {
  private ItemPlaceHelper() {
  }

  public static boolean placeBestBlockInHand(BotConnection connection) {
    var player = connection.dataManager().localPlayer();
    var playerInventory = player.inventoryMenu;

    ItemType leastHardItemType = null;
    var leastDestroyTime = 0F;
    for (var slot : playerInventory.storage()) {
      var slotItem = slot.item();
      if (slotItem.isEmpty()) {
        continue;
      }

      var slotItemType = slotItem.type();
      var blockType = BlockItems.getBlockType(slotItemType);
      if (blockType.isEmpty()) {
        continue;
      }

      var destroyTime = blockType.get().destroyTime();
      if (leastHardItemType == null || destroyTime < leastDestroyTime) {
        leastHardItemType = slotItemType;
        leastDestroyTime = destroyTime;
      }
    }

    if (leastHardItemType == null) {
      throw new IllegalStateException("Failed to find item stack to place");
    }

    var finalLeastHardItemType = leastHardItemType;
    return placeInHand(player, playerInventory,
      playerInventory.findMatchingSlotForAction(
          slot -> slot.item().type() == finalLeastHardItemType)
        .orElseThrow(() -> new IllegalStateException("Failed to find item stack to use")));
  }

  public static boolean placeBestToolInHand(SessionDataManager dataManager, SFVec3i blockPosition) {
    var player = dataManager.localPlayer();
    var playerInventory = player.inventoryMenu;

    SFItemStack bestItemStack = null;
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

      var optionalBlockType = dataManager.currentLevel().getBlockState(blockPosition).blockType();
      if (optionalBlockType == BlockType.VOID_AIR) {
        throw new IllegalStateException("Block at %s is not loaded".formatted(blockPosition));
      }

      var cost =
        Costs.getRequiredMiningTicks(
            dataManager.tagsState(),
            dataManager.localPlayer(),
            dataManager.localPlayer().onGround(),
            slotItem,
            optionalBlockType)
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
          slot -> SFItemStack.isSameItemSameComponents(slot.item(), finalBestItemStack))
        .orElseThrow(() -> new IllegalStateException("Failed to find item stack to use")));
  }

  private static boolean placeInHand(LocalPlayer player, PlayerInventoryMenu playerInventory,
                                     ContainerSlot slot) {
    if (player.hasContainerOpen()) {
      log.warn("Cannot place item in hand while looking at a foreign container");
      return false;
    }

    if (playerInventory.isHeldItem(slot)) {
      return true;
    } else if (PlayerInventoryMenu.isHotbarSlot(slot)) {
      player.inventory().selected = PlayerInventoryMenu.toHotbarIndex(slot);
      return true;
    } else if (PlayerInventoryMenu.isMainInventory(slot)) {
      player.openPlayerInventory();
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
    } else {
      throw new IllegalStateException("Unexpected container slot type");
    }
  }
}
