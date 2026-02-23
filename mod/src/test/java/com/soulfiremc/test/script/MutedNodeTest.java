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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.soulfiremc.test.script.ScriptTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

/// Tests for muted node bypass behavior.
/// Muted nodes should be skipped but their downstream chain should continue.
@Timeout(5)
final class MutedNodeTest {

  @Test
  void mutedNodeIsSkippedButDownstreamExecutes() {
    // Trigger -> mutedPrint -> afterPrint
    // The muted print should be bypassed, but afterPrint should still execute.
    var graph = ScriptGraph.builder("test-muted-node", "Muted Node Test")
      .addNode(new ScriptGraph.GraphNode("trigger", "trigger.on_script_init", null, false))
      .addNode(new ScriptGraph.GraphNode("muted_print", "action.print", Map.of("message", "should not log"), true))
      .addNode(new ScriptGraph.GraphNode("after_print", "action.print", Map.of("message", "after muted"), false))
      .addExecutionEdge("trigger", "out", "muted_print", "in")
      .addExecutionEdge("muted_print", "out", "after_print", "in")
      .build();

    var listener = new LogRecordingEventListener();
    var context = new ReactiveScriptContext(listener);
    var engine = new ReactiveScriptEngine();
    engine.executeFromTriggerSync(graph, "trigger", context, Map.of()).block();

    assertNoErrors(listener);
    // Muted node should be "completed" (bypassed) but not actually log anything
    assertTrue(listener.completedNodes.contains("muted_print"),
      "Muted node should be marked as completed");
    assertFalse(listener.logMessages.contains("should not log"),
      "Muted print should NOT produce log output");

    // Downstream node should execute normally
    assertTrue(listener.completedNodes.contains("after_print"),
      "Node after muted node should execute");
    assertTrue(listener.logMessages.contains("after muted"),
      "After muted node should log normally");
  }

  @Test
  void mutedNodePassesThroughExecContext() {
    // Trigger(on_chat with message) -> muted_print -> print(uses message from exec context)
    // The muted print should pass through the execution context so downstream print can read it.
    // Using action.print as the muted node since it has EXEC ports (in/out).
    var graph = ScriptGraph.builder("test-muted-passthrough", "Muted Passthrough Test")
      .addNode(new ScriptGraph.GraphNode("trigger", "trigger.on_chat", null, false))
      .addNode(new ScriptGraph.GraphNode("muted_print", "action.print", Map.of("message", "muted msg"), true))
      .addNode(new ScriptGraph.GraphNode("print", "action.print", null, false))
      .addExecutionEdge("trigger", "out", "muted_print", "in")
      .addExecutionEdge("muted_print", "out", "print", "in")
      .addDataEdge("trigger", "messagePlainText", "print", "message")
      .build();

    var eventInputs = Map.<String, NodeValue>of(
      "messagePlainText", NodeValue.ofString("test message"),
      "message", NodeValue.ofString("test message"),
      "timestamp", NodeValue.ofNumber(System.currentTimeMillis())
    );

    var listener = new LogRecordingEventListener();
    var context = new ReactiveScriptContext(listener);
    var engine = new ReactiveScriptEngine();
    engine.executeFromTriggerSync(graph, "trigger", context, eventInputs).block();

    assertNoErrors(listener);
    assertTrue(listener.completedNodes.contains("muted_print"),
      "Muted print should be marked as completed (bypassed)");
    assertTrue(listener.completedNodes.contains("print"),
      "Print node should execute after muted node");
    assertFalse(listener.logMessages.contains("muted msg"),
      "Muted node should not produce log output");
    assertTrue(listener.logMessages.contains("test message"),
      "Print should receive trigger data through the muted node");
  }

  @Test
  void multipleMutedNodesInChain() {
    // Trigger -> muted1 -> muted2 -> print
    var graph = ScriptGraph.builder("test-multi-muted", "Multi Muted Test")
      .addNode(new ScriptGraph.GraphNode("trigger", "trigger.on_script_init", null, false))
      .addNode(new ScriptGraph.GraphNode("muted1", "action.print", Map.of("message", "skip1"), true))
      .addNode(new ScriptGraph.GraphNode("muted2", "action.print", Map.of("message", "skip2"), true))
      .addNode(new ScriptGraph.GraphNode("final_print", "action.print", Map.of("message", "reached"), false))
      .addExecutionEdge("trigger", "out", "muted1", "in")
      .addExecutionEdge("muted1", "out", "muted2", "in")
      .addExecutionEdge("muted2", "out", "final_print", "in")
      .build();

    var listener = new LogRecordingEventListener();
    var context = new ReactiveScriptContext(listener);
    var engine = new ReactiveScriptEngine();
    engine.executeFromTriggerSync(graph, "trigger", context, Map.of()).block();

    assertNoErrors(listener);
    assertEquals(List.of("reached"), listener.logMessages,
      "Only the unmuted final print should produce output");
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
