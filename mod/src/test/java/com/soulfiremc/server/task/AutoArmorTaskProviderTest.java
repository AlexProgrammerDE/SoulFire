/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.task;

import com.soulfiremc.test.utils.TestBootstrap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.Equippable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class AutoArmorTaskProviderTest {
  @BeforeAll
  static void bootstrapMinecraft() {
    TestBootstrap.bootstrapForTest();
  }

  @Test
  void doesNotScheduleAnUpgradeForAnUnremovableItem() {
    var player = mock(Player.class);
    var inventory = inventory(player);
    when(inventory.getSlot(5).getItem()).thenReturn(armor(EquipmentSlot.HEAD, EquipmentSlotGroup.HEAD, 1));
    when(inventory.getSlot(9).getItem()).thenReturn(armor(EquipmentSlot.HEAD, EquipmentSlotGroup.HEAD, 3));
    when(inventory.getSlot(5).mayPickup(player)).thenReturn(false);

    assertTrue(AutoArmorTaskProvider.findUpgrade(inventory, player).isEmpty());
  }

  @Test
  void keepsLookingForAnUpgradeWhenAnotherArmorSlotIsBlocked() {
    var player = mock(Player.class);
    var inventory = inventory(player);
    when(inventory.getSlot(5).getItem()).thenReturn(armor(EquipmentSlot.HEAD, EquipmentSlotGroup.HEAD, 1));
    when(inventory.getSlot(9).getItem()).thenReturn(armor(EquipmentSlot.HEAD, EquipmentSlotGroup.HEAD, 3));
    when(inventory.getSlot(5).mayPickup(player)).thenReturn(false);
    when(inventory.getSlot(6).getItem()).thenReturn(armor(EquipmentSlot.CHEST, EquipmentSlotGroup.CHEST, 3));
    when(inventory.getSlot(10).getItem()).thenReturn(armor(EquipmentSlot.CHEST, EquipmentSlotGroup.CHEST, 8));

    var upgrade = AutoArmorTaskProvider.findUpgrade(inventory, player).orElseThrow();
    assertEquals(10, upgrade.sourceSlot());
    assertEquals(6, upgrade.equipmentSlot());
  }

  private static InventoryMenu inventory(Player player) {
    var inventory = mock(InventoryMenu.class);
    for (var index = InventoryMenu.ARMOR_SLOT_START; index < InventoryMenu.USE_ROW_SLOT_END; index++) {
      var slot = mock(Slot.class);
      // Menu slot identity is assigned when vanilla adds a slot to its menu.
      slot.index = index;
      when(slot.getItem()).thenReturn(ItemStack.EMPTY);
      when(slot.mayPickup(player)).thenReturn(true);
      when(inventory.getSlot(index)).thenReturn(slot);
    }
    return inventory;
  }

  private static ItemStack armor(EquipmentSlot slot, EquipmentSlotGroup group, double armor) {
    var stack = new ItemStack(Holder.direct(Items.LEATHER_HELMET, DataComponentMap.EMPTY));
    stack.set(DataComponents.EQUIPPABLE, Equippable.builder(slot).build());
    stack.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder()
      .add(Attributes.ARMOR,
        new AttributeModifier(Identifier.parse("soulfire:test_armor"), armor, AttributeModifier.Operation.ADD_VALUE), group)
      .build());
    return stack;
  }
}
