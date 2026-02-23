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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/// Tests for script cancellation via ReactiveScriptContext.cancel().
@Timeout(5)
final class CancellationTest {

  @Test
  void cancelledContextSkipsExecution() {
    // Pre-cancel the context before execution.
    // The engine should check isCancelled() and not execute any nodes.
    var graph = ScriptGraph.builder("test-pre-cancel", "Pre-Cancel Test")
      .addNode("trigger", "trigger.on_script_init", null)
      .addNode("print", "action.print", Map.of("message", "should not run"))
      .addExecutionEdge("trigger", "out", "print", "in")
      .build();

    var listener = new RecordingEventListener();
    var context = new ReactiveScriptContext(listener);
    context.cancel();

    var engine = new ReactiveScriptEngine();
    engine.executeFromTriggerSync(graph, "trigger", context, Map.of()).block();

    assertTrue(listener.startedNodes.isEmpty(),
      "No nodes should start when context is pre-cancelled");
    assertFalse(listener.completedNodes.contains("print"),
      "Print node should not execute");
  }

  @Test
  void cancelFiresOnScriptCancelledEvent() {
    var listener = new RecordingEventListener();
    var context = new ReactiveScriptContext(listener);

    assertFalse(context.isCancelled(), "Context should not be cancelled initially");
    context.cancel();
    assertTrue(context.isCancelled(), "Context should be cancelled after cancel()");
    assertTrue(listener.cancelled, "onScriptCancelled should be called");
  }

  @Test
  void cancelledContextStopsDownstreamNodes() {
    // Build a long chain; cancel after trigger completes.
    // Downstream nodes should not execute.
    var graph = ScriptGraph.builder("test-cancel-mid", "Cancel Mid Test")
      .addNode("trigger", "trigger.on_script_init", null)
      .addNode("print1", "action.print", Map.of("message", "first"))
      .addNode("print2", "action.print", Map.of("message", "second"))
      .addNode("print3", "action.print", Map.of("message", "third"))
      .addExecutionEdge("trigger", "out", "print1", "in")
      .addExecutionEdge("print1", "out", "print2", "in")
      .addExecutionEdge("print2", "out", "print3", "in")
      .build();

    var cancellingListener = new CancellingEventListener("print1");
    var context = new ReactiveScriptContext(cancellingListener);
    cancellingListener.context = context;

    var engine = new ReactiveScriptEngine();
    engine.executeFromTriggerSync(graph, "trigger", context, Map.of()).block();

    // print1 should have completed (it triggered the cancel)
    assertTrue(cancellingListener.completedNodes.contains("print1"),
      "print1 should complete before cancel takes effect");
    // print2 should not execute because cancel fired before it started
    assertFalse(cancellingListener.completedNodes.contains("print2"),
      "print2 should not execute because cancel fired before it started");
    // print3 should definitely not run
    assertFalse(cancellingListener.completedNodes.contains("print3"),
      "print3 should not execute after cancellation");
  }

  /// Event listener that cancels the context when a specific node completes.
  private static class CancellingEventListener extends RecordingEventListener {
    private final String cancelAfterNode;
    ReactiveScriptContext context;
    boolean cancelled;

    CancellingEventListener(String cancelAfterNode) {
      this.cancelAfterNode = cancelAfterNode;
    }

    @Override
    public void onNodeCompleted(String nodeId, Map<String, NodeValue> outputs) {
      super.onNodeCompleted(nodeId, outputs);
      if (nodeId.equals(cancelAfterNode) && context != null) {
        context.cancel();
      }
    }

    @Override
    public void onScriptCancelled() {
      cancelled = true;
    }
  }
}
