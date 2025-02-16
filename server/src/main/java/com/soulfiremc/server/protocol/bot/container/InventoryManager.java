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
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ClickItemAction;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerActionType;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClosePacket;

import java.util.EnumMap;
import java.util.Map;

@Data
@RequiredArgsConstructor
public class InventoryManager {
  private final Int2ObjectMap<Container> containerData = new Int2ObjectOpenHashMap<>();
  private final Map<EquipmentSlot, SFItemStack> lastInEquipment = new EnumMap<>(EquipmentSlot.class);
  @ToString.Exclude
  private final BotConnection connection;
  private Container currentContainer;
  private int lastStateId = 0;
  private SFItemStack cursorItem;

  public Container getContainer(int containerId) {
    return containerData.get(containerId);
  }

  public void setContainer(int containerId, Container container) {
    containerData.put(containerId, container);
  }

  public void closeInventory() {
    if (currentContainer == null) {
      return;
    }

    connection.sendPacket(new ServerboundContainerClosePacket(currentContainer.id()));
    currentContainer = null;
  }

  public boolean lookingAtForeignContainer() {
    return currentContainer != null && currentContainer != playerInventory();
  }

  public void openPlayerInventory() {
    if (currentContainer == playerInventory()) {
      return;
    }

    closeInventory();
    currentContainer = playerInventory();
  }

  public PlayerInventoryContainer playerInventory() {
    return connection.dataManager().localPlayer().inventory();
  }

  public void changeHeldItem(int slot) {
    playerInventory().selected = slot;
  }

  public void leftClickSlot(ContainerSlot slot) {
    leftClickSlot(slot.slot());
  }

  private void leftClickSlot(int slot) {
    if (currentContainer == null) {
      throw new IllegalStateException("No container is open");
    }

    SFItemStack slotItem;
    {
      var containerSlot = currentContainer.getSlot(slot);
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

    currentContainer.setSlot(slot, slotItem);
    Int2ObjectMap<ItemStack> changes = new Int2ObjectArrayMap<>(1);
    changes.put(slot, slotItem);

    connection.sendPacket(
      new ServerboundContainerClickPacket(
        currentContainer.id(),
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
    var item = playerInventory().getEquipmentSlotItem(equipmentSlot);
    var previousItem = lastInEquipment.get(equipmentSlot);
    boolean hasChanged;
    if (previousItem != null) {
      if (item.isEmpty() || previousItem.type() != item.get().type()) {
        // Item before, but we don't have one now, or it's different
        hasChanged = true;

        // Remove the old item's modifiers
        connection.dataManager().localPlayer().attributeState().removeItemModifiers(previousItem, equipmentSlot);
      } else {
        // Item before, and we have the same one now
        hasChanged = false;
      }
    } else {
      // No item before, but we have one now
      hasChanged = item.isPresent();
    }

    if (hasChanged && item.isPresent()) {
      connection.dataManager().localPlayer().attributeState().putItemModifiers(item.get(), equipmentSlot);
    }

    lastInEquipment.put(equipmentSlot, item.orElse(null));
  }
}
