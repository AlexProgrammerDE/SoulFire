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

import lombok.RequiredArgsConstructor;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class CachedLazyObject<V> {
  private final Supplier<V> supplier;
  private V value;
  private long time;

  public CachedLazyObject(Supplier<V> supplier, long time, TimeUnit unit) {
    this.supplier = supplier;
    this.time = unit.toMillis(time);
  }

  public V get() {
    if (value == null || System.currentTimeMillis() - time > 0) {
      value = supplier.get();
      time = System.currentTimeMillis() + time;
    }
    return value;
  }
}
