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

import com.soulfiremc.server.script.NodeValue;
import com.soulfiremc.server.script.ScriptGraph;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static com.soulfiremc.test.ScriptTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

/// Integration tests for the reactive script engine: data edges, data-only nodes, trigger data edges.
final class ScriptEngineTest {

  @Test
  void dataEdgeDeliversValueAcrossNodes() {
    var graph = ScriptGraph.builder("test-data-edge", "Data Edge Test")
      .addNode("trigger", "trigger.on_script_init", null)
      .addNode("const_a", "constant.number", Map.of("value", 42))
      .addNode("const_b", "constant.number", Map.of("value", 8))
      .addNode("add", "math.add", null)
      .addNode("print", "action.print", null)
      .addExecutionEdge("trigger", "out", "const_a", "in")
      .addExecutionEdge("const_a", "out", "const_b", "in")
      .addExecutionEdge("const_b", "out", "add", "in")
      .addExecutionEdge("add", "out", "print", "in")
      .addDataEdge("const_a", "value", "add", "a")
      .addDataEdge("const_b", "value", "add", "b")
      .build();

    var dataEdges = graph.getIncomingDataEdges("add");
    assertEquals(2, dataEdges.size(), "AddNode should have 2 incoming data edges");
  }

  @Test
  void dataOnlyConstantNodeExecutedViaDataEdge() {
    var graph = ScriptGraph.builder("test-data-only", "Data Only Test")
      .addNode("trigger", "trigger.on_script_init", null)
      .addNode("const_a", "constant.number", Map.of("value", 42))
      .addNode("const_b", "constant.number", Map.of("value", 8))
      .addNode("add", "math.add", null)
      .addExecutionEdge("trigger", "out", "add", "in")
      .addDataEdge("const_a", "value", "add", "a")
      .addDataEdge("const_b", "value", "add", "b")
      .build();

    var listener = runGraph(graph, "trigger");

    assertNoErrors(listener);
    assertTrue(listener.completedNodes.contains("const_a"),
      "Data-only constant node A should be eagerly executed");
    assertTrue(listener.completedNodes.contains("const_b"),
      "Data-only constant node B should be eagerly executed");
    assertTrue(listener.completedNodes.contains("add"),
      "Math add node should complete");

    var addOutputs = listener.nodeOutputs.get("add");
    assertNotNull(addOutputs, "Add node should have outputs");
    assertEquals(50.0, addOutputs.get("result").asDouble(0.0), 0.001);
  }

  @Test
  void dataOnlyStringConstantExecutedViaDataEdge() {
    var graph = ScriptGraph.builder("test-string-const", "String Const Test")
      .addNode("trigger", "trigger.on_script_init", null)
      .addNode("const", "constant.string", Map.of("value", "hello"))
      .addNode("len", "string.length", null)
      .addExecutionEdge("trigger", "out", "len", "in")
      .addDataEdge("const", "value", "len", "text")
      .build();

    var listener = runGraph(graph, "trigger");

    assertNoErrors(listener);
    assertTrue(listener.completedNodes.contains("const"),
      "Data-only string constant should be eagerly executed");
    assertTrue(listener.completedNodes.contains("len"),
      "String length node should complete");

    var lenOutputs = listener.nodeOutputs.get("len");
    assertNotNull(lenOutputs, "String length node should have outputs");
    assertEquals(5, lenOutputs.get("length").asInt(0), "Length of 'hello' should be 5");
  }

  @Test
  void chainedDataOnlyNodesExecutedRecursively() {
    var graph = ScriptGraph.builder("test-chained-data", "Chained Data Test")
      .addNode("trigger", "trigger.on_script_init", null)
      .addNode("const", "constant.string", Map.of("value", "hello,world,test"))
      .addNode("split", "string.split", Map.of("delimiter", ","))
      .addNode("first", "list.first", null)
      .addNode("len", "string.length", null)
      .addExecutionEdge("trigger", "out", "len", "in")
      .addDataEdge("const", "value", "split", "text")
      .addDataEdge("split", "result", "first", "list")
      .addDataEdge("first", "item", "len", "text")
      .build();

    var listener = runGraph(graph, "trigger");

    assertNoErrors(listener);
    assertTrue(listener.completedNodes.contains("const"),
      "Data-only constant should be eagerly executed");
    assertTrue(listener.completedNodes.contains("split"),
      "Data-only split should be eagerly executed");
    assertTrue(listener.completedNodes.contains("first"),
      "Data-only list.first should be eagerly executed");
    assertTrue(listener.completedNodes.contains("len"),
      "String length node should complete");

    var lenOutputs = listener.nodeOutputs.get("len");
    assertNotNull(lenOutputs, "String length node should have outputs");
    assertEquals(5, lenOutputs.get("length").asInt(0), "Length of 'hello' should be 5");
  }

  @Test
  void dataOnlyNodeSharedByMultipleConsumers() {
    var graph = ScriptGraph.builder("test-shared-data", "Shared Data Test")
      .addNode("trigger", "trigger.on_script_init", null)
      .addNode("const", "constant.string", Map.of("value", "shared"))
      .addNode("print1", "action.print", null)
      .addNode("print2", "action.print", null)
      .addExecutionEdge("trigger", "out", "print1", "in")
      .addExecutionEdge("print1", "out", "print2", "in")
      .addDataEdge("const", "value", "print1", "message")
      .addDataEdge("const", "value", "print2", "message")
      .build();

    var listener = runGraph(graph, "trigger");

    assertNoErrors(listener);
    assertTrue(listener.completedNodes.contains("const"),
      "Data-only constant should be eagerly executed");
    assertTrue(listener.completedNodes.contains("print1"),
      "First print node should complete");
    assertTrue(listener.completedNodes.contains("print2"),
      "Second print node should complete (same constant consumed twice)");
  }

  @Test
  void triggerDataEdgeNotReExecuted() {
    var graph = ScriptGraph.builder("test-trigger-data-edge", "Trigger Data Edge Test")
      .addNode("trigger", "trigger.on_chat", null)
      .addNode("len", "string.length", null)
      .addExecutionEdge("trigger", "out", "len", "in")
      .addDataEdge("trigger", "messagePlainText", "len", "text")
      .build();

    var eventInputs = new HashMap<String, NodeValue>();
    eventInputs.put("messagePlainText", NodeValue.ofString("hello world"));
    eventInputs.put("message", NodeValue.ofString("hello world"));
    eventInputs.put("timestamp", NodeValue.ofNumber(System.currentTimeMillis()));

    var listener = runGraph(graph, "trigger", eventInputs);

    assertNoErrors(listener);
    assertTrue(listener.completedNodes.contains("trigger"),
      "Trigger node should complete");
    assertTrue(listener.completedNodes.contains("len"),
      "StringLength node should complete");

    var lenOutputs = listener.nodeOutputs.get("len");
    assertNotNull(lenOutputs, "StringLength node should have outputs");
    assertEquals(11, lenOutputs.get("length").asInt(0),
      "StringLength should see 'hello world' (length 11) from event inputs, not empty string from re-execution");

    var triggerStartCount = countNodeExecutions(listener, "trigger");
    assertEquals(1, triggerStartCount,
      "Trigger should be executed exactly once, not re-executed as a data-only node");
  }
}
