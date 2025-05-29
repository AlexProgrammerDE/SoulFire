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
import net.kyori.adventure.key.KeyPattern;

import java.util.Objects;

public record MetadataKey<T>(Key key, Class<T> type) {
  public MetadataKey {
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(type, "type");

    if (key.namespace().equals(Key.MINECRAFT_NAMESPACE)) {
      throw new IllegalArgumentException("Key namespace must not be the Minecraft namespace");
    }
  }

  @SuppressWarnings("unchecked")
  public static <R extends T, T> MetadataKey<R> of(@KeyPattern.Namespace String namespace, @KeyPattern.Value String value, Class<T> type) {
    return (MetadataKey<R>) new MetadataKey<>(Key.key(namespace, value), type);
  }

  public T cast(Object value) {
    return this.type.cast(value);
  }
}
