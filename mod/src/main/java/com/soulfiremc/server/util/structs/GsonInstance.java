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
package com.soulfiremc.server.util.structs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.kyori.adventure.key.Key;

import java.io.IOException;
import java.util.ServiceLoader;

public final class GsonInstance {
  public static final Gson GSON;
  private static final TypeAdapter<Key> RESOURCE_KEY_ADAPTER =
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

  static {
    var builder = new GsonBuilder()
      .registerTypeAdapter(Key.class, RESOURCE_KEY_ADAPTER);

    for (var factory : ServiceLoader.load(TypeAdapterFactory.class)) {
      builder.registerTypeAdapterFactory(factory);
    }

    GSON = builder.create();
  }

  private GsonInstance() {
  }
}
