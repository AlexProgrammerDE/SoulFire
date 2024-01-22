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
package net.pistonmaster.soulfire.server.protocol.bot.container;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.inventory.ClickItemAction;
import com.github.steveice10.mc.protocol.data.game.inventory.ContainerActionType;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClosePacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundSetCarriedItemPacket;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.pistonmaster.soulfire.server.protocol.bot.SessionDataManager;
import org.jetbrains.annotations.ApiStatus;

import java.util.concurrent.locks.ReentrantLock;

@Data
@RequiredArgsConstructor
public class InventoryManager {
    private final Int2ObjectMap<Container> containerData = new Int2ObjectOpenHashMap<>();
    private final ReentrantLock inventoryControlLock = new ReentrantLock();
    @ToString.Exclude
    private final SessionDataManager dataManager;
    private Container openContainer;
    private int heldItemSlot = 0;
    private int lastStateId = -1;
    private SWItemStack cursorItem;

    /**
     * The inventory has a control lock to prevent multiple threads from moving items at the same time.
     */
    public void lockInventoryControl() {
        inventoryControlLock.lock();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean tryInventoryControl() {
        return inventoryControlLock.tryLock();
    }

    public void unlockInventoryControl() {
        inventoryControlLock.unlock();
    }

    @ApiStatus.Internal
    public void initPlayerInventory() {
        containerData.put(0, new PlayerInventoryContainer());
    }

    public PlayerInventoryContainer getPlayerInventory() {
        return (PlayerInventoryContainer) containerData.get(0);
    }

    public Container getContainer(int containerId) {
        return containerData.get(containerId);
    }

    public void setContainer(int containerId, Container container) {
        containerData.put(containerId, container);
    }

    public void sendHeldItemChange() {
        dataManager.sendPacket(new ServerboundSetCarriedItemPacket(heldItemSlot));
    }

    public void closeInventory() {
        if (openContainer != null) {
            dataManager.sendPacket(new ServerboundContainerClosePacket(openContainer.id()));
            openContainer = null;
        } else {
            dataManager.sendPacket(new ServerboundContainerClosePacket(0));
        }
    }

    public void openPlayerInventory() {
        openContainer = getPlayerInventory();
    }

    public void leftClickSlot(int slot) {
        if (!inventoryControlLock.isHeldByCurrentThread()) {
            throw new IllegalStateException("You need to lock the inventoryControlLock before calling this method!");
        }

        if (openContainer == null) {
            openPlayerInventory();
        }

        SWItemStack slotItem;
        {
            var containerSlot = openContainer.getSlot(slot);
            if (containerSlot.item() == null) {
                // The target slot is empty, and we don't have an item in our cursor
                if (cursorItem == null) {
                    return;
                }

                // Place the cursor into empty slot
                slotItem = cursorItem;
                cursorItem = null;
            } else if (cursorItem == null) {
                // Take the slot into the cursor
                slotItem = null;
                cursorItem = containerSlot.item();
            } else {
                // Swap the cursor and the slot
                slotItem = cursorItem;

                cursorItem = containerSlot.item();
            }
        }

        openContainer.setSlot(slot, slotItem);
        Int2ObjectMap<ItemStack> changes = new Int2ObjectArrayMap<>(1);
        changes.put(slot, slotItem);

        dataManager.sendPacket(new ServerboundContainerClickPacket(openContainer.id(),
                lastStateId,
                slot,
                ContainerActionType.CLICK_ITEM,
                ClickItemAction.LEFT_CLICK,
                cursorItem,
                changes
        ));
    }
}
