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
package com.soulfiremc.test.script;

import com.soulfiremc.server.script.ScriptStateStore;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/// Tests for {@link ScriptStateStore} typed getOrCreate and clear.
final class ScriptStateStoreTest {

  @Test
  void typedGetOrCreateReturnsCorrectType() {
    var store = new ScriptStateStore();
    var counter = store.getOrCreate("counter", AtomicInteger.class, AtomicInteger::new);
    assertNotNull(counter);
    assertEquals(0, counter.get());

    counter.incrementAndGet();

    var same = store.getOrCreate("counter", AtomicInteger.class, AtomicInteger::new);
    assertSame(counter, same, "Should return the same instance");
    assertEquals(1, same.get());
  }

  @Test
  void typedGetOrCreateRejectsTypeMismatch() {
    var store = new ScriptStateStore();
    store.getOrCreate("key", ArrayList.class, ArrayList::new);

    assertThrows(IllegalStateException.class, () ->
        store.getOrCreate("key", AtomicInteger.class, AtomicInteger::new),
      "Should throw when requesting different type for same key");
  }

  @Test
  void clearRemovesAllState() {
    var store = new ScriptStateStore();
    store.getOrCreate("a", () -> "value-a");
    store.getOrCreate("b", () -> "value-b");

    store.clear();

    // After clear, getOrCreate should create new values
    var newA = store.getOrCreate("a", () -> "new-a");
    assertEquals("new-a", newA, "Should create new value after clear");
  }
}
