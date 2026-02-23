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
import com.soulfiremc.server.script.nodes.NodeRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.soulfiremc.test.script.ScriptTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

/// Tests for error handling and routing in the reactive script engine.
/// Covers: exec_error port routing, branch termination on error, error message propagation.
@Timeout(5)
final class ErrorHandlingTest {

  @Test
  void nodeWithErrorPortRoutesToErrorBranch() {
    // WebFetch with empty URL produces exec_error output.
    // Connect exec_error to a print node to verify error routing.
    var graph = ScriptGraph.builder("test-error-routing", "Error Routing Test")
      .addNode("trigger", "trigger.on_script_init", null)
      .addNode("fetch", "network.web_fetch", Map.of("url", ""))
      .addNode("success_print", "action.print", Map.of("message", "success"))
      .addNode("error_print", "action.print", Map.of("message", "error"))
      .addExecutionEdge("trigger", "out", "fetch", "in")
      .addExecutionEdge("fetch", "exec_success", "success_print", "in")
      .addExecutionEdge("fetch", "exec_error", "error_print", "in")
      .build();

    var listener = new LogRecordingEventListener();
    var context = new ReactiveScriptContext(listener);
    var engine = new ReactiveScriptEngine();
    engine.executeFromTriggerSync(graph, "trigger", context, Map.of()).block();

    assertTrue(listener.completedNodes.contains("fetch"),
      "Fetch node should complete (with error output)");
    assertTrue(listener.completedNodes.contains("error_print"),
      "Error print node should execute via exec_error port");
    assertFalse(listener.completedNodes.contains("success_print"),
      "Success print node should NOT execute");
    assertTrue(listener.logMessages.contains("error"),
      "Error branch should log 'error'");
  }

  @Test
  void nodeWithoutErrorPortStopsBranchOnError() {
    // A node that throws an error without an exec_error port should stop the branch.
    // Use a node that requires a bot but don't provide one (will throw IllegalStateException).
    var graph = ScriptGraph.builder("test-error-stops-branch", "Error Stops Branch Test")
      .addNode("trigger", "trigger.on_script_init", null)
      .addNode("failing", "action.move_forward", null) // requires bot
      .addNode("after", "action.print", Map.of("message", "should not run"))
      .addExecutionEdge("trigger", "out", "failing", "in")
      .addExecutionEdge("failing", "out", "after", "in")
      .build();

    var listener = new LogRecordingEventListener();
    var context = new ReactiveScriptContext(listener);
    var engine = new ReactiveScriptEngine();
    engine.executeFromTriggerSync(graph, "trigger", context, Map.of()).block();

    assertFalse(listener.errorNodes.isEmpty(),
      "Should have error for failing node");
    assertTrue(listener.errorNodes.containsKey("failing"),
      "Error should be on the failing node");
    assertFalse(listener.completedNodes.contains("after"),
      "Node after error should NOT execute when no error port exists");
  }

  @Test
  void errorMessagePropagatedToListener() {
    // Verify the error message from a failing node reaches the event listener.
    var graph = ScriptGraph.builder("test-error-message", "Error Message Test")
      .addNode("trigger", "trigger.on_script_init", null)
      .addNode("failing", "action.move_forward", null) // requires bot, will throw
      .addExecutionEdge("trigger", "out", "failing", "in")
      .build();

    var listener = new RecordingEventListener();
    var context = new ReactiveScriptContext(listener);
    var engine = new ReactiveScriptEngine();
    engine.executeFromTriggerSync(graph, "trigger", context, Map.of()).block();

    assertTrue(listener.errorNodes.containsKey("failing"),
      "Should have error for failing node");
    var errorMsg = listener.errorNodes.get("failing");
    assertNotNull(errorMsg, "Error message should not be null");
    assertFalse(errorMsg.isEmpty(), "Error message should not be empty");
  }

  @Test
  void errorPortOutputsContainErrorMessage() {
    // When a node with exec_error port fails, the error outputs should contain
    // success=false and an errorMessage.
    var graph = ScriptGraph.builder("test-error-outputs", "Error Outputs Test")
      .addNode("trigger", "trigger.on_script_init", null)
      .addNode("fetch", "network.web_fetch", Map.of("url", ""))
      .addNode("error_handler", "action.print", null)
      .addExecutionEdge("trigger", "out", "fetch", "in")
      .addExecutionEdge("fetch", "exec_error", "error_handler", "in")
      .addDataEdge("fetch", "errorMessage", "error_handler", "message")
      .build();

    var listener = new LogRecordingEventListener();
    var context = new ReactiveScriptContext(listener);
    var engine = new ReactiveScriptEngine();
    engine.executeFromTriggerSync(graph, "trigger", context, Map.of()).block();

    assertTrue(listener.completedNodes.contains("error_handler"),
      "Error handler should execute");
    var fetchOutputs = listener.nodeOutputs.get("fetch");
    assertNotNull(fetchOutputs, "Fetch should have outputs");
    assertFalse(fetchOutputs.get("success").asBoolean(true), "Success should be false on error");
    assertFalse(fetchOutputs.get("errorMessage").asString("").isEmpty(),
      "Error message should be populated");
  }

  @Test
  void errorInMiddleOfChainStopsDownstream() {
    // Trigger -> Print1 -> FailingNode -> Print2
    // Print1 should run, FailingNode should error, Print2 should not run.
    var graph = ScriptGraph.builder("test-error-mid-chain", "Error Mid Chain Test")
      .addNode("trigger", "trigger.on_script_init", null)
      .addNode("print1", "action.print", Map.of("message", "before"))
      .addNode("failing", "action.move_forward", null) // requires bot
      .addNode("print2", "action.print", Map.of("message", "after"))
      .addExecutionEdge("trigger", "out", "print1", "in")
      .addExecutionEdge("print1", "out", "failing", "in")
      .addExecutionEdge("failing", "out", "print2", "in")
      .build();

    var listener = new LogRecordingEventListener();
    var context = new ReactiveScriptContext(listener);
    var engine = new ReactiveScriptEngine();
    engine.executeFromTriggerSync(graph, "trigger", context, Map.of()).block();

    assertTrue(listener.completedNodes.contains("print1"),
      "Print before error should execute");
    assertTrue(listener.logMessages.contains("before"),
      "First print should log 'before'");
    assertFalse(listener.completedNodes.contains("print2"),
      "Print after error should NOT execute");
  }

  /// Extended RecordingEventListener that also captures log messages.
  private static class LogRecordingEventListener extends RecordingEventListener {
    final List<String> logMessages = new ArrayList<>();

    @Override
    public void onLog(String level, String message) {
      logMessages.add(message);
    }
  }
}
