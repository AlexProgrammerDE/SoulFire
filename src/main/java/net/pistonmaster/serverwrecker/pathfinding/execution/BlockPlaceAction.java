/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.pathfinding.execution;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.pistonmaster.serverwrecker.data.ItemType;
import net.pistonmaster.serverwrecker.protocol.BotConnection;
import net.pistonmaster.serverwrecker.protocol.bot.BotMovementManager;
import net.pistonmaster.serverwrecker.protocol.bot.container.ContainerSlot;
import net.pistonmaster.serverwrecker.protocol.bot.container.InventoryManager;
import net.pistonmaster.serverwrecker.protocol.bot.container.PlayerInventoryContainer;
import net.pistonmaster.serverwrecker.protocol.bot.container.SWItemStack;
import net.pistonmaster.serverwrecker.protocol.bot.state.LevelState;
import net.pistonmaster.serverwrecker.util.TimeUtil;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.concurrent.TimeUnit;

@ToString
@RequiredArgsConstructor
public class BlockPlaceAction implements WorldAction {
    private final Vector3i blockPosition;
    private final ItemType blockType;
    private boolean putOnHotbar = false;

    @Override
    public boolean isCompleted(BotConnection connection) {
        var levelState = connection.sessionDataManager().getCurrentLevel();
        if (levelState == null) {
            return false;
        }

        return levelState.getBlockTypeAt(blockPosition).map(type -> type.name().equals(blockType.name())).orElse(false);
    }

    @Override
    public void tick(BotConnection connection) {
        var movementManager = connection.sessionDataManager().getBotMovementManager();
        movementManager.getControlState().resetAll();

        if (!putOnHotbar) {
            var inventoryManager = connection.sessionDataManager().getInventoryManager();
            var playerInventory = inventoryManager.getPlayerInventory();
            var heldSlot = playerInventory.getHotbarSlot(inventoryManager.getHeldItemSlot());
            if (heldSlot.item() != null) {
                var item = heldSlot.item();
                if (item.getType() == blockType) {
                    putOnHotbar = true;
                    return;
                }
            }

            for (var hotbarSlot : playerInventory.getHotbar()) {
                if (hotbarSlot.item() == null) {
                    continue;
                }

                var item = hotbarSlot.item();
                if (item.getType() == blockType) {
                    inventoryManager.setHeldItemSlot(playerInventory.toHotbarIndex(hotbarSlot));
                    inventoryManager.sendHeldItemChange();
                    putOnHotbar = true;
                    return;
                }
            }

            for (var slot : playerInventory.getMainInventory()) {
                if (slot.item() == null) {
                    continue;
                }

                var item = slot.item();
                if (item.getType() == blockType) {
                    if (!inventoryManager.tryInventoryControl()) {
                        return;
                    }

                    try {
                        inventoryManager.leftClickSlot(slot.slot());
                        TimeUtil.waitTime(50, TimeUnit.MILLISECONDS);
                        inventoryManager.leftClickSlot(playerInventory.getHotbarSlot(inventoryManager.getHeldItemSlot()).slot());
                        TimeUtil.waitTime(50, TimeUnit.MILLISECONDS);

                        if (inventoryManager.getCursorItem() != null) {
                            inventoryManager.leftClickSlot(slot.slot());
                            TimeUtil.waitTime(50, TimeUnit.MILLISECONDS);
                        }
                    } finally {
                        inventoryManager.unlockInventoryControl();
                    }

                    putOnHotbar = true;
                    return;
                }
            }
        }

        // TODO: Place block
    }

    @Override
    public int getAllowedTicks() {
        // 3-seconds max to break a block
        return 3 * 20;
    }
}
