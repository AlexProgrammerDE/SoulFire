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
import com.soulfiremc.server.data.ItemType;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Data;

@Data
public class EntityAttributeState {
  private final Map<AttributeType, AttributeState> attributeStore =
    new Object2ObjectOpenHashMap<>();

  public AttributeState getOrCreateAttribute(AttributeType type) {
    return attributeStore.computeIfAbsent(type, k -> new AttributeState(type, type.defaultValue()));
  }

  public void putItemModifiers(ItemType type) {
    for (var attribute : type.attributes()) {
      getOrCreateAttribute(attribute.type())
        .modifiers()
        .putAll(
          attribute.modifiers().stream()
            .collect(Collectors.toMap(Attribute.Modifier::uuid, Function.identity())));
    }
  }

  public void removeItemModifiers(ItemType type) {
    for (var attribute : type.attributes()) {
      for (var modifier : attribute.modifiers()) {
        getOrCreateAttribute(attribute.type()).modifiers().remove(modifier.uuid());
      }
    }
  }
}
