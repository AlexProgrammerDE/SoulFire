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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.soulfiremc.util.ResourceHelper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class GsonDataHelper {
  private static final Map<String, JsonArray> LOADED_DATA = new HashMap<>();
  private static final Function<Map<Class<?>, TypeAdapter<?>>, Gson> GSON_FACTORY = (typeAdapters) -> {
    var builder = new GsonBuilder()
      .registerTypeAdapter(
        ResourceKey.class,
        new TypeAdapter<ResourceKey>() {
          @Override
          public void write(JsonWriter out, ResourceKey value) throws IOException {
            out.value(value.toString());
          }

          @Override
          public ResourceKey read(JsonReader in) throws IOException {
            var key = in.nextString();
            return ResourceKey.fromString(key);
          }
        });

    for (var entry : typeAdapters.entrySet()) {
      builder.registerTypeAdapter(entry.getKey(), entry.getValue());
    }

    return builder.create();
  };

  public static <T> T fromJson(String dataFile, String dataKey, Class<T> clazz) {
    return fromJson(dataFile, dataKey, clazz, Map.of());
  }

  public static <T> T fromJson(String dataFile, String dataKey, Class<T> clazz,
                               Map<Class<?>, TypeAdapter<?>> typeAdapters) {
    var gson = GSON_FACTORY.apply(typeAdapters);
    var array =
      LOADED_DATA.computeIfAbsent(
        dataFile,
        file -> {
          var data = new JsonArray();
          try {
            data =
              gson.fromJson(ResourceHelper.getResource(file), JsonArray.class);
          } catch (Exception e) {
            throw new RuntimeException("Failed to load data file " + file, e);
          }
          return data;
        });
    for (var element : array) {
      if (element.getAsJsonObject().get("key").getAsString().equals(dataKey)) {
        return gson.fromJson(element, clazz);
      }
    }

    throw new RuntimeException("Failed to find data key " + dataKey + " in file " + dataFile);
  }
}
