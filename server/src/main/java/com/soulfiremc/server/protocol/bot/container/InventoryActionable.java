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

import org.geysermc.mcprotocollib.protocol.data.game.inventory.*;

/**
 * Represents an object that can perform actions on an inventory.
 * More info in the <a href="https://minecraft.wiki/w/Java_Edition_protocol#Click_Container">protocol wiki</a>.
 */
public interface InventoryActionable {
  int SPECIAL_SLOT = -999;

  default void leftClick(ContainerSlot slot) {
    sendAction(ContainerActionType.CLICK_ITEM, ClickItemAction.LEFT_CLICK, slot.slot());
  }

  default void rightClick(ContainerSlot slot) {
    sendAction(ContainerActionType.CLICK_ITEM, ClickItemAction.RIGHT_CLICK, slot.slot());
  }

  default void leftClickOutsideInventory() {
    sendAction(ContainerActionType.CLICK_ITEM, ClickItemAction.LEFT_CLICK, SPECIAL_SLOT);
  }

  default void rightClickOutsideInventory() {
    sendAction(ContainerActionType.CLICK_ITEM, ClickItemAction.RIGHT_CLICK, SPECIAL_SLOT);
  }

  default void shiftLeftClick(ContainerSlot slot) {
    sendAction(ContainerActionType.SHIFT_CLICK_ITEM, ShiftClickItemAction.LEFT_CLICK, slot.slot());
  }

  default void shiftRightClick(ContainerSlot slot) {
    sendAction(ContainerActionType.SHIFT_CLICK_ITEM, ShiftClickItemAction.RIGHT_CLICK, slot.slot());
  }

  default void numberKey(ContainerSlot slot, MoveToHotbarAction targetSlot) {
    sendAction(ContainerActionType.MOVE_TO_HOTBAR_SLOT, targetSlot, slot.slot());
  }

  default void middleClick(ContainerSlot slot) {
    sendAction(ContainerActionType.CREATIVE_GRAB_MAX_STACK, CreativeGrabAction.GRAB, slot.slot());
  }

  default void dropOne(ContainerSlot slot) {
    sendAction(ContainerActionType.DROP_ITEM, DropItemAction.DROP_FROM_SELECTED, slot.slot());
  }

  default void dropFullStack(ContainerSlot slot) {
    sendAction(ContainerActionType.DROP_ITEM, DropItemAction.DROP_SELECTED_STACK, slot.slot());
  }

  default void spreadStartLeft() {
    sendAction(ContainerActionType.SPREAD_ITEM, SpreadItemAction.LEFT_MOUSE_BEGIN_DRAG, SPECIAL_SLOT);
  }

  default void spreadStartRight() {
    sendAction(ContainerActionType.SPREAD_ITEM, SpreadItemAction.RIGHT_MOUSE_BEGIN_DRAG, SPECIAL_SLOT);
  }

  default void spreadStartMiddle() {
    sendAction(ContainerActionType.SPREAD_ITEM, SpreadItemAction.MIDDLE_MOUSE_BEGIN_DRAG, SPECIAL_SLOT);
  }

  default void spreadAddLeft(ContainerSlot slot) {
    sendAction(ContainerActionType.SPREAD_ITEM, SpreadItemAction.LEFT_MOUSE_ADD_SLOT, slot.slot());
  }

  default void spreadAddRight(ContainerSlot slot) {
    sendAction(ContainerActionType.SPREAD_ITEM, SpreadItemAction.RIGHT_MOUSE_ADD_SLOT, slot.slot());
  }

  default void spreadAddMiddle(ContainerSlot slot) {
    sendAction(ContainerActionType.SPREAD_ITEM, SpreadItemAction.MIDDLE_MOUSE_ADD_SLOT, slot.slot());
  }

  default void spreadEndLeft() {
    sendAction(ContainerActionType.SPREAD_ITEM, SpreadItemAction.LEFT_MOUSE_END_DRAG, SPECIAL_SLOT);
  }

  default void spreadEndRight() {
    sendAction(ContainerActionType.SPREAD_ITEM, SpreadItemAction.RIGHT_MOUSE_END_DRAG, SPECIAL_SLOT);
  }

  default void spreadEndMiddle() {
    sendAction(ContainerActionType.SPREAD_ITEM, SpreadItemAction.MIDDLE_MOUSE_END_DRAG, SPECIAL_SLOT);
  }

  default void doubleClick(ContainerSlot slot) {
    sendAction(ContainerActionType.FILL_STACK, FillStackAction.FILL, slot.slot());
  }

  void sendAction(ContainerActionType mode, ContainerAction button, int slot);
}
