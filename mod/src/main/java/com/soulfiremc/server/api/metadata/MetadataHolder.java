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
package com.soulfiremc.server.api.metadata;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.KeyPattern;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

public final class MetadataHolder<O> {
  private final Map<Key, O> metadata = new HashMap<>();
  private final ReadWriteLock lock = new ReentrantReadWriteLock();

  public <T extends O> T getOrSet(MetadataKey<T> key, Supplier<T> defaultValue) {
    lock.readLock().lock();
    try {
      var existing = this.metadata.get(key.key());
      if (existing != null) {
        return key.cast(existing);
      }
    } finally {
      lock.readLock().unlock();
    }

    lock.writeLock().lock();
    try {
      var existing = this.metadata.get(key.key());
      if (existing != null) {
        return key.cast(existing);
      }
      var value = defaultValue.get();
      this.metadata.put(key.key(), value);
      return value;
    } finally {
      lock.writeLock().unlock();
    }
  }

  public <T extends O> T getOrDefault(MetadataKey<T> key, T defaultValue) {
    lock.readLock().lock();
    try {
      return key.cast(this.metadata.getOrDefault(key.key(), defaultValue));
    } finally {
      lock.readLock().unlock();
    }
  }

  public <T extends O> T get(MetadataKey<T> key) {
    lock.readLock().lock();
    try {
      return key.cast(this.metadata.get(key.key()));
    } finally {
      lock.readLock().unlock();
    }
  }

  public <T extends O> void set(MetadataKey<T> key, T value) {
    lock.writeLock().lock();
    try {
      this.metadata.put(key.key(), value);
    } finally {
      lock.writeLock().unlock();
    }
  }

  public <T extends O> void remove(MetadataKey<T> key) {
    lock.writeLock().lock();
    try {
      this.metadata.remove(key.key());
    } finally {
      lock.writeLock().unlock();
    }
  }

  public <T extends O> T getAndRemove(MetadataKey<T> key) {
    lock.writeLock().lock();
    try {
      return key.cast(this.metadata.remove(key.key()));
    } finally {
      lock.writeLock().unlock();
    }
  }

  public void resetFrom(Map<String, Map<String, O>> newMetadata) {
    lock.writeLock().lock();
    try {
      this.metadata.clear();
      newMetadata.forEach((@KeyPattern.Namespace String namespace, Map<String, O> keyValues) -> {
        keyValues.forEach((@KeyPattern.Value String key, O value) -> {
          this.metadata.put(Key.key(namespace, key), value);
        });
      });
    } finally {
      lock.writeLock().unlock();
    }
  }
}
