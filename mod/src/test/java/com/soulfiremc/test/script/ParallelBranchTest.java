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

/// Tests for parallel branch execution and fan-in synchronization.
@Timeout(5)
final class ParallelBranchTest {

  @Test
  void triggerWithTwoParallelBranches() {
    // Trigger -> branch1_print AND branch2_print (both from "out")
    var graph = ScriptGraph.builder("test-parallel-branches", "Parallel Branches")
      .addNode("trigger", "trigger.on_script_init", null)
      .addNode("print1", "action.print", Map.of("message", "branch1"))
      .addNode("print2", "action.print", Map.of("message", "branch2"))
      .addExecutionEdge("trigger", "out", "print1", "in")
      .addExecutionEdge("trigger", "out", "print2", "in")
      .build();

    var listener = new LogRecordingEventListener();
    var context = new ReactiveScriptContext(listener);
    var engine = new ReactiveScriptEngine();
    engine.executeFromTriggerSync(graph, "trigger", context, Map.of()).block();

    assertNoErrors(listener);
    assertTrue(listener.completedNodes.contains("print1"),
      "Branch 1 should execute");
    assertTrue(listener.completedNodes.contains("print2"),
      "Branch 2 should execute");
    assertTrue(listener.logMessages.contains("branch1"), "Branch 1 should log");
    assertTrue(listener.logMessages.contains("branch2"), "Branch 2 should log");
  }

  @Test
  void branchNodeRoutesToTrueAndFalse() {
    // Trigger -> Branch(condition=true) -> true_print / false_print
    var graph = ScriptGraph.builder("test-branch-true", "Branch True Test")
      .addNode("trigger", "trigger.on_script_init", null)
      .addNode("branch", "flow.branch", Map.of("condition", true))
      .addNode("true_print", "action.print", Map.of("message", "true_path"))
      .addNode("false_print", "action.print", Map.of("message", "false_path"))
      .addExecutionEdge("trigger", "out", "branch", "in")
      .addExecutionEdge("branch", "exec_true", "true_print", "in")
      .addExecutionEdge("branch", "exec_false", "false_print", "in")
      .build();

    var listener = new LogRecordingEventListener();
    var context = new ReactiveScriptContext(listener);
    var engine = new ReactiveScriptEngine();
    engine.executeFromTriggerSync(graph, "trigger", context, Map.of()).block();

    assertNoErrors(listener);
    assertTrue(listener.completedNodes.contains("true_print"),
      "True branch should execute");
    assertFalse(listener.completedNodes.contains("false_print"),
      "False branch should NOT execute");
    assertTrue(listener.logMessages.contains("true_path"));
  }

  @Test
  void branchNodeRoutesToFalse() {
    var graph = ScriptGraph.builder("test-branch-false", "Branch False Test")
      .addNode("trigger", "trigger.on_script_init", null)
      .addNode("branch", "flow.branch", Map.of("condition", false))
      .addNode("true_print", "action.print", Map.of("message", "true_path"))
      .addNode("false_print", "action.print", Map.of("message", "false_path"))
      .addExecutionEdge("trigger", "out", "branch", "in")
      .addExecutionEdge("branch", "exec_true", "true_print", "in")
      .addExecutionEdge("branch", "exec_false", "false_print", "in")
      .build();

    var listener = new LogRecordingEventListener();
    var context = new ReactiveScriptContext(listener);
    var engine = new ReactiveScriptEngine();
    engine.executeFromTriggerSync(graph, "trigger", context, Map.of()).block();

    assertNoErrors(listener);
    assertFalse(listener.completedNodes.contains("true_print"),
      "True branch should NOT execute");
    assertTrue(listener.completedNodes.contains("false_print"),
      "False branch should execute");
    assertTrue(listener.logMessages.contains("false_path"));
  }

  @Test
  void sequenceNodeExecutesBranchesInOrder() {
    // Trigger -> Sequence(2) -> exec_0:print_a, exec_1:print_b
    var graph = ScriptGraph.builder("test-sequence", "Sequence Test")
      .addNode("trigger", "trigger.on_script_init", null)
      .addNode("seq", "flow.sequence", Map.of("branchCount", 2))
      .addNode("print_a", "action.print", Map.of("message", "first"))
      .addNode("print_b", "action.print", Map.of("message", "second"))
      .addExecutionEdge("trigger", "out", "seq", "in")
      .addExecutionEdge("seq", "exec_0", "print_a", "in")
      .addExecutionEdge("seq", "exec_1", "print_b", "in")
      .build();

    var listener = new LogRecordingEventListener();
    var context = new ReactiveScriptContext(listener);
    var engine = new ReactiveScriptEngine();
    engine.executeFromTriggerSync(graph, "trigger", context, Map.of()).block();

    assertNoErrors(listener);
    assertTrue(listener.completedNodes.contains("print_a"), "First branch should execute");
    assertTrue(listener.completedNodes.contains("print_b"), "Second branch should execute");
    assertEquals(List.of("first", "second"), listener.logMessages,
      "Sequence should execute branches in order");
  }

  @Test
  void fanInDataEdgesFromMultipleDataOnlyNodes() {
    // Two data-only constant nodes feed into a single add node.
    // The add node should wait for both before executing.
    var graph = ScriptGraph.builder("test-fan-in", "Fan-In Test")
      .addNode("trigger", "trigger.on_script_init", null)
      .addNode("const_a", "constant.number", Map.of("value", 10))
      .addNode("const_b", "constant.number", Map.of("value", 20))
      .addNode("add", "math.add", null)
      .addNode("print", "action.print", null)
      .addExecutionEdge("trigger", "out", "print", "in")
      .addDataEdge("const_a", "value", "add", "a")
      .addDataEdge("const_b", "value", "add", "b")
      .addDataEdge("add", "result", "print", "message")
      .build();

    var listener = new LogRecordingEventListener();
    var context = new ReactiveScriptContext(listener);
    var engine = new ReactiveScriptEngine();
    engine.executeFromTriggerSync(graph, "trigger", context, Map.of()).block();

    assertNoErrors(listener);
    assertTrue(listener.completedNodes.contains("const_a"), "Const A should execute");
    assertTrue(listener.completedNodes.contains("const_b"), "Const B should execute");
    assertTrue(listener.completedNodes.contains("add"), "Add should execute");
    var addOutputs = listener.nodeOutputs.get("add");
    assertEquals(30.0, addOutputs.get("result").asDouble(0), 0.001,
      "10 + 20 should = 30");
  }

  @Test
  void parallelBranchesWithIndependentDataNodes() {
    // Two parallel branches each consuming their own data-only constant.
    var graph = ScriptGraph.builder("test-parallel-data", "Parallel Data Test")
      .addNode("trigger", "trigger.on_script_init", null)
      .addNode("const_a", "constant.string", Map.of("value", "hello"))
      .addNode("const_b", "constant.string", Map.of("value", "world"))
      .addNode("print_a", "action.print", null)
      .addNode("print_b", "action.print", null)
      .addExecutionEdge("trigger", "out", "print_a", "in")
      .addExecutionEdge("trigger", "out", "print_b", "in")
      .addDataEdge("const_a", "value", "print_a", "message")
      .addDataEdge("const_b", "value", "print_b", "message")
      .build();

    var listener = new LogRecordingEventListener();
    var context = new ReactiveScriptContext(listener);
    var engine = new ReactiveScriptEngine();
    engine.executeFromTriggerSync(graph, "trigger", context, Map.of()).block();

    assertNoErrors(listener);
    assertTrue(listener.logMessages.contains("hello"), "Branch A should print 'hello'");
    assertTrue(listener.logMessages.contains("world"), "Branch B should print 'world'");
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
