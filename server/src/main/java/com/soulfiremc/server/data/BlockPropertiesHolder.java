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
package com.soulfiremc.server.data;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.soulfiremc.server.data.block.Property;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.ToString;

@ToString
public class BlockPropertiesHolder {
  public static final JsonDeserializer<BlockPropertiesHolder> BLOCK_STATE_PROPERTIES =
    (json, typeOfT, context) -> new BlockPropertiesHolder(json.getAsJsonObject());
  private final Object2ObjectMap<String, String> stateProperties = new Object2ObjectOpenHashMap<>();

  public BlockPropertiesHolder(JsonObject properties) {
    if (properties == null) {
      return;
    }

    for (var property : properties.entrySet()) {
      var key = property.getKey();
      var value = property.getValue().getAsJsonPrimitive();

      stateProperties.put(key, value.getAsString());
    }
  }

  public <T> T get(Property<T> property) {
    return property.parse(stateProperties.get(property.name()))
      .orElseThrow(() -> new IllegalStateException("Invalid property value"));
  }
}
