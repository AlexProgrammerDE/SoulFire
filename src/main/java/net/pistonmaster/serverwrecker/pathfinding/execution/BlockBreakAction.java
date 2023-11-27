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
import net.pistonmaster.serverwrecker.pathfinding.Costs;
import net.pistonmaster.serverwrecker.protocol.BotConnection;
import net.pistonmaster.serverwrecker.protocol.bot.block.BlockStateMeta;
import net.pistonmaster.serverwrecker.protocol.bot.container.SWItemStack;
import net.pistonmaster.serverwrecker.util.TimeUtil;
import net.pistonmaster.serverwrecker.util.VectorHelper;
import net.pistonmaster.serverwrecker.pathfinding.SWVec3i;

import java.util.concurrent.TimeUnit;

@ToString
@RequiredArgsConstructor
public class BlockBreakAction implements WorldAction {
    private final SWVec3i blockPosition;
    boolean finishedDigging = false;
    private boolean didLook = false;
    private boolean putOnHotbar = false;
    private boolean calculatedBestItemStack = false;
    private SWItemStack bestItemStack = null;
    private int remainingTicks = -1;

    @Override
    public boolean isCompleted(BotConnection connection) {
        var levelState = connection.sessionDataManager().getCurrentLevel();
        if (levelState == null) {
            return false;
        }

        return levelState.getBlockStateAt(blockPosition)
                .map(blockState -> blockState.blockType().blockShapeTypes().isEmpty())
                .orElse(false);
    }

    @Override
    public void tick(BotConnection connection) {
        var sessionDataManager = connection.sessionDataManager();
        var movementManager = sessionDataManager.getBotMovementManager();
        var levelState = sessionDataManager.getCurrentLevel();
        var inventoryManager = sessionDataManager.getInventoryManager();
        var playerInventory = inventoryManager.getPlayerInventory();

        if (levelState == null) {
            return;
        }

        movementManager.getControlState().resetAll();

        if (!didLook) {
            didLook = true;
            var previousYaw = movementManager.getYaw();
            var previousPitch = movementManager.getPitch();
            movementManager.lookAt(RotationOrigin.EYES, VectorHelper.middleOfBlockNormalize(blockPosition.toVector3d()));
            if (previousPitch != movementManager.getPitch() || previousYaw != movementManager.getYaw()) {
                movementManager.sendRot();
            }
        }

        if (!calculatedBestItemStack) {
            SWItemStack itemStack = null;
            var bestCost = Integer.MAX_VALUE;
            var sawEmpty = false;
            for (var slot : playerInventory.getStorage()) {
                var item = slot.item();
                if (item == null) {
                    if (sawEmpty) {
                        continue;
                    }

                    sawEmpty = true;
                }

                var cost = Costs.getRequiredMiningTicks(
                        sessionDataManager.getTagsState(),
                        sessionDataManager.getSelfEffectState(),
                        sessionDataManager.getBotMovementManager().getEntity().isOnGround(),
                        item,
                        levelState.getBlockStateAt(blockPosition).map(BlockStateMeta::blockType).orElseThrow()
                ).ticks();

                if (cost < bestCost || (item == null && cost == bestCost)) {
                    bestCost = cost;
                    itemStack = item;
                }
            }

            bestItemStack = itemStack;
            calculatedBestItemStack = true;
        }

        if (!putOnHotbar && bestItemStack != null) {
            var heldSlot = playerInventory.getHotbarSlot(inventoryManager.getHeldItemSlot());
            if (heldSlot.item() != null) {
                var item = heldSlot.item();
                if (item.equalsShape(bestItemStack)) {
                    putOnHotbar = true;
                    return;
                }
            }

            for (var hotbarSlot : playerInventory.getHotbar()) {
                if (hotbarSlot.item() == null) {
                    continue;
                }

                var item = hotbarSlot.item();
                if (!item.equalsShape(bestItemStack)) {
                    continue;
                }

                inventoryManager.setHeldItemSlot(playerInventory.toHotbarIndex(hotbarSlot));
                inventoryManager.sendHeldItemChange();
                putOnHotbar = true;
                return;
            }

            for (var slot : playerInventory.getMainInventory()) {
                if (slot.item() == null) {
                    continue;
                }

                var item = slot.item();
                if (!item.equalsShape(bestItemStack)) {
                    continue;
                }

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

            throw new IllegalStateException("Failed to find item stack");
        }

        if (finishedDigging) {
            return;
        }

        if (remainingTicks == -1) {
            remainingTicks = Costs.getRequiredMiningTicks(
                    sessionDataManager.getTagsState(),
                    sessionDataManager.getSelfEffectState(),
                    sessionDataManager.getBotMovementManager().getEntity().isOnGround(),
                    sessionDataManager.getInventoryManager().getPlayerInventory()
                            .getHotbarSlot(sessionDataManager.getInventoryManager().getHeldItemSlot())
                            .item(),
                    levelState.getBlockStateAt(blockPosition).map(BlockStateMeta::blockType).orElseThrow()
            ).ticks();
            sessionDataManager.getBotActionManager().sendStartBreakBlock(blockPosition.toVector3i());
        } else if (--remainingTicks == 0) {
            sessionDataManager.getBotActionManager().sendEndBreakBlock(blockPosition.toVector3i());
            finishedDigging = true;
        }
    }

    @Override
    public int getAllowedTicks() {
        // 20-seconds max to break a block
        return 20 * 20;
    }
}
