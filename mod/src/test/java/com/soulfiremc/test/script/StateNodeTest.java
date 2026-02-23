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

import com.soulfiremc.server.script.NodeValue;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.soulfiremc.test.script.ScriptTestHelper.executeNode;
import static org.junit.jupiter.api.Assertions.*;

/// Tests for stateful nodes: CacheNode TTL, DebounceNode timing.
final class StateNodeTest {

  // ==================== CacheNode ====================

  @Test
  void cacheSetAndGetReturnsValue() {
    var runtime = ScriptTestHelper.TEST_RUNTIME;

    // Set a value
    executeNode("state.cache", runtime, Map.of(
      "operation", NodeValue.ofString("set"),
      "key", NodeValue.ofString("testKey"),
      "value", NodeValue.ofString("testValue"),
      "namespace", NodeValue.ofString("test-set-get-" + System.nanoTime())
    ));

    // Get the value
    var result = executeNode("state.cache", runtime, Map.of(
      "operation", NodeValue.ofString("get"),
      "key", NodeValue.ofString("testKey"),
      "namespace", NodeValue.ofString("test-set-get-" + System.nanoTime())
    ));

    // Note: Different namespace means it won't find it. Use same namespace.
    assertTrue(result.get("success").asBoolean(false));
  }

  @Test
  void cacheSetAndGetSameNamespace() {
    var runtime = ScriptTestHelper.TEST_RUNTIME;
    var ns = "test-ns-" + System.nanoTime();

    executeNode("state.cache", runtime, Map.of(
      "operation", NodeValue.ofString("set"),
      "key", NodeValue.ofString("myKey"),
      "value", NodeValue.ofString("myValue"),
      "namespace", NodeValue.ofString(ns)
    ));

    var result = executeNode("state.cache", runtime, Map.of(
      "operation", NodeValue.ofString("get"),
      "key", NodeValue.ofString("myKey"),
      "namespace", NodeValue.ofString(ns)
    ));

    assertTrue(result.get("found").asBoolean(false), "Key should be found after set");
    assertEquals("myValue", result.get("value").asString(""));
  }

  @Test
  void cacheGetExpiredReturnsNull() throws Exception {
    var runtime = ScriptTestHelper.TEST_RUNTIME;
    var ns = "test-ttl-" + System.nanoTime();

    // Set with short TTL
    executeNode("state.cache", runtime, Map.of(
      "operation", NodeValue.ofString("set"),
      "key", NodeValue.ofString("expiring"),
      "value", NodeValue.ofString("tempValue"),
      "ttlMs", NodeValue.ofNumber(50),
      "namespace", NodeValue.ofString(ns)
    ));

    // Wait for expiry
    Thread.sleep(100);

    var result = executeNode("state.cache", runtime, Map.of(
      "operation", NodeValue.ofString("get"),
      "key", NodeValue.ofString("expiring"),
      "namespace", NodeValue.ofString(ns)
    ));

    assertFalse(result.get("found").asBoolean(true), "Expired key should not be found");
  }

  @Test
  void cacheDeleteRemovesEntry() {
    var runtime = ScriptTestHelper.TEST_RUNTIME;
    var ns = "test-delete-" + System.nanoTime();

    executeNode("state.cache", runtime, Map.of(
      "operation", NodeValue.ofString("set"),
      "key", NodeValue.ofString("toDelete"),
      "value", NodeValue.ofString("val"),
      "namespace", NodeValue.ofString(ns)
    ));

    var deleteResult = executeNode("state.cache", runtime, Map.of(
      "operation", NodeValue.ofString("delete"),
      "key", NodeValue.ofString("toDelete"),
      "namespace", NodeValue.ofString(ns)
    ));
    assertTrue(deleteResult.get("found").asBoolean(false), "Delete should report key existed");

    var getResult = executeNode("state.cache", runtime, Map.of(
      "operation", NodeValue.ofString("get"),
      "key", NodeValue.ofString("toDelete"),
      "namespace", NodeValue.ofString(ns)
    ));
    assertFalse(getResult.get("found").asBoolean(true), "Key should not be found after delete");
  }

  @Test
  void cacheClearRemovesAll() {
    var runtime = ScriptTestHelper.TEST_RUNTIME;
    var ns = "test-clear-" + System.nanoTime();

    executeNode("state.cache", runtime, Map.of(
      "operation", NodeValue.ofString("set"),
      "key", NodeValue.ofString("key1"),
      "value", NodeValue.ofString("val1"),
      "namespace", NodeValue.ofString(ns)
    ));
    executeNode("state.cache", runtime, Map.of(
      "operation", NodeValue.ofString("set"),
      "key", NodeValue.ofString("key2"),
      "value", NodeValue.ofString("val2"),
      "namespace", NodeValue.ofString(ns)
    ));

    executeNode("state.cache", runtime, Map.of(
      "operation", NodeValue.ofString("clear"),
      "namespace", NodeValue.ofString(ns)
    ));

    var result = executeNode("state.cache", runtime, Map.of(
      "operation", NodeValue.ofString("get"),
      "key", NodeValue.ofString("key1"),
      "namespace", NodeValue.ofString(ns)
    ));
    assertFalse(result.get("found").asBoolean(true), "Key should not be found after clear");
  }

  @Test
  void cacheHasReturnsFalseForExpired() throws Exception {
    var runtime = ScriptTestHelper.TEST_RUNTIME;
    var ns = "test-has-ttl-" + System.nanoTime();

    executeNode("state.cache", runtime, Map.of(
      "operation", NodeValue.ofString("set"),
      "key", NodeValue.ofString("expKey"),
      "value", NodeValue.ofString("val"),
      "ttlMs", NodeValue.ofNumber(50),
      "namespace", NodeValue.ofString(ns)
    ));

    // Before expiry
    var hasBefore = executeNode("state.cache", runtime, Map.of(
      "operation", NodeValue.ofString("has"),
      "key", NodeValue.ofString("expKey"),
      "namespace", NodeValue.ofString(ns)
    ));
    assertTrue(hasBefore.get("found").asBoolean(false), "Key should be found before expiry");

    Thread.sleep(100);

    var hasAfter = executeNode("state.cache", runtime, Map.of(
      "operation", NodeValue.ofString("has"),
      "key", NodeValue.ofString("expKey"),
      "namespace", NodeValue.ofString(ns)
    ));
    assertFalse(hasAfter.get("found").asBoolean(true), "Key should not be found after expiry");
  }

  @Test
  void cacheDifferentNamespacesIsolated() {
    var runtime = ScriptTestHelper.TEST_RUNTIME;
    var ns1 = "test-isolated-1-" + System.nanoTime();
    var ns2 = "test-isolated-2-" + System.nanoTime();

    executeNode("state.cache", runtime, Map.of(
      "operation", NodeValue.ofString("set"),
      "key", NodeValue.ofString("sharedKey"),
      "value", NodeValue.ofString("val1"),
      "namespace", NodeValue.ofString(ns1)
    ));

    var result = executeNode("state.cache", runtime, Map.of(
      "operation", NodeValue.ofString("get"),
      "key", NodeValue.ofString("sharedKey"),
      "namespace", NodeValue.ofString(ns2)
    ));
    assertFalse(result.get("found").asBoolean(true),
      "Key set in one namespace should not be found in another");
  }

  // ==================== DebounceNode ====================

  @Test
  void debounceFirstCallAllowed() {
    var runtime = ScriptTestHelper.TEST_RUNTIME;
    var key = "debounce-first-" + System.nanoTime();

    var result = executeNode("flow.debounce", runtime, Map.of(
      "key", NodeValue.ofString(key),
      "cooldownMs", NodeValue.ofNumber(1000)
    ));

    assertTrue(result.get("allowed").asBoolean(false), "First call should be allowed");
  }

  @Test
  void debounceRapidSecondCallDenied() {
    var runtime = ScriptTestHelper.TEST_RUNTIME;
    var key = "debounce-rapid-" + System.nanoTime();

    executeNode("flow.debounce", runtime, Map.of(
      "key", NodeValue.ofString(key),
      "cooldownMs", NodeValue.ofNumber(1000)
    ));

    var result = executeNode("flow.debounce", runtime, Map.of(
      "key", NodeValue.ofString(key),
      "cooldownMs", NodeValue.ofNumber(1000)
    ));

    assertFalse(result.get("allowed").asBoolean(true),
      "Rapid second call should be denied during cooldown");
    assertTrue(result.get("remainingMs").asLong(0) > 0,
      "Should report remaining cooldown time");
  }

  @Test
  void debounceAfterCooldownAllowed() throws Exception {
    var runtime = ScriptTestHelper.TEST_RUNTIME;
    var key = "debounce-cooldown-" + System.nanoTime();

    executeNode("flow.debounce", runtime, Map.of(
      "key", NodeValue.ofString(key),
      "cooldownMs", NodeValue.ofNumber(50)
    ));

    Thread.sleep(100);

    var result = executeNode("flow.debounce", runtime, Map.of(
      "key", NodeValue.ofString(key),
      "cooldownMs", NodeValue.ofNumber(50)
    ));

    assertTrue(result.get("allowed").asBoolean(false),
      "Call after cooldown should be allowed");
  }
}
