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

import com.google.common.collect.Lists;
import net.minecraft.core.IdMapper;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.ToIntFunction;

public final class IDMap<K, V> {
  private final Object[] values;
  private final ToIntFunction<K> idFunction;

  public IDMap(IdMapper<K> idMapper, Function<K, V> valueFunction) {
    this(
      Lists.newArrayList(idMapper.iterator()),
      idMapper::getId,
      valueFunction
    );
  }

  public IDMap(Collection<K> collection, ToIntFunction<K> idFunction, Function<K, V> valueFunction) {
    this.values = new Object[collection.size()];
    this.idFunction = idFunction;

    for (var key : collection) {
      this.values[idFunction.applyAsInt(key)] = valueFunction.apply(key);
    }
  }

  @SuppressWarnings("unchecked")
  public V get(K key) {
    return (V) this.values[idFunction.applyAsInt(key)];
  }
}
