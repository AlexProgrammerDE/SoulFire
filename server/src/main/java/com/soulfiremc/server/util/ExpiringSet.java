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
package com.soulfiremc.server.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;

public class ExpiringSet<E> {
  private final Cache<E, Long> cache;
  private final long lifetime;

  public ExpiringSet(long duration, TimeUnit unit) {
    this.cache = Caffeine.newBuilder().expireAfterWrite(duration, unit).build();
    this.lifetime = unit.toMillis(duration);
  }

  public boolean add(E item) {
    var present = contains(item);
    this.cache.put(item, System.currentTimeMillis() + this.lifetime);
    return !present;
  }

  public boolean contains(E item) {
    var timeout = this.cache.getIfPresent(item);
    return timeout != null && timeout > System.currentTimeMillis();
  }

  public void remove(E item) {
    this.cache.invalidate(item);
  }
}
