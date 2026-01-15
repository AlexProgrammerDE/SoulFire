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

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

public final class Long2ObjectLRUCache<V> {
  private final Long2ObjectLinkedOpenHashMap<V> cache;
  private final int capacity;

  public Long2ObjectLRUCache(int capacity) {
    this.capacity = capacity;
    this.cache = new Long2ObjectLinkedOpenHashMap<>(capacity, 0.75f) {
      @Override
      protected void rehash(int newN) {
        // Prevent rehashing by overriding this method
        // This ensures the cache size is controlled solely by capacity
      }
    };
  }

  public V get(long key) {
    // Use getAndMoveToLast for efficient LRU access-order update
    // This is O(1) instead of O(n) for remove+put
    return cache.getAndMoveToLast(key);
  }

  public void put(long key, V value) {
    clean();
    cache.put(key, value);
  }

  public V remove(long key) {
    return cache.remove(key);
  }

  public int size() {
    return cache.size();
  }

  public void clean() {
    if (cache.size() >= capacity) {
      // Remove the least recently used entry
      var lruKey = cache.firstLongKey();
      cache.remove(lruKey);
    }
  }

  @Override
  public String toString() {
    return cache.toString();
  }
}
