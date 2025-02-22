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

import com.soulfiremc.server.data.AttributeType;
import com.soulfiremc.server.data.EquipmentSlot;
import com.soulfiremc.server.protocol.bot.container.SFItemStack;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Data;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeModifier;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ItemAttributeModifiers;

import java.util.Map;

@Data
public class EntityAttributeState {
  private final Map<AttributeType, AttributeState> attributeStore = new Object2ObjectOpenHashMap<>();

  private static boolean isNotPartOf(ItemAttributeModifiers.EquipmentSlotGroup itemSlot, EquipmentSlot comparedTo) {
    return !switch (itemSlot) {
      case ANY -> true;
      case MAIN_HAND -> comparedTo == EquipmentSlot.MAINHAND;
      case OFF_HAND -> comparedTo == EquipmentSlot.OFFHAND;
      case HAND -> comparedTo.isHand();
      case FEET -> comparedTo == EquipmentSlot.FEET;
      case LEGS -> comparedTo == EquipmentSlot.LEGS;
      case CHEST -> comparedTo == EquipmentSlot.CHEST;
      case HEAD -> comparedTo == EquipmentSlot.HEAD;
      case ARMOR -> comparedTo.isArmor();
      case BODY -> comparedTo == EquipmentSlot.BODY;
    };
  }

  public boolean hasAttribute(AttributeType type) {
    return attributeStore.containsKey(type);
  }

  public AttributeState getOrCreateAttribute(AttributeType type) {
    return attributeStore.computeIfAbsent(type, k -> new AttributeState(type, type.defaultValue()));
  }

  public void putItemModifiers(SFItemStack itemStack, EquipmentSlot slot) {
    var components = itemStack.getDataComponents();
    for (var modifier : components.get(DataComponentTypes.ATTRIBUTE_MODIFIERS).getModifiers()) {
      if (isNotPartOf(modifier.getSlot(), slot)) {
        continue;
      }

      getOrCreateAttribute(AttributeType.REGISTRY.getById(modifier.getAttribute()))
        .modifiers()
        .put(modifier.getModifier().getId(), new AttributeModifier(modifier.getModifier().getId(), modifier.getModifier().getAmount(), modifier.getModifier().getOperation()));
    }
  }

  public void removeItemModifiers(SFItemStack itemStack, EquipmentSlot slot) {
    var components = itemStack.getDataComponents();
    for (var modifier : components.get(DataComponentTypes.ATTRIBUTE_MODIFIERS).getModifiers()) {
      if (isNotPartOf(modifier.getSlot(), slot)) {
        continue;
      }

      getOrCreateAttribute(AttributeType.REGISTRY.getById(modifier.getAttribute()))
        .modifiers()
        .remove(modifier.getModifier().getId());
    }
  }

  public void assignAllValues(EntityAttributeState other) {
    other.attributeStore.forEach((type, state) -> {
      var attribute = getOrCreateAttribute(type);
      attribute.baseValue(state.baseValue());
      attribute.modifiers().clear();
      attribute.modifiers().putAll(state.modifiers());
    });
  }

  public void assignBaseValues(EntityAttributeState other) {
    other.attributeStore.forEach((type, state) -> {
      var attribute = getOrCreateAttribute(type);
      attribute.baseValue(state.baseValue());
    });
  }
}
