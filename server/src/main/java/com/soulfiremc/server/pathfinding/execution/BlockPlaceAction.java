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

import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.soulfiremc.server.data.BlockItems;
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.protocol.bot.BotActionManager;
import com.soulfiremc.server.protocol.bot.container.SFItemStack;
import com.soulfiremc.server.util.BlockTypeHelper;
import com.soulfiremc.server.util.ItemTypeHelper;
import com.soulfiremc.server.util.TimeUtil;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public final class BlockPlaceAction implements WorldAction {
  private final SFVec3i blockPosition;
  private final BotActionManager.BlockPlaceAgainstData blockPlaceAgainstData;
  private boolean putOnHotbar = false;
  private boolean finishedPlacing = false;

  @Override
  public boolean isCompleted(BotConnection connection) {
    var level = connection.sessionDataManager().currentLevel();

    return BlockTypeHelper.isFullBlock(level.getBlockStateAt(blockPosition));
  }

  @Override
  public void tick(BotConnection connection) {
    var sessionDataManager = connection.sessionDataManager();
    sessionDataManager.controlState().resetAll();

    if (!putOnHotbar) {
      var inventoryManager = sessionDataManager.inventoryManager();
      var playerInventory = inventoryManager.playerInventory();

      SFItemStack leastHardItem = null;
      var leastDestroyTime = 0F;
      for (var slot : playerInventory.storage()) {
        if (slot.item() == null) {
          continue;
        }

        var item = slot.item();
        var blockType = BlockItems.getBlockType(item.type());
        if (blockType.isEmpty()) {
          continue;
        }

        var destroyTime = blockType.get().destroyTime();
        if (leastHardItem == null || destroyTime < leastDestroyTime) {
          leastHardItem = item;
          leastDestroyTime = destroyTime;
        }
      }

      var heldSlot = playerInventory.getHeldItem();
      if (heldSlot.item() != null) {
        var item = heldSlot.item();
        if (ItemTypeHelper.isSafeFullBlockItem(item.type())) {
          putOnHotbar = true;
          return;
        }
      }

      for (var hotbarSlot : playerInventory.hotbar()) {
        if (hotbarSlot.item() == null) {
          continue;
        }

        var item = hotbarSlot.item();
        if (!ItemTypeHelper.isSafeFullBlockItem(item.type())) {
          continue;
        }

        inventoryManager.heldItemSlot(playerInventory.toHotbarIndex(hotbarSlot));
        inventoryManager.sendHeldItemChange();
        putOnHotbar = true;
        return;
      }

      for (var slot : playerInventory.mainInventory()) {
        if (slot.item() == null) {
          continue;
        }

        var item = slot.item();
        if (!ItemTypeHelper.isSafeFullBlockItem(item.type())) {
          continue;
        }

        if (!inventoryManager.tryInventoryControl()) {
          return;
        }

        try {
          inventoryManager.leftClickSlot(slot.slot());
          TimeUtil.waitTime(50, TimeUnit.MILLISECONDS);
          inventoryManager.leftClickSlot(playerInventory.getHeldItem().slot());
          TimeUtil.waitTime(50, TimeUnit.MILLISECONDS);

          if (inventoryManager.cursorItem() != null) {
            inventoryManager.leftClickSlot(slot.slot());
            TimeUtil.waitTime(50, TimeUnit.MILLISECONDS);
          }
        } finally {
          inventoryManager.unlockInventoryControl();
        }

        putOnHotbar = true;
        return;
      }

      throw new IllegalStateException("Failed to find item stack");
    }

    if (finishedPlacing) {
      return;
    }

    connection.sessionDataManager().botActionManager().placeBlock(Hand.MAIN_HAND, blockPlaceAgainstData);
    finishedPlacing = true;
  }

  @Override
  public int getAllowedTicks() {
    // 3-seconds max to place a block
    return 3 * 20;
  }

  @Override
  public String toString() {
    return "BlockPlaceAction -> " + blockPosition.formatXYZ();
  }
}
