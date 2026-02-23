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

import java.util.*;

import static com.soulfiremc.test.script.ScriptTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

/// Tests for scripts with multiple triggers.
/// Each trigger should activate independently and not interfere with others.
@Timeout(5)
final class MultiTriggerTest {

  @Test
  void multipleTriggerNodesAreDetected() {
    var graph = ScriptGraph.builder("test-multi-trigger", "Multi Trigger")
      .addNode("trigger_init", "trigger.on_script_init", null)
      .addNode("trigger_chat", "trigger.on_chat", null)
      .addNode("print1", "action.print", Map.of("message", "from init"))
      .addNode("print2", "action.print", Map.of("message", "from chat"))
      .addExecutionEdge("trigger_init", "out", "print1", "in")
      .addExecutionEdge("trigger_chat", "out", "print2", "in")
      .build();

    var triggers = graph.findTriggerNodes();
    assertEquals(2, triggers.size(), "Should find 2 triggers");
    assertTrue(triggers.contains("trigger_init"));
    assertTrue(triggers.contains("trigger_chat"));
  }

  @Test
  void eachTriggerActivatesItsOwnBranch() {
    // Two triggers in the same graph, each with its own downstream chain.
    // Firing trigger_init should only execute print1.
    var graph = ScriptGraph.builder("test-trigger-isolation", "Trigger Isolation")
      .addNode("trigger_init", "trigger.on_script_init", null)
      .addNode("trigger_chat", "trigger.on_chat", null)
      .addNode("print1", "action.print", Map.of("message", "init branch"))
      .addNode("print2", "action.print", Map.of("message", "chat branch"))
      .addExecutionEdge("trigger_init", "out", "print1", "in")
      .addExecutionEdge("trigger_chat", "out", "print2", "in")
      .build();

    // Fire only trigger_init
    var listener = new LogRecordingEventListener();
    var context = new ReactiveScriptContext(listener);
    var engine = new ReactiveScriptEngine();
    engine.executeFromTriggerSync(graph, "trigger_init", context, Map.of()).block();

    assertNoErrors(listener);
    assertTrue(listener.logMessages.contains("init branch"),
      "Init trigger branch should execute");
    assertFalse(listener.logMessages.contains("chat branch"),
      "Chat trigger branch should NOT execute when only init is fired");
  }

  @Test
  void firingSecondTriggerActivatesItsBranch() {
    var graph = ScriptGraph.builder("test-trigger-chat", "Trigger Chat")
      .addNode("trigger_init", "trigger.on_script_init", null)
      .addNode("trigger_chat", "trigger.on_chat", null)
      .addNode("print1", "action.print", Map.of("message", "init branch"))
      .addNode("print2", "action.print", Map.of("message", "chat branch"))
      .addExecutionEdge("trigger_init", "out", "print1", "in")
      .addExecutionEdge("trigger_chat", "out", "print2", "in")
      .build();

    // Fire only trigger_chat
    var eventInputs = new HashMap<String, NodeValue>();
    eventInputs.put("messagePlainText", NodeValue.ofString("hello"));
    eventInputs.put("message", NodeValue.ofString("hello"));
    eventInputs.put("timestamp", NodeValue.ofNumber(System.currentTimeMillis()));

    var listener = new LogRecordingEventListener();
    var context = new ReactiveScriptContext(listener);
    var engine = new ReactiveScriptEngine();
    engine.executeFromTriggerSync(graph, "trigger_chat", context, eventInputs).block();

    assertNoErrors(listener);
    assertTrue(listener.completedNodes.contains("print2"),
      "Chat trigger branch print node should execute");
    assertFalse(listener.completedNodes.contains("print1"),
      "Init trigger branch print node should NOT execute when only chat is fired");
    // print2 logs the trigger's message from exec context (overrides default "chat branch")
    assertFalse(listener.logMessages.isEmpty(),
      "print2 should produce a log message");
  }

  @Test
  void triggersShareDataOnlyNodes() {
    // Two triggers can share the same data-only constant node.
    var graph = ScriptGraph.builder("test-shared-data", "Shared Data Node")
      .addNode("trigger_init", "trigger.on_script_init", null)
      .addNode("trigger_chat", "trigger.on_chat", null)
      .addNode("shared_const", "constant.string", Map.of("value", "shared value"))
      .addNode("print1", "action.print", null)
      .addNode("print2", "action.print", null)
      .addExecutionEdge("trigger_init", "out", "print1", "in")
      .addExecutionEdge("trigger_chat", "out", "print2", "in")
      .addDataEdge("shared_const", "value", "print1", "message")
      .addDataEdge("shared_const", "value", "print2", "message")
      .build();

    // Fire trigger_init
    var listener1 = new LogRecordingEventListener();
    var context1 = new ReactiveScriptContext(listener1);
    var engine = new ReactiveScriptEngine();
    engine.executeFromTriggerSync(graph, "trigger_init", context1, Map.of()).block();

    assertNoErrors(listener1);
    assertTrue(listener1.logMessages.contains("shared value"),
      "Init branch should receive shared constant");

    // Fire trigger_chat separately
    var eventInputs = new HashMap<String, NodeValue>();
    eventInputs.put("messagePlainText", NodeValue.ofString("hello"));
    eventInputs.put("message", NodeValue.ofString("hello"));
    eventInputs.put("timestamp", NodeValue.ofNumber(System.currentTimeMillis()));

    var listener2 = new LogRecordingEventListener();
    var context2 = new ReactiveScriptContext(listener2);
    engine.executeFromTriggerSync(graph, "trigger_chat", context2, eventInputs).block();

    assertNoErrors(listener2);
    assertTrue(listener2.logMessages.contains("shared value"),
      "Chat branch should also receive shared constant");
  }

  @Test
  void nonExistentTriggerDoesNothing() {
    var graph = ScriptGraph.builder("test-missing-trigger", "Missing Trigger")
      .addNode("trigger", "trigger.on_script_init", null)
      .addNode("print", "action.print", Map.of("message", "hello"))
      .addExecutionEdge("trigger", "out", "print", "in")
      .build();

    var listener = new RecordingEventListener();
    var context = new ReactiveScriptContext(listener);
    var engine = new ReactiveScriptEngine();
    engine.executeFromTriggerSync(graph, "nonexistent_trigger", context, Map.of()).block();

    assertTrue(listener.startedNodes.isEmpty(),
      "No nodes should start for nonexistent trigger");
  }

  @Test
  void concurrentTriggersDontCorruptState() throws Exception {
    // Graph with trigger → loop(3) → print
    var graph = ScriptGraph.builder("test-concurrent", "Concurrent Triggers")
      .addNode("trigger", "trigger.on_script_init", null)
      .addNode("loop", "flow.loop", Map.of("count", 3))
      .addNode("print", "action.print", Map.of("message", "iteration"))
      .addExecutionEdge("trigger", "out", "loop", "in")
      .addExecutionEdge("loop", "exec_loop", "print", "in")
      .build();

    var threads = new ArrayList<Thread>();
    var listeners = Collections.synchronizedList(new ArrayList<RecordingEventListener>());
    var errors = Collections.synchronizedList(new ArrayList<Throwable>());

    for (var i = 0; i < 4; i++) {
      var thread = new Thread(() -> {
        try {
          var listener = new RecordingEventListener();
          var context = new ReactiveScriptContext(listener);
          var engine = new ReactiveScriptEngine();
          engine.executeFromTriggerSync(graph, "trigger", context, Map.of()).block();
          listeners.add(listener);
        } catch (Throwable t) {
          errors.add(t);
        }
      });
      threads.add(thread);
    }

    for (var thread : threads) {
      thread.start();
    }
    for (var thread : threads) {
      thread.join(5000);
    }

    assertTrue(errors.isEmpty(), "No errors should occur: " + errors);
    assertEquals(4, listeners.size(), "All 4 executions should complete");
    for (var listener : listeners) {
      assertTrue(listener.errorNodes.isEmpty(),
        "No errors in execution: " + listener.errorNodes);
      assertEquals(3, listener.startedNodes.stream().filter("print"::equals).count(),
        "Each execution should run print 3 times");
    }
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
