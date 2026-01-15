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

public final class IDBooleanMap<K> {
  private final boolean[] values;
  private final ToIntFunction<K> idFunction;

  public IDBooleanMap(IdMapper<K> idMapper, Function<K, Boolean> valueFunction) {
    this(
      Lists.newArrayList(idMapper.iterator()),
      idMapper::getId,
      valueFunction
    );
  }

  public IDBooleanMap(Collection<K> collection, ToIntFunction<K> idFunction, Function<K, Boolean> valueFunction) {
    this.values = new boolean[collection.size()];
    this.idFunction = idFunction;

    for (var key : collection) {
      this.values[idFunction.applyAsInt(key)] = valueFunction.apply(key);
    }
  }

  public boolean get(K key) {
    return this.values[idFunction.applyAsInt(key)];
  }
}
