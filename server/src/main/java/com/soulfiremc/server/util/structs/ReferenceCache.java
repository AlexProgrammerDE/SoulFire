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

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;

/**
 * Make an object use shared memory.
 * Will try to free memory when the object is not used/already in memory.
 *
 * @param <T> The type of object to pool.
 */
public class ReferenceCache<T> {
  private final Map<T, T> cache = new WeakHashMap<>();

  public T poolReference(T obj) {
    synchronized (cache) {
      return cache.computeIfAbsent(obj, Function.identity());
    }
  }
}
