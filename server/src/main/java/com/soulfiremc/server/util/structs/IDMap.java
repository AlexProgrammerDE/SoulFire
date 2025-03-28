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
package com.soulfiremc.server.util.structs;

import com.soulfiremc.server.data.IDValue;

import java.util.Collection;
import java.util.function.Function;

public final class IDMap<K extends IDValue, V> {
  private final Object[] values;

  public IDMap(Collection<K> collection, Function<K, V> valueFunction) {
    values = new Object[collection.size()];

    for (var key : collection) {
      values[key.id()] = valueFunction.apply(key);
    }
  }

  @SuppressWarnings("unchecked")
  public V get(K key) {
    return (V) values[key.id()];
  }
}
