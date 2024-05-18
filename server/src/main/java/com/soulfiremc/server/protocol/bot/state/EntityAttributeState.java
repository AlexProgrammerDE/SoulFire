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
package com.soulfiremc.server.protocol.bot.state;

import com.soulfiremc.server.data.Attribute;
import com.soulfiremc.server.data.AttributeType;
import com.soulfiremc.server.data.EquipmentSlot;
import com.soulfiremc.server.data.ModifierOperation;
import com.soulfiremc.server.protocol.bot.container.SFItemStack;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import lombok.Data;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ItemAttributeModifiers;

@Data
public class EntityAttributeState {
  private final Map<AttributeType, AttributeState> attributeStore = new Object2ObjectOpenHashMap<>();

  private static boolean isNotPartOf(ItemAttributeModifiers.EquipmentSlotGroup itemSlot, EquipmentSlot comparedTo) {
    return !switch (itemSlot) {
      case ANY -> true;
      case MAIN_HAND -> comparedTo == EquipmentSlot.MAINHAND;
      case OFF_HAND -> comparedTo == EquipmentSlot.OFFHAND;
      case HAND -> comparedTo == EquipmentSlot.MAINHAND || comparedTo == EquipmentSlot.OFFHAND;
      case FEET -> comparedTo == EquipmentSlot.FEET;
      case LEGS -> comparedTo == EquipmentSlot.LEGS;
      case CHEST -> comparedTo == EquipmentSlot.CHEST;
      case HEAD -> comparedTo == EquipmentSlot.HEAD;
      case ARMOR -> comparedTo == EquipmentSlot.CHEST || comparedTo == EquipmentSlot.LEGS || comparedTo == EquipmentSlot.FEET;
      case BODY -> comparedTo == EquipmentSlot.HEAD || comparedTo == EquipmentSlot.CHEST || comparedTo == EquipmentSlot.LEGS || comparedTo == EquipmentSlot.FEET;
    };
  }

  public AttributeState getOrCreateAttribute(AttributeType type) {
    return attributeStore.computeIfAbsent(type, k -> new AttributeState(type, type.defaultValue()));
  }

  public void putItemModifiers(SFItemStack itemStack, EquipmentSlot slot) {
    var components = itemStack.components();
    for (var modifier : components.get(DataComponentType.ATTRIBUTE_MODIFIERS).getModifiers()) {
      if (isNotPartOf(modifier.getSlot(), slot)) {
        continue;
      }

      getOrCreateAttribute(AttributeType.REGISTRY.getById(modifier.getAttribute()))
        .modifiers()
        .put(modifier.getModifier().getId(), new Attribute.Modifier(modifier.getModifier().getId(), modifier.getModifier().getAmount(), switch (modifier.getModifier().getOperation()) {
          case ADD -> ModifierOperation.ADD_VALUE;
          case ADD_MULTIPLIED_BASE -> ModifierOperation.ADD_MULTIPLIED_BASE;
          case ADD_MULTIPLIED_TOTAL -> ModifierOperation.ADD_MULTIPLIED_TOTAL;
        }));
    }
  }

  public void removeItemModifiers(SFItemStack itemStack, EquipmentSlot slot) {
    var components = itemStack.components();
    for (var modifier : components.get(DataComponentType.ATTRIBUTE_MODIFIERS).getModifiers()) {
      if (isNotPartOf(modifier.getSlot(), slot)) {
        continue;
      }

      getOrCreateAttribute(AttributeType.REGISTRY.getById(modifier.getAttribute()))
        .modifiers()
        .remove(modifier.getModifier().getId());
    }
  }
}
