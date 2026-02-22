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

import com.soulfiremc.server.script.ScriptGraph;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/// Tests for {@link ScriptGraph} building, querying, and topological sorting.
final class ScriptGraphTest {

  @Test
  void scriptGraphBuilderCreatesGraph() {
    var graph = ScriptGraph.builder("test-id", "Test Script")
      .addNode("node1", "trigger.on_pre_entity_tick", null)
      .addNode("node2", "action.print", Map.of("message", "Hello"))
      .addExecutionEdge("node1", "out", "node2", "in")
      .build();

    assertNotNull(graph.getNode("node1"), "Node1 should exist in graph");
    assertNotNull(graph.getNode("node2"), "Node2 should exist in graph");
    var nextNodes = graph.getNextExecutionNodes("node1", "out");
    assertEquals(1, nextNodes.size(), "Should have 1 downstream node");
    assertTrue(nextNodes.contains("node2"), "Downstream node should be node2");
  }

  @Test
  void scriptGraphGetNode() {
    var graph = ScriptGraph.builder("test-id", "Test Script")
      .addNode("node1", "trigger.on_pre_entity_tick", null)
      .build();

    var node = graph.getNode("node1");
    assertNotNull(node, "Node should exist");
    assertEquals("node1", node.id(), "Node ID should match");
    assertEquals("trigger.on_pre_entity_tick", node.type(), "Node type should match");

    assertNull(graph.getNode("nonexistent"), "Nonexistent node should return null");
  }

  @Test
  void scriptGraphFindTriggerNodes() {
    var graph = ScriptGraph.builder("test-id", "Test Script")
      .addNode("trigger1", "trigger.on_pre_entity_tick", null)
      .addNode("trigger2", "trigger.on_join", null)
      .addNode("action1", "action.print", null)
      .addExecutionEdge("trigger1", "out", "action1", "in")
      .build();

    var triggers = graph.findTriggerNodes();

    assertEquals(2, triggers.size(), "Should find 2 trigger nodes");
    assertTrue(triggers.contains("trigger1"), "trigger1 should be a trigger");
    assertTrue(triggers.contains("trigger2"), "trigger2 should be a trigger");
    assertFalse(triggers.contains("action1"), "action1 should not be a trigger");
  }

  @Test
  void scriptGraphGetNextExecutionNodes() {
    var graph = ScriptGraph.builder("test-id", "Test Script")
      .addNode("trigger", "trigger.on_pre_entity_tick", null)
      .addNode("action1", "action.print", null)
      .addNode("action2", "action.print", null)
      .addExecutionEdge("trigger", "out", "action1", "in")
      .addExecutionEdge("trigger", "out", "action2", "in")
      .build();

    var nextNodes = graph.getNextExecutionNodes("trigger", "out");

    assertEquals(2, nextNodes.size(), "Should have 2 downstream nodes");
    assertTrue(nextNodes.contains("action1"), "action1 should be downstream");
    assertTrue(nextNodes.contains("action2"), "action2 should be downstream");
  }

  @Test
  void scriptGraphGetIncomingDataEdges() {
    var graph = ScriptGraph.builder("test-id", "Test Script")
      .addNode("const1", "constant.number", Map.of("value", 10))
      .addNode("const2", "constant.number", Map.of("value", 20))
      .addNode("add", "math.add", null)
      .addDataEdge("const1", "value", "add", "a")
      .addDataEdge("const2", "value", "add", "b")
      .build();

    var dataEdges = graph.getIncomingDataEdges("add");

    assertEquals(2, dataEdges.size(), "Should have 2 incoming data edges");
  }

  @Test
  void scriptGraphTopologicalSort() {
    var graph = ScriptGraph.builder("test-id", "Test Script")
      .addNode("trigger", "trigger.on_pre_entity_tick", null)
      .addNode("action1", "action.print", null)
      .addNode("action2", "action.print", null)
      .addExecutionEdge("trigger", "out", "action1", "in")
      .addExecutionEdge("action1", "out", "action2", "in")
      .build();

    var sorted = graph.topologicalSort();

    assertEquals(3, sorted.size(), "Should have 3 nodes in sorted order");
    assertTrue(sorted.indexOf("trigger") < sorted.indexOf("action1"),
      "trigger should come before action1");
    assertTrue(sorted.indexOf("action1") < sorted.indexOf("action2"),
      "action1 should come before action2");
  }

  @Test
  void scriptGraphTopologicalSortDetectsCycle() {
    var graph = ScriptGraph.builder("test-id", "Test Script")
      .addNode("node1", "action.print", null)
      .addNode("node2", "action.print", null)
      .addNode("node3", "action.print", null)
      .addExecutionEdge("node1", "out", "node2", "in")
      .addExecutionEdge("node2", "out", "node3", "in")
      .addExecutionEdge("node3", "out", "node1", "in")
      .build();

    assertThrows(IllegalStateException.class, graph::topologicalSort,
      "Cyclic graph should throw IllegalStateException");
  }

  @Test
  void hasIncomingExecutionEdgesReturnsTrueForExecTarget() {
    var graph = ScriptGraph.builder("test", "Test")
      .addNode("trigger", "trigger.on_script_init", null)
      .addNode("action", "action.print", null)
      .addExecutionEdge("trigger", "out", "action", "in")
      .build();

    assertTrue(graph.hasIncomingExecutionEdges("action"));
    assertFalse(graph.hasIncomingExecutionEdges("trigger"));
  }

  @Test
  void hasIncomingExecutionEdgesReturnsFalseForDataOnlyNode() {
    var graph = ScriptGraph.builder("test", "Test")
      .addNode("trigger", "trigger.on_script_init", null)
      .addNode("const", "constant.string", Map.of("value", "hello"))
      .addNode("action", "action.print", null)
      .addExecutionEdge("trigger", "out", "action", "in")
      .addDataEdge("const", "value", "action", "message")
      .build();

    assertFalse(graph.hasIncomingExecutionEdges("const"),
      "Data-only node should have no incoming execution edges");
    assertTrue(graph.hasIncomingExecutionEdges("action"));
  }
}
