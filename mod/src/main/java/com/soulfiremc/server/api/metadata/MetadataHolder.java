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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

public final class MetadataHolder<O> {
  private final Map<Key, O> metadata = new HashMap<>();
  private final ReadWriteLock lock = new ReentrantReadWriteLock();
  private boolean dirty;
  private long revision;

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
      dirty = true;
      revision++;
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
    set(key.key(), value, true);
  }

  public <T extends O> void remove(MetadataKey<T> key) {
    remove(key.key(), true);
  }

  public <T extends O> T getAndRemove(MetadataKey<T> key) {
    return key.cast(remove(key.key(), true));
  }

  public void resetFrom(Map<String, Map<String, O>> newMetadata) {
    lock.writeLock().lock();
    try {
      this.metadata.clear();
      newMetadata.forEach((@KeyPattern.Namespace String namespace, Map<String, O> keyValues) ->
        keyValues.forEach((@KeyPattern.Value String key, O value) ->
          this.metadata.put(Key.key(namespace, key), value)));
      dirty = false;
      revision++;
    } finally {
      lock.writeLock().unlock();
    }
  }

  public void set(@KeyPattern.Namespace String namespace, @KeyPattern.Value String key, O value) {
    set(Key.key(namespace, key), value, true);
  }

  public void remove(@KeyPattern.Namespace String namespace, @KeyPattern.Value String key) {
    remove(Key.key(namespace, key), true);
  }

  /// Applies a value already stored by an external writer without clearing other pending changes.
  public void setPersisted(@KeyPattern.Namespace String namespace, @KeyPattern.Value String key, O value) {
    set(Key.key(namespace, key), value, false);
  }

  /// Applies a deletion already stored by an external writer without clearing other pending changes.
  public void removePersisted(@KeyPattern.Namespace String namespace, @KeyPattern.Value String key) {
    remove(Key.key(namespace, key), false);
  }

  private void set(Key key, O value, boolean markDirty) {
    lock.writeLock().lock();
    try {
      metadata.put(key, value);
      revision++;
      if (markDirty) {
        dirty = true;
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  private O remove(Key key, boolean markDirty) {
    lock.writeLock().lock();
    try {
      if (!metadata.containsKey(key)) {
        return null;
      }
      var removed = metadata.remove(key);
      revision++;
      if (markDirty) {
        dirty = true;
      }
      return removed;
    } finally {
      lock.writeLock().unlock();
    }
  }

  public void markClean() {
    lock.writeLock().lock();
    try {
      dirty = false;
    } finally {
      lock.writeLock().unlock();
    }
  }

  /// Acknowledges a saved snapshot only while no newer mutation has changed this holder.
  public void markClean(Snapshot<O> saved) {
    lock.writeLock().lock();
    try {
      if (revision == saved.revision()) {
        dirty = false;
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  public Optional<Map<String, Map<String, O>>> exportIfDirty() {
    return snapshotIfDirty().map(Snapshot::values);
  }

  public Optional<Snapshot<O>> snapshotIfDirty() {
    lock.readLock().lock();
    try {
      if (!dirty) {
        return Optional.empty();
      }
      var result = new LinkedHashMap<String, Map<String, O>>();
      for (var entry : this.metadata.entrySet()) {
        var key = entry.getKey();
        result.computeIfAbsent(key.namespace(), _ -> new LinkedHashMap<>())
          .put(key.value(), entry.getValue());
      }
      return Optional.of(new Snapshot<>(result, revision));
    } finally {
      lock.readLock().unlock();
    }
  }

  public record Snapshot<O>(Map<String, Map<String, O>> values, long revision) {}
}
