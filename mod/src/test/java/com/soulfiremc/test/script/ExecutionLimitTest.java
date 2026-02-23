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

import com.soulfiremc.server.script.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Map;

import static com.soulfiremc.test.script.ScriptTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

/// Tests for execution limit enforcement in real graph execution.
/// The engine enforces a 100,000 operation limit per execution run.
@Timeout(30)
final class ExecutionLimitTest {

  @Test
  void highIterationLoopHitsExecutionLimit() {
    // A loop with a very high count should eventually hit the 100k limit
    // and stop executing with an error.
    var graph = ScriptGraph.builder("test-exec-limit", "Execution Limit Test")
      .addNode("trigger", "trigger.on_script_init", null)
      .addNode("loop", "flow.loop", Map.of("count", 200000))
      .addNode("print", "action.print", Map.of("message", "tick"))
      .addExecutionEdge("trigger", "out", "loop", "in")
      .addExecutionEdge("loop", "exec_loop", "print", "in")
      .build();

    var listener = new RecordingEventListener();
    var context = new ReactiveScriptContext(listener);
    var engine = new ReactiveScriptEngine();
    engine.executeFromTriggerSync(graph, "trigger", context, Map.of()).block();

    // The loop should have been cut short by the execution limit
    assertFalse(listener.errorNodes.isEmpty(),
      "Execution limit should produce at least one error");

    // Verify the error message mentions the limit
    var hasLimitError = listener.errorNodes.values().stream()
      .anyMatch(msg -> msg.contains("Execution limit"));
    assertTrue(hasLimitError,
      "Error message should mention execution limit, got: " + listener.errorNodes.values());
  }

  @Test
  void normalLoopDoesNotHitLimit() {
    // A reasonable loop count should not trigger the limit.
    var graph = ScriptGraph.builder("test-normal-loop", "Normal Loop Test")
      .addNode("trigger", "trigger.on_script_init", null)
      .addNode("loop", "flow.loop", Map.of("count", 10))
      .addNode("print", "action.print", Map.of("message", "ok"))
      .addExecutionEdge("trigger", "out", "loop", "in")
      .addExecutionEdge("loop", "exec_loop", "print", "in")
      .build();

    var listener = new RecordingEventListener();
    var context = new ReactiveScriptContext(listener);
    var engine = new ReactiveScriptEngine();
    engine.executeFromTriggerSync(graph, "trigger", context, Map.of()).block();

    assertNoErrors(listener);
    assertTrue(listener.completedNodes.contains("loop"),
      "Loop should complete normally");
  }
}
