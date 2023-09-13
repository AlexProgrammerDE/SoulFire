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

import com.github.steveice10.mc.protocol.data.game.entity.RotationOrigin;
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
import net.pistonmaster.serverwrecker.util.VectorHelper;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@ToString
@RequiredArgsConstructor
public class BlockBreakAction implements WorldAction {
    private final Vector3i blockPosition;
    private final ItemType toolType;
    private boolean didLook = false;
    private boolean putOnHotbar = false;
    private CompletableFuture<Void> breakFuture;

    @Override
    public boolean isCompleted(BotConnection connection) {
        LevelState levelState = connection.sessionDataManager().getCurrentLevel();
        if (levelState == null) {
            return false;
        }

        return levelState.getBlockTypeAt(blockPosition).map(blockType -> blockType.blockShapeTypes().isEmpty()).orElse(false);
    }

    @Override
    public void tick(BotConnection connection) {
        BotMovementManager movementManager = connection.sessionDataManager().getBotMovementManager();

        if (!didLook) {
            didLook = true;
            float previousYaw = movementManager.getYaw();
            float previousPitch = movementManager.getPitch();
            movementManager.lookAt(RotationOrigin.EYES, VectorHelper.middleOfBlockNormalize(blockPosition.toDouble()));
            if (previousPitch != movementManager.getPitch() || previousYaw != movementManager.getYaw()) {
                movementManager.sendRot();
            }
        }

        if (!putOnHotbar && toolType != null) {
            InventoryManager inventoryManager = connection.sessionDataManager().getInventoryManager();
            PlayerInventoryContainer playerInventory = inventoryManager.getPlayerInventory();
            ContainerSlot heldSlot = playerInventory.getHotbarSlot(inventoryManager.getHeldItemSlot());
            if (heldSlot.item() != null) {
                SWItemStack item = heldSlot.item();
                if (item.getType() == toolType) {
                    putOnHotbar = true;
                    return;
                }
            }

            for (ContainerSlot hotbarSlot : playerInventory.getHotbar()) {
                if (hotbarSlot.item() == null) {
                    continue;
                }

                SWItemStack item = hotbarSlot.item();
                if (item.getType() == toolType) {
                    inventoryManager.setHeldItemSlot(playerInventory.toHotbarIndex(hotbarSlot));
                    inventoryManager.sendHeldItemChange();
                    putOnHotbar = true;
                    return;
                }
            }

            for (ContainerSlot slot : playerInventory.getMainInventory()) {
                if (slot.item() == null) {
                    continue;
                }

                SWItemStack item = slot.item();
                if (item.getType() == toolType) {
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

        if (breakFuture == null || breakFuture.isCancelled() || breakFuture.isCompletedExceptionally()) {
            breakFuture = connection.sessionDataManager().getBotActionManager().breakBlock(blockPosition);
        }
    }

    @Override
    public int getAllowedTicks() {
        // 30-seconds max to break a block
        return 30 * 20;
    }
}
