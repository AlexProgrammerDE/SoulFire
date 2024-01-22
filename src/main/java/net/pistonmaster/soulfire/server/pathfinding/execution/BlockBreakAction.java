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

import com.github.steveice10.mc.protocol.data.game.entity.RotationOrigin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.soulfire.server.data.BlockType;
import net.pistonmaster.soulfire.server.pathfinding.Costs;
import net.pistonmaster.soulfire.server.pathfinding.SWVec3i;
import net.pistonmaster.soulfire.server.protocol.BotConnection;
import net.pistonmaster.soulfire.server.protocol.bot.container.SWItemStack;
import net.pistonmaster.soulfire.server.util.TimeUtil;
import net.pistonmaster.soulfire.server.util.VectorHelper;

import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public final class BlockBreakAction implements WorldAction {
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

        return levelState.getBlockStateAt(blockPosition).blockType().air();
    }

    @Override
    public void tick(BotConnection connection) {
        var sessionDataManager = connection.sessionDataManager();
        var clientEntity = sessionDataManager.clientEntity();
        sessionDataManager.controlState().resetAll();

        var levelState = sessionDataManager.getCurrentLevel();
        var inventoryManager = sessionDataManager.inventoryManager();
        var playerInventory = inventoryManager.getPlayerInventory();

        if (levelState == null) {
            return;
        }

        if (!didLook) {
            didLook = true;
            var previousYaw = clientEntity.yaw();
            var previousPitch = clientEntity.pitch();
            clientEntity.lookAt(
                    RotationOrigin.EYES,
                    VectorHelper.topMiddleOfBlock(
                            blockPosition.toVector3d(),
                            levelState.getBlockStateAt(blockPosition)
                    )
            );
            if (previousPitch != clientEntity.pitch() || previousYaw != clientEntity.yaw()) {
                clientEntity.sendRot();
            }
        }

        if (!calculatedBestItemStack) {
            SWItemStack itemStack = null;
            var bestCost = Integer.MAX_VALUE;
            var sawEmpty = false;
            for (var slot : playerInventory.storage()) {
                var item = slot.item();
                if (item == null) {
                    if (sawEmpty) {
                        continue;
                    }

                    sawEmpty = true;
                }

                var optionalBlockType = levelState.getBlockStateAt(blockPosition).blockType();
                if (optionalBlockType == BlockType.VOID_AIR) {
                    log.warn("Block at {} is not in view range!", blockPosition);
                    return;
                }

                var cost = Costs.getRequiredMiningTicks(
                        sessionDataManager.tagsState(),
                        sessionDataManager.clientEntity().effectState(),
                        clientEntity.onGround(),
                        item,
                        optionalBlockType
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
            var heldSlot = playerInventory.hotbarSlot(inventoryManager.heldItemSlot());
            if (heldSlot.item() != null) {
                var item = heldSlot.item();
                if (item.equalsShape(bestItemStack)) {
                    putOnHotbar = true;
                    return;
                }
            }

            for (var hotbarSlot : playerInventory.hotbar()) {
                if (hotbarSlot.item() == null) {
                    continue;
                }

                var item = hotbarSlot.item();
                if (!item.equalsShape(bestItemStack)) {
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
                if (!item.equalsShape(bestItemStack)) {
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

        if (finishedDigging) {
            return;
        }

        if (remainingTicks == -1) {
            var optionalBlockType = levelState.getBlockStateAt(blockPosition).blockType();
            if (optionalBlockType == BlockType.VOID_AIR) {
                log.warn("Block at {} is not in view range!", blockPosition);
                return;
            }

            remainingTicks = Costs.getRequiredMiningTicks(
                    sessionDataManager.tagsState(),
                    sessionDataManager.clientEntity().effectState(),
                    clientEntity.onGround(),
                    sessionDataManager.inventoryManager().getPlayerInventory()
                            .hotbarSlot(sessionDataManager.inventoryManager().heldItemSlot())
                            .item(),
                    optionalBlockType
            ).ticks();
            sessionDataManager.botActionManager().sendStartBreakBlock(blockPosition.toVector3i());
        } else if (--remainingTicks == 0) {
            sessionDataManager.botActionManager().sendEndBreakBlock(blockPosition.toVector3i());
            finishedDigging = true;
        } else {
            sessionDataManager.botActionManager().sendBreakBlockAnimation();
        }
    }

    @Override
    public int getAllowedTicks() {
        // 20-seconds max to break a block
        return 20 * 20;
    }

    @Override
    public String toString() {
        return "BlockBreakAction -> " + blockPosition.formatXYZ();
    }
}
