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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.soulfiremc.server.util.SFHelpers;
import com.soulfiremc.server.util.structs.GsonInstance;
import net.kyori.adventure.key.Key;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class GsonDataHelper {
  public static final TypeAdapter<Key> RESOURCE_KEY_ADAPTER =
    new TypeAdapter<>() {
      @Override
      public void write(JsonWriter out, Key value) throws IOException {
        out.value(value.asString());
      }

      @Override
      @SuppressWarnings("PatternValidation")
      public Key read(JsonReader in) throws IOException {
        var key = in.nextString();
        return Key.key(key);
      }
    };
  private static final LoadingCache<String, JsonArray> LOADED_DATA = Caffeine.newBuilder()
    .expireAfterAccess(Duration.ofSeconds(1))
    .build(file -> {
      try {
        return GsonInstance.GSON.fromJson(SFHelpers.getResourceAsString(file), JsonArray.class);
      } catch (Exception e) {
        throw new RuntimeException("Failed to load data file " + file, e);
      }
    });
  private static final Function<Map<Class<?>, Object>, Gson> GSON_FACTORY = (typeAdapters) -> {
    var builder = new GsonBuilder()
      .registerTypeAdapter(Key.class, RESOURCE_KEY_ADAPTER)
      .registerTypeAdapter(ByteDataComponents.class, ByteDataComponents.SERIALIZER);

    for (var entry : typeAdapters.entrySet()) {
      builder.registerTypeAdapter(entry.getKey(), entry.getValue());
    }

    return builder.create();
  };

  public static <T> T fromJson(String dataFile, String dataKey, Class<T> clazz) {
    return fromJson(dataFile, dataKey, clazz, Map.of());
  }

  public static <T> T fromJson(String dataFile, String dataKey, Class<T> clazz,
                               Map<Class<?>, Object> typeAdapters) {
    var gson = createGson(typeAdapters);
    var array = Objects.requireNonNull(LOADED_DATA.get(dataFile));
    for (var element : array) {
      if (element.getAsJsonObject().get("key").getAsString().equals(dataKey)) {
        return gson.fromJson(element, clazz);
      }
    }

    throw new RuntimeException("Failed to find data key %s in file %s".formatted(dataKey, dataFile));
  }

  public static Gson createGson(Map<Class<?>, Object> typeAdapters) {
    return GSON_FACTORY.apply(typeAdapters);
  }
}
