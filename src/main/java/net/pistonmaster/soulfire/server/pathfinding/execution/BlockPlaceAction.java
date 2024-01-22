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
package net.pistonmaster.soulfire.server.pathfinding.execution;

import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.soulfire.server.data.BlockItems;
import net.pistonmaster.soulfire.server.pathfinding.SWVec3i;
import net.pistonmaster.soulfire.server.protocol.BotConnection;
import net.pistonmaster.soulfire.server.protocol.bot.BotActionManager;
import net.pistonmaster.soulfire.server.protocol.bot.container.SWItemStack;
import net.pistonmaster.soulfire.server.util.BlockTypeHelper;
import net.pistonmaster.soulfire.server.util.ItemTypeHelper;
import net.pistonmaster.soulfire.server.util.TimeUtil;

import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public final class BlockPlaceAction implements WorldAction {
    private final SWVec3i blockPosition;
    private final BotActionManager.BlockPlaceData blockPlaceData;
    private boolean putOnHotbar = false;
    private boolean finishedPlacing = false;

    @Override
    public boolean isCompleted(BotConnection connection) {
        var levelState = connection.sessionDataManager().getCurrentLevel();
        if (levelState == null) {
            return false;
        }

        return BlockTypeHelper.isFullBlock(levelState.getBlockStateAt(blockPosition));
    }

    @Override
    public void tick(BotConnection connection) {
        var sessionDataManager = connection.sessionDataManager();
        sessionDataManager.controlState().resetAll();

        if (!putOnHotbar) {
            var inventoryManager = sessionDataManager.inventoryManager();
            var playerInventory = inventoryManager.getPlayerInventory();

            SWItemStack leastHardItem = null;
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

            var heldSlot = playerInventory.hotbarSlot(inventoryManager.heldItemSlot());
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
                    inventoryManager.leftClickSlot(playerInventory.hotbarSlot(inventoryManager.heldItemSlot()).slot());
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

        connection.sessionDataManager().botActionManager().placeBlock(Hand.MAIN_HAND, blockPlaceData);
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
