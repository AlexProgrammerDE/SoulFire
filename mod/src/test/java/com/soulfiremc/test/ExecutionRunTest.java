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
package com.soulfiremc.test;

import com.soulfiremc.server.script.ExecutionRun;
import com.soulfiremc.server.script.NodeValue;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/// Tests for {@link ExecutionRun} isolation, limits, publishing, and check result.
final class ExecutionRunTest {

  @Test
  void executionRunIsolation() {
    var run1 = new ExecutionRun();
    var run2 = new ExecutionRun();

    var outputs1 = Map.of("value", NodeValue.ofString("run1"));
    var outputs2 = Map.of("value", NodeValue.ofString("run2"));

    run1.publishNodeOutputs("node1", outputs1);
    run2.publishNodeOutputs("node1", outputs2);

    var result1 = run1.awaitNodeOutputs("node1", "node1").block();
    var result2 = run2.awaitNodeOutputs("node1", "node1").block();

    assertNotNull(result1, "Run1 result should not be null");
    assertNotNull(result2, "Run2 result should not be null");
    assertEquals("run1", result1.get("value").asString(""), "Run1 should have its own value");
    assertEquals("run2", result2.get("value").asString(""), "Run2 should have its own value");
  }

  @Test
  void executionRunAwaitBeforePublish() {
    var run = new ExecutionRun();

    var mono = run.awaitNodeOutputs("node1", "node1");
    run.publishNodeOutputs("node1", Map.of("value", NodeValue.ofNumber(42)));

    var result = mono.block();
    assertNotNull(result, "Awaited result should not be null");
    assertEquals(42, result.get("value").asInt(0), "Awaited value should be 42");
  }

  @Test
  void executionRunSupportsMultiplePublishes() {
    var run = new ExecutionRun();

    run.publishNodeOutputs("node1", Map.of("value", NodeValue.ofNumber(1)));
    run.publishNodeOutputs("node1", Map.of("value", NodeValue.ofNumber(2)));
    run.publishNodeOutputs("node1", Map.of("value", NodeValue.ofNumber(3)));

    var result = run.awaitNodeOutputs("node1", "node1").block();
    assertNotNull(result, "Result should not be null after multiple publishes");
    assertEquals(3, result.get("value").asInt(0), "Should get the latest published value");
  }

  @Test
  void executionRunLimitEnforced() {
    var run = new ExecutionRun();

    for (var i = 0; i < 100_000; i++) {
      assertTrue(run.incrementAndCheckLimit(), "Should allow execution " + i);
    }

    assertFalse(run.incrementAndCheckLimit(), "Should deny execution after limit");
  }

  @Test
  void markDataNodeTriggeredReturnsTrueOnFirstCall() {
    var run = new ExecutionRun();

    assertTrue(run.markDataNodeTriggered("node1"));
    assertFalse(run.markDataNodeTriggered("node1"),
      "Second call should return false (already triggered)");
  }

  @Test
  void markDataNodeTriggeredIsolatedPerNode() {
    var run = new ExecutionRun();

    assertTrue(run.markDataNodeTriggered("node1"));
    assertTrue(run.markDataNodeTriggered("node2"),
      "Different node should be independently triggerable");
  }

  @Test
  void executionRunCheckResultDefaultsFalse() {
    var run = new ExecutionRun();
    assertFalse(run.getAndResetCheckResult(), "Check result should default to false");
  }

  @Test
  void executionRunCheckResultSetAndReset() {
    var run = new ExecutionRun();

    run.setCheckResult(true);
    assertTrue(run.getAndResetCheckResult(), "Should return true after set");
    assertFalse(run.getAndResetCheckResult(), "Should return false after reset");
  }

  @Test
  void executionRunResetDataNodeTriggers() {
    var run = new ExecutionRun();

    assertTrue(run.markDataNodeTriggered("node1"), "First trigger should succeed");
    assertFalse(run.markDataNodeTriggered("node1"), "Second trigger should fail");

    run.resetDataNodeTriggers();
    assertTrue(run.markDataNodeTriggered("node1"),
      "After reset, trigger should succeed again");
  }
}
