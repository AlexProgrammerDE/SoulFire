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
package com.soulfiremc.server.protocol.bot.container;

import com.soulfiremc.server.data.EquipmentSlot;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.protocol.bot.SessionDataManager;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ClickItemAction;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerActionType;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClosePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundSetCarriedItemPacket;

@Data
@RequiredArgsConstructor
public class InventoryManager {
  private final PlayerInventoryContainer playerInventory = new PlayerInventoryContainer(this);
  private final Int2ObjectMap<Container> containerData =
    new Int2ObjectOpenHashMap<>(Map.of(0, playerInventory));
  private final Map<EquipmentSlot, SFItemStack> lastInEquipment = new EnumMap<>(EquipmentSlot.class);
  private final ReentrantLock inventoryControlLock = new ReentrantLock();
  @ToString.Exclude
  private final SessionDataManager dataManager;
  @ToString.Exclude
  private final BotConnection connection;
  private Container openContainer;
  private int heldItemSlot = 0;
  private int lastStateId = -1;
  private SFItemStack cursorItem;

  /**
   * The inventory has a control lock to prevent multiple threads from moving items at the same
   * time.
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

  public Container getContainer(int containerId) {
    return containerData.get(containerId);
  }

  public void setContainer(int containerId, Container container) {
    containerData.put(containerId, container);
  }

  public void sendHeldItemChange() {
    connection.sendPacket(new ServerboundSetCarriedItemPacket(heldItemSlot));
  }

  public void closeInventory() {
    if (openContainer != null) {
      connection.sendPacket(new ServerboundContainerClosePacket(openContainer.id()));
      openContainer = null;
    } else {
      connection.sendPacket(new ServerboundContainerClosePacket(0));
    }
  }

  public void openPlayerInventory() {
    openContainer = playerInventory();
  }

  public void leftClickSlot(ContainerSlot slot) {
    leftClickSlot(slot.slot());
  }

  public void leftClickSlot(int slot) {
    if (!inventoryControlLock.isHeldByCurrentThread()) {
      throw new IllegalStateException(
        "You need to lock the inventoryControlLock before calling this method!");
    }

    if (openContainer == null) {
      openPlayerInventory();
    }

    SFItemStack slotItem;
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

    connection.sendPacket(
      new ServerboundContainerClickPacket(
        openContainer.id(),
        lastStateId,
        slot,
        ContainerActionType.CLICK_ITEM,
        ClickItemAction.LEFT_CLICK,
        cursorItem,
        changes));
  }

  public void applyItemAttributes() {
    applyIfMatches(EquipmentSlot.MAINHAND);
    applyIfMatches(EquipmentSlot.OFFHAND);
    applyIfMatches(EquipmentSlot.HEAD);
    applyIfMatches(EquipmentSlot.CHEST);
    applyIfMatches(EquipmentSlot.LEGS);
    applyIfMatches(EquipmentSlot.FEET);
  }

  private void applyIfMatches(EquipmentSlot equipmentSlot) {
    var item = playerInventory.getEquipmentSlot(equipmentSlot).item();
    var previousItem = lastInEquipment.get(equipmentSlot);
    boolean hasChanged;
    if (previousItem != null) {
      if (item == null || previousItem.type() != item.type()) {
        // Item before, but we don't have one now, or it's different
        hasChanged = true;

        // Remove the old item's modifiers
        dataManager.clientEntity().attributeState().removeItemModifiers(previousItem, equipmentSlot);
      } else {
        // Item before, and we have the same one now
        hasChanged = false;
      }
    } else {
      // No item before, but we have one now
      hasChanged = item != null;
    }

    if (hasChanged && item != null) {
      dataManager.clientEntity().attributeState().putItemModifiers(item, equipmentSlot);
    }

    lastInEquipment.put(equipmentSlot, item);
  }
}
