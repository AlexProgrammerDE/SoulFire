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
package com.soulfiremc.server.api.metadata;

import net.kyori.adventure.key.Key;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class MetadataHolder {
  private final Map<Key, Object> metadata = new ConcurrentHashMap<>();

  public <T> T getOrSet(MetadataKey<T> key, Supplier<T> defaultValue) {
    return key.cast(this.metadata.computeIfAbsent(key.key(), k -> defaultValue.get()));
  }

  public <T> T getOrDefault(MetadataKey<T> key, T defaultValue) {
    return key.cast(this.metadata.getOrDefault(key.key(), defaultValue));
  }

  public <T> T get(MetadataKey<T> key) {
    return key.cast(this.metadata.get(key.key()));
  }

  public <T> void set(MetadataKey<T> key, T value) {
    this.metadata.put(key.key(), value);
  }

  public <T> void remove(MetadataKey<T> key) {
    this.metadata.remove(key.key());
  }

  public <T> T getAndRemove(MetadataKey<T> key) {
    return key.cast(this.metadata.remove(key.key()));
  }
}
