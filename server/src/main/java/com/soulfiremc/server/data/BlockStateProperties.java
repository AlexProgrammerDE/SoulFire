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

import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.ToString;

@ToString
public class BlockStateProperties {
  private final Object2BooleanMap<String> booleanProperties;
  private final Object2IntMap<String> intProperties;
  private final Object2ObjectMap<String, String> stringProperties;

  public BlockStateProperties(JsonObject properties) {
    this.booleanProperties = new Object2BooleanOpenHashMap<>();
    this.intProperties = new Object2IntOpenHashMap<>();
    this.stringProperties = new Object2ObjectOpenHashMap<>();

    if (properties == null) {
      return;
    }

    for (var property : properties.entrySet()) {
      var key = property.getKey();
      var value = property.getValue().getAsJsonPrimitive();

      if (value.isBoolean()) {
        booleanProperties.put(key, value.getAsBoolean());
      } else if (value.isNumber()) {
        // All numeric values are int properties
        intProperties.put(key, value.getAsInt());
      } else {
        stringProperties.put(key, value.getAsString());
      }
    }
  }

  public boolean getBoolean(String key) {
    return booleanProperties.getBoolean(key);
  }

  public int getInt(String key) {
    return intProperties.getInt(key);
  }

  public String getString(String key) {
    return stringProperties.get(key);
  }
}
