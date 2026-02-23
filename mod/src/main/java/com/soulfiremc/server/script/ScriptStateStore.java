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
package com.soulfiremc.server.script;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/// Per-script state container with typed key access.
/// State is scoped to the ReactiveScriptContext and is automatically
/// cleaned up when the script deactivates (context is GC'd).
public final class ScriptStateStore {
  private final ConcurrentHashMap<String, Object> store = new ConcurrentHashMap<>();

  /// Gets an existing value or creates a new one using the factory.
  ///
  /// @param key     the state key
  /// @param factory supplier to create the initial value
  /// @param <T>     the value type
  /// @return the existing or newly created value
  @SuppressWarnings("unchecked")
  public <T> T getOrCreate(String key, Supplier<T> factory) {
    return (T) store.computeIfAbsent(key, _ -> factory.get());
  }

  /// Gets an existing value or creates a new one, with runtime type checking.
  ///
  /// @param key     the state key
  /// @param type    the expected type
  /// @param factory supplier to create the initial value
  /// @param <T>     the value type
  /// @return the existing or newly created value
  /// @throws IllegalStateException if existing value has a different type
  public <T> T getOrCreate(String key, Class<T> type, Supplier<T> factory) {
    var existing = store.get(key);
    if (existing != null) {
      if (!type.isInstance(existing)) {
        throw new IllegalStateException("State key '" + key + "' has type "
          + existing.getClass().getSimpleName() + " but " + type.getSimpleName() + " was requested");
      }
      return type.cast(existing);
    }
    return type.cast(store.computeIfAbsent(key, _ -> factory.get()));
  }

  /// Clears all state, releasing references for GC.
  /// Called when a script is cancelled.
  public void clear() {
    store.clear();
  }
}
