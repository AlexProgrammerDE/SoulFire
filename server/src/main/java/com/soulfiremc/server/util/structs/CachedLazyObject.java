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

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class CachedLazyObject<V> implements Supplier<V> {
  private final Supplier<V> supplier;
  private final long cacheDuration;
  private V value;
  private long invalidAt;

  public CachedLazyObject(Supplier<V> supplier, long amount, TimeUnit unit) {
    this.supplier = supplier;
    this.cacheDuration = unit.toMillis(amount);
  }

  @Override
  public V get() {
    if (value == null || System.currentTimeMillis() >= invalidAt) {
      value = supplier.get();
      invalidAt = System.currentTimeMillis() + cacheDuration;
    }
    return value;
  }
}
