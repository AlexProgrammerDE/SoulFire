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

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.kyori.adventure.key.Key;

public final class Registry<T> {
  private final Object2ObjectMap<Key, T> byKey = new Object2ObjectOpenHashMap<>();
  private final Int2ObjectMap<T> byId = new Int2ObjectOpenHashMap<>();

  public void register(Key key, int id, T value) {
    byKey.put(key, value);
    byId.put(id, value);
  }

  public T get(Key key) {
    return byKey.get(key);
  }

  public T get(int id) {
    return byId.get(id);
  }

  public int size() {
    return byKey.size();
  }

  public Key getKey(T value) {
    for (var entry : byKey.object2ObjectEntrySet()) {
      if (entry.getValue() == value) {
        return entry.getKey();
      }
    }

    throw new IllegalArgumentException("Value not found in registry");
  }

  public int getId(T value) {
    for (var entry : byId.int2ObjectEntrySet()) {
      if (entry.getValue() == value) {
        return entry.getIntKey();
      }
    }

    throw new IllegalArgumentException("Value not found in registry");
  }
}
