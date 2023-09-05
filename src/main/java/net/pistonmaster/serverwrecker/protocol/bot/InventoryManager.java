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
package net.pistonmaster.serverwrecker.protocol.bot;

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
import net.pistonmaster.serverwrecker.protocol.bot.container.Container;
import net.pistonmaster.serverwrecker.protocol.bot.container.PlayerInventoryContainer;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Data
@RequiredArgsConstructor
public class InventoryManager {
    private final Int2ObjectMap<Container> containerData = new Int2ObjectOpenHashMap<>();
    @ToString.Exclude
    private final SessionDataManager dataManager;
    private Container openContainer;
    private int heldItemSlot = -1;
    private int lastStateId = -1;
    private ItemStack cursorItem;

    protected void initPlayerInventory() {
        containerData.put(0, new PlayerInventoryContainer(dataManager.getConnection()));
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
        dataManager.getSession().send(new ServerboundSetCarriedItemPacket(heldItemSlot));
    }

    public void closeInventory() {
        if (openContainer != null) {
            dataManager.getSession().send(new ServerboundContainerClosePacket(openContainer.getId()));
            openContainer = null;
        } else {
            dataManager.getSession().send(new ServerboundContainerClosePacket(0));
        }
    }

    public void openPlayerInventory() {
        openContainer = getPlayerInventory();
    }

    public void leftClickSlot(int slot) {
        ItemStack slotItem;
        {
            ItemStack item = openContainer.getSlot(slot);
            if (item == null) {
                if (cursorItem == null) {
                    return;
                }

                openContainer.setSlot(slot, cursorItem);
                slotItem = cursorItem;
                cursorItem = null;
            } else {
                if (cursorItem == null) {
                    cursorItem = item;
                    openContainer.setSlot(slot, null);
                    slotItem = null;
                } else {
                    slotItem = cursorItem;
                    openContainer.setSlot(slot, cursorItem);
                    cursorItem = item;
                }
            }
        }

        Int2ObjectMap<ItemStack> changes = new Int2ObjectArrayMap<>(1);
        changes.put(slot, slotItem);

        dataManager.getSession().send(new ServerboundContainerClickPacket(openContainer.getId(),
                lastStateId,
                slot,
                ContainerActionType.CLICK_ITEM,
                ClickItemAction.LEFT_CLICK,
                cursorItem,
                changes
        ));
    }
}
