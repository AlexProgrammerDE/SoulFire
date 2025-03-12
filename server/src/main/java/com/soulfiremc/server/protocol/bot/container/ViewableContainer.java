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

import com.soulfiremc.server.protocol.bot.state.entity.Player;
import lombok.Getter;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.*;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;

import java.util.HashMap;
import java.util.List;

public class ViewableContainer extends Container {
  private static final int SPECIAL_SLOT = -999;
  @Getter
  private final int containerId;
  private final Player player;

  public ViewableContainer(Player player, int slots, int containerId) {
    super(slots);
    this.containerId = containerId;
    this.player = player;
  }

  public void leftClick(ContainerSlot slot) {
    var carried = getCarried();
    var slotItem = slot.item();
    if (carried.isEmpty()) {
      setCarried(slotItem);
      slot.setItem(SFItemStack.EMPTY);
    } else if (SFItemStack.isSameItemSameComponents(slotItem, carried)) {
      var newCount = slotItem.getCount() + carried.getCount();
      var maxStackSize = slotItem.getMaxStackSize();
      if (newCount > maxStackSize) {
        slotItem.setCount(maxStackSize);
        carried.setCount(newCount - maxStackSize);
      } else {
        slotItem.setCount(newCount);
        setCarried(SFItemStack.EMPTY);
      }
    } else {
      setCarried(slotItem);
      slot.setItem(carried);
    }

    sendAction(ContainerActionType.CLICK_ITEM, ClickItemAction.LEFT_CLICK, slot.slot(), List.of(slot));
  }

  public void rightClick(ContainerSlot slot) {
    var carried = getCarried();
    var slotItem = slot.item();
    if (carried.isEmpty()) {
      var newCount = ((double) slotItem.getCount()) / 2;
      var clone = slotItem.copy();
      clone.setCount((int) Math.ceil(newCount));
      setCarried(clone);

      slotItem.setCount((int) Math.floor(newCount));
    } else if (SFItemStack.isSameItemSameComponents(slotItem, carried)) {
      var newCount = slotItem.getCount() + 1;
      var maxStackSize = slotItem.getMaxStackSize();
      if (newCount <= maxStackSize) {
        slotItem.setCount(newCount);
        carried.setCount(carried.getCount() - 1);
      }
    } else {
      setCarried(slotItem);
      slot.setItem(carried);
    }

    sendAction(ContainerActionType.CLICK_ITEM, ClickItemAction.RIGHT_CLICK, slot.slot(), List.of(slot));
  }

  public void leftClickOutsideInventory() {
    sendAction(ContainerActionType.CLICK_ITEM, ClickItemAction.LEFT_CLICK, SPECIAL_SLOT, List.of());
  }

  public void rightClickOutsideInventory() {
    sendAction(ContainerActionType.CLICK_ITEM, ClickItemAction.RIGHT_CLICK, SPECIAL_SLOT, List.of());
  }

  public void middleClick(ContainerSlot slot) {
    if (!getCarried().isEmpty()) {
      // MC ignores the middle click if the cursor item is not empty
      return;
    }

    var copy = slot.item();
    copy.setCount(copy.getMaxStackSize());
    setCarried(copy);

    sendAction(ContainerActionType.CREATIVE_GRAB_MAX_STACK, CreativeGrabAction.GRAB, slot.slot(), List.of());
  }

  public void dropOne(ContainerSlot slot) {
    var currentItem = slot.item();
    currentItem.setCount(currentItem.getCount() - 1);
    if (currentItem.isEmpty()) {
      slot.setItem(SFItemStack.EMPTY);
    }

    sendAction(ContainerActionType.DROP_ITEM, DropItemAction.DROP_FROM_SELECTED, slot.slot(), List.of(slot));
  }

  public void dropFullStack(ContainerSlot slot) {
    slot.setItem(SFItemStack.EMPTY);
    sendAction(ContainerActionType.DROP_ITEM, DropItemAction.DROP_SELECTED_STACK, slot.slot(), List.of(slot));
  }

  private void sendAction(ContainerActionType mode, ContainerAction button, int slot, List<ContainerSlot> changedSlots) {
    var changeMap = new HashMap<Integer, ItemStack>();
    for (var changedSlot : changedSlots) {
      changeMap.put(changedSlot.slot(), changedSlot.item().toMCPL());
    }

    player.level().connection().sendPacket(new ServerboundContainerClickPacket(containerId, getStateId(), slot, mode, button, getCarried().toMCPL(), changeMap));
  }
}
