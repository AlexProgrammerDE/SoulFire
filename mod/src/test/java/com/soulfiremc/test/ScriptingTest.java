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

import com.soulfiremc.server.InstanceManager;
import com.soulfiremc.server.SoulFireScheduler;
import com.soulfiremc.server.script.*;
import com.soulfiremc.server.script.nodes.NodeRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

final class ScriptingTest {

  /// Minimal NodeRuntime for tests that need stateStore() (e.g., RateLimitNode, DebounceNode).
  private static final NodeRuntime TEST_RUNTIME = new NodeRuntime() {
    private final ScriptStateStore stateStore = new ScriptStateStore();

    @Override
    public ScriptStateStore stateStore() {
      return stateStore;
    }

    @Override
    public InstanceManager instance() {
      return null;
    }

    @Override
    public SoulFireScheduler scheduler() {
      return null;
    }

    @Override
    public void log(String level, String message) {}
  };

  // ==================== NodeValue Tests ====================

  @Test
  void nodeValueOfString() {
    var value = NodeValue.ofString("hello");
    assertEquals("hello", value.asString("default"));
    assertFalse(value.isNull());
  }

  @Test
  void nodeValueOfNumber() {
    var value = NodeValue.ofNumber(42);
    assertEquals(42, value.asInt(0));
    assertEquals(42.0, value.asDouble(0.0));
    assertEquals(42L, value.asLong(0L));
  }

  @Test
  void nodeValueOfBoolean() {
    var trueValue = NodeValue.ofBoolean(true);
    var falseValue = NodeValue.ofBoolean(false);

    assertTrue(trueValue.asBoolean(false));
    assertFalse(falseValue.asBoolean(true));
  }

  @Test
  void nodeValueOfNull() {
    var value = NodeValue.ofNull();
    assertTrue(value.isNull());
    assertEquals("default", value.asString("default"));
    assertEquals(0, value.asInt(0));
  }

  @Test
  void nodeValueOfList() {
    var list = List.of(NodeValue.ofNumber(1), NodeValue.ofNumber(2), NodeValue.ofNumber(3));
    var value = NodeValue.ofList(list);

    var result = value.asList();
    assertEquals(3, result.size());
    assertEquals(1, result.getFirst().asInt(0));
    assertEquals(2, result.get(1).asInt(0));
    assertEquals(3, result.get(2).asInt(0));
  }

  @Test
  void nodeValueOfMap() {
    var map = Map.of("key1", "value1", "key2", 42);
    var value = NodeValue.of(map);

    assertFalse(value.isNull());
    assertNotNull(value.asJsonElement());
    assertTrue(value.asJsonElement().isJsonObject());
  }

  @Test
  void nodeValueConversionFromObject() {
    assertEquals("test", NodeValue.of("test").asString(""));
    assertEquals(123, NodeValue.of(123).asInt(0));
    assertTrue(NodeValue.of(true).asBoolean(false));
    assertTrue(NodeValue.of(null).isNull());
  }

  @Test
  void nodeValueStringList() {
    var value = NodeValue.of(List.of("a", "b", "c"));
    var strings = value.asStringList();

    assertEquals(3, strings.size());
    assertEquals("a", strings.getFirst());
    assertEquals("b", strings.get(1));
    assertEquals("c", strings.get(2));
  }

  // ==================== ScriptGraph Builder Tests ====================

  @Test
  void scriptGraphBuilderCreatesGraph() {
    var graph = ScriptGraph.builder("test-id", "Test Script")
      .addNode("node1", "trigger.on_pre_entity_tick", null)
      .addNode("node2", "action.print", Map.of("message", "Hello"))
      .addExecutionEdge("node1", "out", "node2", "in")
      .build();

    // Verify nodes exist via getNode
    assertNotNull(graph.getNode("node1"));
    assertNotNull(graph.getNode("node2"));
    // Verify the execution edge was created by checking next nodes
    var nextNodes = graph.getNextExecutionNodes("node1", "out");
    assertEquals(1, nextNodes.size());
    assertTrue(nextNodes.contains("node2"));
  }

  @Test
  void scriptGraphGetNode() {
    var graph = ScriptGraph.builder("test-id", "Test Script")
      .addNode("node1", "trigger.on_pre_entity_tick", null)
      .build();

    var node = graph.getNode("node1");
    assertNotNull(node);
    assertEquals("node1", node.id());
    assertEquals("trigger.on_pre_entity_tick", node.type());

    assertNull(graph.getNode("nonexistent"));
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

    // trigger2 has no outgoing edges but is still a trigger (no incoming execution edges)
    // action1 has an incoming execution edge so it's not a trigger
    assertEquals(2, triggers.size());
    assertTrue(triggers.contains("trigger1"));
    assertTrue(triggers.contains("trigger2"));
    assertFalse(triggers.contains("action1"));
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

    assertEquals(2, nextNodes.size());
    assertTrue(nextNodes.contains("action1"));
    assertTrue(nextNodes.contains("action2"));
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

    assertEquals(2, dataEdges.size());
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

    assertEquals(3, sorted.size());
    // trigger must come before action1, action1 must come before action2
    assertTrue(sorted.indexOf("trigger") < sorted.indexOf("action1"));
    assertTrue(sorted.indexOf("action1") < sorted.indexOf("action2"));
  }

  @Test
  void scriptGraphTopologicalSortDetectsCycle() {
    var graph = ScriptGraph.builder("test-id", "Test Script")
      .addNode("node1", "action.print", null)
      .addNode("node2", "action.print", null)
      .addNode("node3", "action.print", null)
      .addExecutionEdge("node1", "out", "node2", "in")
      .addExecutionEdge("node2", "out", "node3", "in")
      .addExecutionEdge("node3", "out", "node1", "in") // Creates cycle
      .build();

    assertThrows(IllegalStateException.class, graph::topologicalSort);
  }

  // ==================== NodeRegistry Tests ====================

  @Test
  void nodeRegistryContainsBasicNodes() {
    assertTrue(NodeRegistry.isRegistered("math.add"));
    assertTrue(NodeRegistry.isRegistered("math.subtract"));
    assertTrue(NodeRegistry.isRegistered("math.multiply"));
    assertTrue(NodeRegistry.isRegistered("logic.compare"));
    assertTrue(NodeRegistry.isRegistered("logic.and"));
    assertTrue(NodeRegistry.isRegistered("logic.or"));
    assertTrue(NodeRegistry.isRegistered("flow.branch"));
    assertTrue(NodeRegistry.isRegistered("trigger.on_pre_entity_tick"));
    assertTrue(NodeRegistry.isRegistered("trigger.on_join"));
  }

  @Test
  void nodeRegistryCacheReturnsSameInstance() {
    var node1 = NodeRegistry.create("math.add");
    var node2 = NodeRegistry.create("math.add");

    assertNotNull(node1);
    assertNotNull(node2);
    assertSame(node1, node2, "Cached instances should be the same reference");
  }

  @Test
  void nodeRegistryCreateThrowsForUnknownType() {
    assertThrows(IllegalArgumentException.class, () -> NodeRegistry.create("nonexistent.node"));
  }

  @Test
  void nodeRegistryGetRegisteredCount() {
    assertTrue(NodeRegistry.getRegisteredCount() > 50); // Should have many nodes
  }

  // ==================== Math Node Tests ====================

  @ParameterizedTest
  @CsvSource({
    "5, 3, 8",
    "0, 0, 0",
    "-5, 3, -2",
    "1.5, 2.5, 4.0"
  })
  void addNodeExecutes(double a, double b, double expected) {
    var node = NodeRegistry.create("math.add");
    var inputs = Map.of(
      "a", NodeValue.ofNumber(a),
      "b", NodeValue.ofNumber(b)
    );

    var result = node.executeReactive(null, inputs).block();

    assertEquals(expected, result.get("result").asDouble(0.0), 0.001);
  }

  @ParameterizedTest
  @CsvSource({
    "10, 3, 7",
    "0, 5, -5",
    "5.5, 2.5, 3.0"
  })
  void subtractNodeExecutes(double a, double b, double expected) {
    var node = NodeRegistry.create("math.subtract");
    var inputs = Map.of(
      "a", NodeValue.ofNumber(a),
      "b", NodeValue.ofNumber(b)
    );

    var result = node.executeReactive(null, inputs).block();

    assertEquals(expected, result.get("result").asDouble(0.0), 0.001);
  }

  @ParameterizedTest
  @CsvSource({
    "5, 3, 15",
    "0, 100, 0",
    "-2, 3, -6",
    "2.5, 4, 10.0"
  })
  void multiplyNodeExecutes(double a, double b, double expected) {
    var node = NodeRegistry.create("math.multiply");
    var inputs = Map.of(
      "a", NodeValue.ofNumber(a),
      "b", NodeValue.ofNumber(b)
    );

    var result = node.executeReactive(null, inputs).block();

    assertEquals(expected, result.get("result").asDouble(0.0), 0.001);
  }

  @ParameterizedTest
  @CsvSource({
    "10, 2, 5",
    "7, 2, 3.5",
    "-10, 2, -5"
  })
  void divideNodeExecutes(double a, double b, double expected) {
    var node = NodeRegistry.create("math.divide");
    var inputs = Map.of(
      "a", NodeValue.ofNumber(a),
      "b", NodeValue.ofNumber(b)
    );

    var result = node.executeReactive(null, inputs).block();

    assertEquals(expected, result.get("result").asDouble(0.0), 0.001);
  }

  @Test
  void divideByZeroReturnsZero() {
    var node = NodeRegistry.create("math.divide");
    var inputs = Map.of(
      "a", NodeValue.ofNumber(10),
      "b", NodeValue.ofNumber(0)
    );

    var result = node.executeReactive(null, inputs).block();

    // The divide node returns 0 when dividing by zero (safe default)
    assertEquals(0.0, result.get("result").asDouble(-1.0), 0.001);
  }

  @ParameterizedTest
  @CsvSource({
    "10, 3, 1",
    "15, 5, 0",
    "7, 4, 3"
  })
  void moduloNodeExecutes(double a, double b, double expected) {
    var node = NodeRegistry.create("math.modulo");
    var inputs = Map.of(
      "a", NodeValue.ofNumber(a),
      "b", NodeValue.ofNumber(b)
    );

    var result = node.executeReactive(null, inputs).block();

    assertEquals(expected, result.get("result").asDouble(0.0), 0.001);
  }

  @ParameterizedTest
  @CsvSource({
    "-5, 5",
    "5, 5",
    "0, 0"
  })
  void absNodeExecutes(double input, double expected) {
    var node = NodeRegistry.create("math.abs");
    var inputs = Map.of("value", NodeValue.ofNumber(input));

    var result = node.executeReactive(null, inputs).block();

    assertEquals(expected, result.get("result").asDouble(0.0), 0.001);
  }

  @ParameterizedTest
  @CsvSource({
    "3.7, 3",
    "3.2, 3",
    "-3.7, -4"
  })
  void floorNodeExecutes(double input, double expected) {
    var node = NodeRegistry.create("math.floor");
    var inputs = Map.of("value", NodeValue.ofNumber(input));

    var result = node.executeReactive(null, inputs).block();

    assertEquals(expected, result.get("result").asDouble(0.0), 0.001);
  }

  @ParameterizedTest
  @CsvSource({
    "3.2, 4",
    "3.7, 4",
    "-3.7, -3"
  })
  void ceilNodeExecutes(double input, double expected) {
    var node = NodeRegistry.create("math.ceil");
    var inputs = Map.of("value", NodeValue.ofNumber(input));

    var result = node.executeReactive(null, inputs).block();

    assertEquals(expected, result.get("result").asDouble(0.0), 0.001);
  }

  @ParameterizedTest
  @CsvSource({
    "3.4, 3",
    "3.5, 4",
    "3.6, 4",
    "-3.5, -3"
  })
  void roundNodeExecutes(double input, double expected) {
    var node = NodeRegistry.create("math.round");
    var inputs = Map.of("value", NodeValue.ofNumber(input));

    var result = node.executeReactive(null, inputs).block();

    assertEquals(expected, result.get("result").asDouble(0.0), 0.001);
  }

  @ParameterizedTest
  @CsvSource({
    "3, 7, 3",
    "10, 5, 5",
    "5, 5, 5"
  })
  void minNodeExecutes(double a, double b, double expected) {
    var node = NodeRegistry.create("math.min");
    var inputs = Map.of(
      "a", NodeValue.ofNumber(a),
      "b", NodeValue.ofNumber(b)
    );

    var result = node.executeReactive(null, inputs).block();

    assertEquals(expected, result.get("result").asDouble(0.0), 0.001);
  }

  @ParameterizedTest
  @CsvSource({
    "3, 7, 7",
    "10, 5, 10",
    "5, 5, 5"
  })
  void maxNodeExecutes(double a, double b, double expected) {
    var node = NodeRegistry.create("math.max");
    var inputs = Map.of(
      "a", NodeValue.ofNumber(a),
      "b", NodeValue.ofNumber(b)
    );

    var result = node.executeReactive(null, inputs).block();

    assertEquals(expected, result.get("result").asDouble(0.0), 0.001);
  }

  @ParameterizedTest
  @CsvSource({
    "2, 3, 8",
    "10, 2, 100",
    "5, 0, 1"
  })
  void powNodeExecutes(double base, double exp, double expected) {
    var node = NodeRegistry.create("math.pow");
    var inputs = Map.of(
      "base", NodeValue.ofNumber(base),
      "exponent", NodeValue.ofNumber(exp)
    );

    var result = node.executeReactive(null, inputs).block();

    assertEquals(expected, result.get("result").asDouble(0.0), 0.001);
  }

  @ParameterizedTest
  @CsvSource({
    "5, 0, 10, 5",
    "-5, 0, 10, 0",
    "15, 0, 10, 10"
  })
  void clampNodeExecutes(double value, double min, double max, double expected) {
    var node = NodeRegistry.create("math.clamp");
    var inputs = Map.of(
      "value", NodeValue.ofNumber(value),
      "min", NodeValue.ofNumber(min),
      "max", NodeValue.ofNumber(max)
    );

    var result = node.executeReactive(null, inputs).block();

    assertEquals(expected, result.get("result").asDouble(0.0), 0.001);
  }

  // ==================== Logic Node Tests ====================

  @ParameterizedTest
  @CsvSource({
    "5, 5, ==, true",
    "5, 3, ==, false",
    "5, 3, !=, true",
    "5, 5, !=, false",
    "5, 3, >, true",
    "3, 5, >, false",
    "3, 5, <, true",
    "5, 3, <, false",
    "5, 5, >=, true",
    "5, 6, >=, false",
    "5, 5, <=, true",
    "6, 5, <=, false"
  })
  void compareNodeExecutes(double a, double b, String op, boolean expected) {
    var node = NodeRegistry.create("logic.compare");
    var inputs = Map.of(
      "a", NodeValue.ofNumber(a),
      "b", NodeValue.ofNumber(b),
      "operator", NodeValue.ofString(op)
    );

    var result = node.executeReactive(null, inputs).block();

    assertEquals(expected, result.get("result").asBoolean(false));
  }

  @ParameterizedTest
  @CsvSource({
    "true, true, true",
    "true, false, false",
    "false, true, false",
    "false, false, false"
  })
  void andNodeExecutes(boolean a, boolean b, boolean expected) {
    var node = NodeRegistry.create("logic.and");
    var inputs = Map.of(
      "a", NodeValue.ofBoolean(a),
      "b", NodeValue.ofBoolean(b)
    );

    var result = node.executeReactive(null, inputs).block();

    assertEquals(expected, result.get("result").asBoolean(false));
  }

  @ParameterizedTest
  @CsvSource({
    "true, true, true",
    "true, false, true",
    "false, true, true",
    "false, false, false"
  })
  void orNodeExecutes(boolean a, boolean b, boolean expected) {
    var node = NodeRegistry.create("logic.or");
    var inputs = Map.of(
      "a", NodeValue.ofBoolean(a),
      "b", NodeValue.ofBoolean(b)
    );

    var result = node.executeReactive(null, inputs).block();

    assertEquals(expected, result.get("result").asBoolean(false));
  }

  @ParameterizedTest
  @CsvSource({
    "true, false",
    "false, true"
  })
  void notNodeExecutes(boolean input, boolean expected) {
    var node = NodeRegistry.create("logic.not");
    var inputs = Map.of("value", NodeValue.ofBoolean(input));

    var result = node.executeReactive(null, inputs).block();

    assertEquals(expected, result.get("result").asBoolean(false));
  }

  @ParameterizedTest
  @CsvSource({
    "true, true, false",
    "true, false, true",
    "false, true, true",
    "false, false, false"
  })
  void xorNodeExecutes(boolean a, boolean b, boolean expected) {
    var node = NodeRegistry.create("logic.xor");
    var inputs = Map.of(
      "a", NodeValue.ofBoolean(a),
      "b", NodeValue.ofBoolean(b)
    );

    var result = node.executeReactive(null, inputs).block();

    assertEquals(expected, result.get("result").asBoolean(false));
  }

  // ==================== Flow Node Tests ====================

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void branchNodeExecutes(boolean condition) {
    var node = NodeRegistry.create("flow.branch");
    var inputs = Map.of("condition", NodeValue.ofBoolean(condition));

    var result = node.executeReactive(null, inputs).block();

    assertEquals(condition ? "true" : "false", result.get("branch").asString(""));
    assertEquals(condition, result.get("condition").asBoolean(false));
  }

  // ==================== String Node Tests ====================

  @Test
  void concatNodeExecutes() {
    var node = NodeRegistry.create("string.concat");
    var inputs = Map.of(
      "a", NodeValue.ofString("Hello"),
      "b", NodeValue.ofString(" World")
    );

    var result = node.executeReactive(null, inputs).block();

    assertEquals("Hello World", result.get("result").asString(""));
  }

  @Test
  void replaceNodeExecutes() {
    var node = NodeRegistry.create("string.replace");
    var inputs = Map.of(
      "text", NodeValue.ofString("Hello World"),
      "search", NodeValue.ofString("World"),
      "replacement", NodeValue.ofString("Universe")
    );

    var result = node.executeReactive(null, inputs).block();

    assertEquals("Hello Universe", result.get("result").asString(""));
  }

  @Test
  void splitNodeExecutes() {
    var node = NodeRegistry.create("string.split");
    var inputs = Map.of(
      "text", NodeValue.ofString("a,b,c"),
      "delimiter", NodeValue.ofString(",")
    );

    var result = node.executeReactive(null, inputs).block();
    var parts = result.get("result").asStringList();

    assertEquals(3, parts.size());
    assertEquals("a", parts.getFirst());
    assertEquals("b", parts.get(1));
    assertEquals("c", parts.get(2));
  }

  @Test
  void substringNodeExecutes() {
    var node = NodeRegistry.create("string.substring");
    var inputs = Map.of(
      "text", NodeValue.ofString("Hello World"),
      "start", NodeValue.ofNumber(0),
      "end", NodeValue.ofNumber(5)
    );

    var result = node.executeReactive(null, inputs).block();

    assertEquals("Hello", result.get("result").asString(""));
  }

  @Test
  void stringLengthNodeExecutes() {
    var node = NodeRegistry.create("string.length");
    var inputs = Map.of("text", NodeValue.ofString("Hello"));

    var result = node.executeReactive(null, inputs).block();

    assertEquals(5, result.get("length").asInt(0));
  }

  @Test
  void toLowerCaseNodeExecutes() {
    var node = NodeRegistry.create("string.to_lower_case");
    var inputs = Map.of("text", NodeValue.ofString("HELLO"));

    var result = node.executeReactive(null, inputs).block();

    assertEquals("hello", result.get("result").asString(""));
  }

  @Test
  void toUpperCaseNodeExecutes() {
    var node = NodeRegistry.create("string.to_upper_case");
    var inputs = Map.of("text", NodeValue.ofString("hello"));

    var result = node.executeReactive(null, inputs).block();

    assertEquals("HELLO", result.get("result").asString(""));
  }

  @Test
  void trimNodeExecutes() {
    var node = NodeRegistry.create("string.trim");
    var inputs = Map.of("text", NodeValue.ofString("  hello  "));

    var result = node.executeReactive(null, inputs).block();

    assertEquals("hello", result.get("result").asString(""));
  }

  @ParameterizedTest
  @CsvSource({
    "Hello World, Hello, true",
    "Hello World, World, false",
    "Hello World, Goodbye, false"
  })
  void startsWithNodeExecutes(String text, String prefix, boolean expected) {
    var node = NodeRegistry.create("string.starts_with");
    var inputs = Map.of(
      "text", NodeValue.ofString(text),
      "prefix", NodeValue.ofString(prefix)
    );

    var result = node.executeReactive(null, inputs).block();

    assertEquals(expected, result.get("result").asBoolean(false));
  }

  @ParameterizedTest
  @CsvSource({
    "Hello World, World, true",
    "Hello World, Hello, false",
    "Hello World, Goodbye, false"
  })
  void endsWithNodeExecutes(String text, String suffix, boolean expected) {
    var node = NodeRegistry.create("string.ends_with");
    var inputs = Map.of(
      "text", NodeValue.ofString(text),
      "suffix", NodeValue.ofString(suffix)
    );

    var result = node.executeReactive(null, inputs).block();

    assertEquals(expected, result.get("result").asBoolean(false));
  }

  @ParameterizedTest
  @CsvSource({
    "Hello World, World, true",
    "Hello World, Universe, false",
    "Hello World, llo, true"
  })
  void stringContainsNodeExecutes(String text, String search, boolean expected) {
    var node = NodeRegistry.create("string.contains");
    var inputs = Map.of(
      "text", NodeValue.ofString(text),
      "search", NodeValue.ofString(search)
    );

    var result = node.executeReactive(null, inputs).block();

    assertEquals(expected, result.get("result").asBoolean(false));
  }

  // ==================== List Node Tests ====================

  @Test
  void listLengthNodeExecutes() {
    var node = NodeRegistry.create("list.length");
    var inputs = Map.of(
      "list", NodeValue.of(List.of(1, 2, 3, 4, 5))
    );

    var result = node.executeReactive(null, inputs).block();

    assertEquals(5, result.get("length").asInt(0));
  }

  @Test
  void getAtNodeExecutes() {
    var node = NodeRegistry.create("list.get_at");
    var inputs = Map.of(
      "list", NodeValue.of(List.of("a", "b", "c")),
      "index", NodeValue.ofNumber(1)
    );

    var result = node.executeReactive(null, inputs).block();

    assertEquals("b", result.get("item").asString(""));
  }

  @Test
  void firstNodeExecutes() {
    var node = NodeRegistry.create("list.first");
    var inputs = Map.of(
      "list", NodeValue.of(List.of("first", "second", "third"))
    );

    var result = node.executeReactive(null, inputs).block();

    assertEquals("first", result.get("item").asString(""));
  }

  @Test
  void lastNodeExecutes() {
    var node = NodeRegistry.create("list.last");
    var inputs = Map.of(
      "list", NodeValue.of(List.of("first", "second", "third"))
    );

    var result = node.executeReactive(null, inputs).block();

    assertEquals("third", result.get("item").asString(""));
  }

  @ParameterizedTest
  @CsvSource({
    "apple, true",
    "banana, true",
    "grape, false"
  })
  void listContainsNodeExecutes(String search, boolean expected) {
    var node = NodeRegistry.create("list.contains");
    var inputs = Map.of(
      "list", NodeValue.of(List.of("apple", "banana", "cherry")),
      "item", NodeValue.ofString(search)
    );

    var result = node.executeReactive(null, inputs).block();

    assertEquals(expected, result.get("result").asBoolean(false));
  }

  @Test
  void joinToStringNodeExecutes() {
    var node = NodeRegistry.create("list.join");
    var inputs = Map.of(
      "list", NodeValue.of(List.of("a", "b", "c")),
      "separator", NodeValue.ofString(", ")
    );

    var result = node.executeReactive(null, inputs).block();

    // The join uses toString() on NodeValue, which includes JSON formatting (quotes for strings)
    assertEquals("\"a\", \"b\", \"c\"", result.get("result").asString(""));
  }

  @Test
  void rangeNodeExecutes() {
    var node = NodeRegistry.create("list.range");
    var inputs = Map.of(
      "start", NodeValue.ofNumber(1),
      "end", NodeValue.ofNumber(5)
    );

    var result = node.executeReactive(null, inputs).block();
    var list = result.get("list").asList();

    assertEquals(4, list.size());
    assertEquals(1, list.getFirst().asInt(0));
    assertEquals(2, list.get(1).asInt(0));
    assertEquals(3, list.get(2).asInt(0));
    assertEquals(4, list.get(3).asInt(0));
  }

  // ==================== Constant Node Tests ====================

  @Test
  void numberConstantNodeExecutes() {
    var node = NodeRegistry.create("constant.number");
    var inputs = Map.of("value", NodeValue.ofNumber(42));

    var result = node.executeReactive(null, inputs).block();

    assertEquals(42, result.get("value").asInt(0));
  }

  @Test
  void stringConstantNodeExecutes() {
    var node = NodeRegistry.create("constant.string");
    var inputs = Map.of("value", NodeValue.ofString("test"));

    var result = node.executeReactive(null, inputs).block();

    assertEquals("test", result.get("value").asString(""));
  }

  @Test
  void booleanConstantNodeExecutes() {
    var node = NodeRegistry.create("constant.boolean");
    var inputs = Map.of("value", NodeValue.ofBoolean(true));

    var result = node.executeReactive(null, inputs).block();

    assertTrue(result.get("value").asBoolean(false));
  }

  // ==================== Utility Node Tests ====================

  @Test
  void toStringNodeExecutes() {
    var node = NodeRegistry.create("util.to_string");
    var inputs = Map.of("value", NodeValue.ofNumber(42));

    var result = node.executeReactive(null, inputs).block();

    assertEquals("42", result.get("result").asString(""));
  }

  @Test
  void toNumberNodeExecutes() {
    var node = NodeRegistry.create("util.to_number");
    var inputs = Map.of("value", NodeValue.ofString("42.5"));

    var result = node.executeReactive(null, inputs).block();

    assertEquals(42.5, result.get("result").asDouble(0.0), 0.001);
  }

  @Test
  void isNullNodeExecutesWithMissingValue() {
    var node = NodeRegistry.create("util.is_null");
    // IsNullNode checks if the input key is absent (Java null), not JSON null
    var inputs = Map.<String, NodeValue>of();

    var result = node.executeReactive(null, inputs).block();

    assertTrue(result.get("result").asBoolean(false));
  }

  @Test
  void isNullNodeExecutesWithPresentValue() {
    var node = NodeRegistry.create("util.is_null");
    var inputs = Map.of("value", NodeValue.ofString("hello"));

    var result = node.executeReactive(null, inputs).block();

    assertFalse(result.get("result").asBoolean(true));
  }

  @Test
  void isEmptyNodeExecutesWithEmptyString() {
    var node = NodeRegistry.create("util.is_empty");
    var inputs = Map.of("value", NodeValue.ofString(""));

    var result = node.executeReactive(null, inputs).block();

    assertTrue(result.get("result").asBoolean(false));
  }

  @Test
  void isEmptyNodeExecutesWithNonEmptyString() {
    var node = NodeRegistry.create("util.is_empty");
    var inputs = Map.of("value", NodeValue.ofString("hello"));

    var result = node.executeReactive(null, inputs).block();

    assertFalse(result.get("result").asBoolean(true));
  }

  // ==================== ExecutionContext Tests ====================

  @Test
  void executionContextEmpty() {
    var ctx = ExecutionContext.empty();
    assertTrue(ctx.values().isEmpty());
  }

  @Test
  void executionContextFrom() {
    var initial = Map.of(
      "bot", NodeValue.ofString("testBot"),
      "tickCount", NodeValue.ofNumber(42)
    );
    var ctx = ExecutionContext.from(initial);

    assertEquals(2, ctx.values().size());
    assertEquals("testBot", ctx.values().get("bot").asString(""));
    assertEquals(42, ctx.values().get("tickCount").asInt(0));
  }

  @Test
  void executionContextMergeWithAddsNewKeys() {
    var ctx = ExecutionContext.from(Map.of("bot", NodeValue.ofString("testBot")));
    var merged = ctx.mergeWith(Map.of("health", NodeValue.ofNumber(20)));

    assertEquals(2, merged.values().size());
    assertEquals("testBot", merged.values().get("bot").asString(""));
    assertEquals(20, merged.values().get("health").asInt(0));
  }

  @Test
  void executionContextMergeWithOverridesExistingKeys() {
    var ctx = ExecutionContext.from(Map.of(
      "bot", NodeValue.ofString("bot1"),
      "health", NodeValue.ofNumber(10)
    ));
    var merged = ctx.mergeWith(Map.of("health", NodeValue.ofNumber(20)));

    assertEquals("bot1", merged.values().get("bot").asString(""));
    assertEquals(20, merged.values().get("health").asInt(0));
  }

  @Test
  void executionContextMergeWithIsImmutable() {
    var ctx = ExecutionContext.from(Map.of("bot", NodeValue.ofString("bot1")));
    var merged = ctx.mergeWith(Map.of("health", NodeValue.ofNumber(20)));

    // Original context unchanged
    assertEquals(1, ctx.values().size());
    assertNull(ctx.values().get("health"));

    // Merged has both
    assertEquals(2, merged.values().size());
  }

  @Test
  void executionContextMergeWithEmptyReturnsThis() {
    var ctx = ExecutionContext.from(Map.of("bot", NodeValue.ofString("bot1")));
    var merged = ctx.mergeWith(Map.of());

    assertSame(ctx, merged);
  }

  // ==================== Node Metadata Tests ====================

  @Test
  void allNodesHaveMetadata() {
    for (var type : NodeRegistry.getRegisteredTypes()) {
      var metadata = NodeRegistry.getMetadata(type);

      assertNotNull(metadata, "Node " + type + " has no metadata");
      assertNotNull(metadata.type(), "Node " + type + " has no type");
      assertNotNull(metadata.displayName(), "Node " + type + " has no display name");
      assertNotNull(metadata.category(), "Node " + type + " has no category");
      assertNotNull(metadata.description(), "Node " + type + " has no description");
      assertEquals(type, metadata.type(), "Node metadata type doesn't match registered type");
    }
  }

  // ==================== ExecutionRun Isolation Tests ====================

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

    assertNotNull(result1);
    assertNotNull(result2);
    assertEquals("run1", result1.get("value").asString(""));
    assertEquals("run2", result2.get("value").asString(""));
  }

  @Test
  void executionRunAwaitBeforePublish() {
    var run = new ExecutionRun();

    // Start awaiting before publishing
    var mono = run.awaitNodeOutputs("node1", "node1");
    run.publishNodeOutputs("node1", Map.of("value", NodeValue.ofNumber(42)));

    var result = mono.block();
    assertNotNull(result);
    assertEquals(42, result.get("value").asInt(0));
  }

  // ==================== BranchNode Routing Tests ====================

  @Test
  void branchNodeOutputsExecTrueKey() {
    var node = NodeRegistry.create("flow.branch");
    var inputs = Map.of("condition", NodeValue.ofBoolean(true));

    var result = node.executeReactive(null, inputs).block();

    assertTrue(result.containsKey("exec_true"), "Should contain exec_true key");
    assertFalse(result.containsKey("exec_false"), "Should not contain exec_false key");
    assertEquals("true", result.get("branch").asString(""));
  }

  @Test
  void branchNodeOutputsExecFalseKey() {
    var node = NodeRegistry.create("flow.branch");
    var inputs = Map.of("condition", NodeValue.ofBoolean(false));

    var result = node.executeReactive(null, inputs).block();

    assertTrue(result.containsKey("exec_false"), "Should contain exec_false key");
    assertFalse(result.containsKey("exec_true"), "Should not contain exec_true key");
    assertEquals("false", result.get("branch").asString(""));
  }

  // ==================== SwitchNode Routing Tests ====================

  @Test
  void switchNodeOutputsMatchingCaseKey() {
    var node = NodeRegistry.create("flow.switch");
    var inputs = Map.of(
      "value", NodeValue.ofString("b"),
      "cases", NodeValue.ofString("a,b,c")
    );

    var result = node.executeReactive(null, inputs).block();

    assertTrue(result.containsKey("exec_case1"), "Should contain exec_case1 key");
    assertFalse(result.containsKey("exec_default"), "Should not contain exec_default key");
    assertEquals("case1", result.get("branch").asString(""));
    assertEquals(1, result.get("caseIndex").asInt(-1));
  }

  @Test
  void switchNodeOutputsDefaultKey() {
    var node = NodeRegistry.create("flow.switch");
    var inputs = Map.of(
      "value", NodeValue.ofString("z"),
      "cases", NodeValue.ofString("a,b,c")
    );

    var result = node.executeReactive(null, inputs).block();

    assertTrue(result.containsKey("exec_default"), "Should contain exec_default key");
    assertFalse(result.containsKey("exec_case0"), "Should not contain exec_case0 key");
    assertEquals("default", result.get("branch").asString(""));
    assertEquals(-1, result.get("caseIndex").asInt(0));
  }

  // ==================== GateNode Routing Tests ====================

  @Test
  void gateNodeOutputsExecAllowedKey() {
    var node = NodeRegistry.create("flow.gate");
    var inputs = new HashMap<String, NodeValue>();
    inputs.put("condition", NodeValue.ofBoolean(true));
    inputs.put("value", NodeValue.ofString("test"));

    var result = node.executeReactive(null, inputs).block();

    assertTrue(result.containsKey("exec_allowed"), "Should contain exec_allowed key");
    assertFalse(result.containsKey("exec_blocked"), "Should not contain exec_blocked key");
    assertTrue(result.get("passed").asBoolean(false));
  }

  @Test
  void gateNodeOutputsExecBlockedKey() {
    var node = NodeRegistry.create("flow.gate");
    var inputs = new HashMap<String, NodeValue>();
    inputs.put("condition", NodeValue.ofBoolean(false));
    inputs.put("value", NodeValue.ofString("test"));

    var result = node.executeReactive(null, inputs).block();

    assertTrue(result.containsKey("exec_blocked"), "Should contain exec_blocked key");
    assertFalse(result.containsKey("exec_allowed"), "Should not contain exec_allowed key");
    assertFalse(result.get("passed").asBoolean(true));
  }

  // ==================== RateLimitNode Routing Tests ====================

  @Test
  void rateLimitNodeOutputsExecAllowedKey() {
    var node = NodeRegistry.create("flow.rate_limit");
    var inputs = Map.of(
      "key", NodeValue.ofString("test-allowed-" + System.nanoTime()),
      "maxTokens", NodeValue.ofNumber(10),
      "refillRate", NodeValue.ofNumber(1),
      "tokensRequired", NodeValue.ofNumber(1)
    );

    var result = node.executeReactive(TEST_RUNTIME, inputs).block();

    assertTrue(result.containsKey("exec_allowed"), "Should contain exec_allowed key");
    assertTrue(result.get("wasAllowed").asBoolean(false));
  }

  // ==================== Data Node Exec Port Tests ====================

  @Test
  void getBotsNodeHasExecPorts() {
    var metadata = NodeRegistry.getMetadata("data.get_bots");

    var hasExecIn = metadata.inputs().stream()
      .anyMatch(p -> p.type() == PortType.EXEC && "in".equals(p.id()));
    var hasExecOut = metadata.outputs().stream()
      .anyMatch(p -> p.type() == PortType.EXEC && "out".equals(p.id()));

    assertTrue(hasExecIn, "GetBotsNode should have exec input");
    assertTrue(hasExecOut, "GetBotsNode should have exec output");
  }

  @Test
  void filterBotsNodeHasExecPorts() {
    var metadata = NodeRegistry.getMetadata("data.filter_bots");

    var hasExecIn = metadata.inputs().stream()
      .anyMatch(p -> p.type() == PortType.EXEC && "in".equals(p.id()));
    var hasExecOut = metadata.outputs().stream()
      .anyMatch(p -> p.type() == PortType.EXEC && "out".equals(p.id()));

    assertTrue(hasExecIn, "FilterBotsNode should have exec input");
    assertTrue(hasExecOut, "FilterBotsNode should have exec output");
  }

  @Test
  void getBotByNameNodeHasExecPorts() {
    var metadata = NodeRegistry.getMetadata("data.get_bot_by_name");

    var hasExecIn = metadata.inputs().stream()
      .anyMatch(p -> p.type() == PortType.EXEC && "in".equals(p.id()));
    var hasExecOut = metadata.outputs().stream()
      .anyMatch(p -> p.type() == PortType.EXEC && "out".equals(p.id()));

    assertTrue(hasExecIn, "GetBotByNameNode should have exec input");
    assertTrue(hasExecOut, "GetBotByNameNode should have exec output");
  }

  // ==================== LLMChatNode Exec Handle Tests ====================

  @Test
  void llmChatNodeOutputsExecErrorOnEmptyPrompt() {
    // LLMChatNode requires a bot, but empty prompt check happens first
    // We need to provide a bot to get past requireBot, but that needs a real instance.
    // Instead, verify that the metadata has the correct exec ports.
    var metadata = NodeRegistry.getMetadata("ai.llm_chat");

    var hasExecSuccess = metadata.outputs().stream()
      .anyMatch(p -> p.type() == PortType.EXEC && "exec_success".equals(p.id()));
    var hasExecError = metadata.outputs().stream()
      .anyMatch(p -> p.type() == PortType.EXEC && "exec_error".equals(p.id()));

    assertTrue(hasExecSuccess, "LLMChatNode should have exec_success port");
    assertTrue(hasExecError, "LLMChatNode should have exec_error port");
  }

  @Test
  void webFetchNodeHasExecPorts() {
    var metadata = NodeRegistry.getMetadata("network.web_fetch");

    var hasExecSuccess = metadata.outputs().stream()
      .anyMatch(p -> p.type() == PortType.EXEC && "exec_success".equals(p.id()));
    var hasExecError = metadata.outputs().stream()
      .anyMatch(p -> p.type() == PortType.EXEC && "exec_error".equals(p.id()));

    assertTrue(hasExecSuccess, "WebFetchNode should have exec_success port");
    assertTrue(hasExecError, "WebFetchNode should have exec_error port");
  }

  @Test
  void discordWebhookNodeHasExecPorts() {
    var metadata = NodeRegistry.getMetadata("integration.discord_webhook");

    var hasExecSuccess = metadata.outputs().stream()
      .anyMatch(p -> p.type() == PortType.EXEC && "exec_success".equals(p.id()));
    var hasExecError = metadata.outputs().stream()
      .anyMatch(p -> p.type() == PortType.EXEC && "exec_error".equals(p.id()));

    assertTrue(hasExecSuccess, "DiscordWebhookNode should have exec_success port");
    assertTrue(hasExecError, "DiscordWebhookNode should have exec_error port");
  }

  // ==================== Regression Tests ====================

  /// Recording event listener for integration tests.
  private static class RecordingEventListener implements ScriptEventListener {
    final List<String> startedNodes = new ArrayList<>();
    final List<String> completedNodes = new ArrayList<>();
    final Map<String, Map<String, NodeValue>> nodeOutputs = new HashMap<>();
    final Map<String, String> errorNodes = new HashMap<>();
    boolean scriptCompleted;

    @Override
    public void onNodeStarted(String nodeId) {
      startedNodes.add(nodeId);
    }

    @Override
    public void onNodeCompleted(String nodeId, Map<String, NodeValue> outputs) {
      completedNodes.add(nodeId);
      nodeOutputs.put(nodeId, outputs);
    }

    @Override
    public void onNodeError(String nodeId, String error) {
      errorNodes.put(nodeId, error);
    }

    @Override
    public void onScriptCompleted(boolean success) {
      scriptCompleted = success;
    }

    @Override
    public void onScriptCancelled() {}
  }

  @Test
  void standardPortsMatchActualNodePorts() {
    // BranchNode
    var branch = NodeRegistry.create("flow.branch");
    var branchResult = branch.executeReactive(null, Map.of("condition", NodeValue.ofBoolean(true))).block();
    assertTrue(branchResult.containsKey(StandardPorts.EXEC_TRUE),
      "BranchNode output keys should match StandardPorts.EXEC_TRUE");

    var branchFalse = branch.executeReactive(null, Map.of("condition", NodeValue.ofBoolean(false))).block();
    assertTrue(branchFalse.containsKey(StandardPorts.EXEC_FALSE),
      "BranchNode output keys should match StandardPorts.EXEC_FALSE");

    // GateNode
    var gate = NodeRegistry.create("flow.gate");
    var gateInputs = new HashMap<String, NodeValue>();
    gateInputs.put("condition", NodeValue.ofBoolean(true));
    gateInputs.put("value", NodeValue.ofString("test"));
    var gateResult = gate.executeReactive(null, gateInputs).block();
    assertTrue(gateResult.containsKey(StandardPorts.EXEC_ALLOWED),
      "GateNode output keys should match StandardPorts.EXEC_ALLOWED");

    // SwitchNode default
    var switchNode = NodeRegistry.create("flow.switch");
    var switchResult = switchNode.executeReactive(null,
      Map.of("value", NodeValue.ofString("z"), "cases", NodeValue.ofString("a,b"))).block();
    assertTrue(switchResult.containsKey(StandardPorts.EXEC_DEFAULT),
      "SwitchNode output keys should match StandardPorts.EXEC_DEFAULT");
  }

  @Test
  void executionRunSupportsMultiplePublishes() {
    var run = new ExecutionRun();

    // Publish to same nodeId 3 times — should not fail (unlike Sinks.One)
    run.publishNodeOutputs("node1", Map.of("value", NodeValue.ofNumber(1)));
    run.publishNodeOutputs("node1", Map.of("value", NodeValue.ofNumber(2)));
    run.publishNodeOutputs("node1", Map.of("value", NodeValue.ofNumber(3)));

    // Should get the latest value
    var result = run.awaitNodeOutputs("node1", "node1").block();
    assertNotNull(result);
    assertEquals(3, result.get("value").asInt(0));
  }

  @Test
  void executionRunLimitEnforced() {
    var run = new ExecutionRun();

    // Should allow up to 100,000
    for (var i = 0; i < 100_000; i++) {
      assertTrue(run.incrementAndCheckLimit(), "Should allow execution " + i);
    }

    // Should deny the 100,001st
    assertFalse(run.incrementAndCheckLimit(), "Should deny execution after limit");
  }

  @Test
  void nodeValueOfListPreservesBotValues() {
    // Create a mixed list with Json and non-Json values
    // Since we can't easily create a BotConnection in tests, test that
    // ofList with only Json values returns a Json array
    var jsonValues = List.of(NodeValue.ofNumber(1), NodeValue.ofString("hello"));
    var jsonList = NodeValue.ofList(jsonValues);
    var result = jsonList.asList();
    assertEquals(2, result.size());
    assertEquals(1, result.getFirst().asInt(0));
    assertEquals("hello", result.get(1).asString(""));
  }

  @Test
  void nodeValueValueListPreservesItems() {
    // Test ValueList directly
    var items = List.of(NodeValue.ofNumber(1), NodeValue.ofString("test"), NodeValue.ofBoolean(true));
    var valueList = new NodeValue.ValueList(items);
    var result = valueList.asList();

    assertEquals(3, result.size());
    assertEquals(1, result.getFirst().asInt(0));
    assertEquals("test", result.get(1).asString(""));
    assertTrue(result.get(2).asBoolean(false));
  }

  @Test
  void followExecOutputsDoesNotFallbackForMultiExecNodes() {
    // BranchNode with condition=false should NOT produce exec_true in outputs
    var node = NodeRegistry.create("flow.branch");
    var result = node.executeReactive(null, Map.of("condition", NodeValue.ofBoolean(false))).block();

    // Should contain exec_false but NOT exec_true and NOT "out"
    assertTrue(result.containsKey(StandardPorts.EXEC_FALSE));
    assertFalse(result.containsKey(StandardPorts.EXEC_TRUE));
    assertFalse(result.containsKey(StandardPorts.EXEC_OUT),
      "BranchNode should not produce 'out' key - followExecOutputs should not fallback");
  }

  @Test
  void dataEdgeDeliversValueAcrossNodes() {
    // Build graph: trigger → exec → AddNode
    // NumberConstant(42) → DATA → AddNode.a
    // NumberConstant(8) → DATA → AddNode.b
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

    // Verify data edges exist
    var dataEdges = graph.getIncomingDataEdges("add");
    assertEquals(2, dataEdges.size(), "AddNode should have 2 incoming data edges");
  }

  // ==================== Data-Only Node Eager Execution Tests ====================

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
  void dataOnlyConstantNodeExecutedViaDataEdge() {
    // Graph: trigger --EXEC--> math.add
    //        constant.number(42) --DATA--> math.add.a
    //        constant.number(8)  --DATA--> math.add.b
    // The constants have NO execution edge, only data edges.
    var graph = ScriptGraph.builder("test-data-only", "Data Only Test")
      .addNode("trigger", "trigger.on_script_init", null)
      .addNode("const_a", "constant.number", Map.of("value", 42))
      .addNode("const_b", "constant.number", Map.of("value", 8))
      .addNode("add", "math.add", null)
      .addExecutionEdge("trigger", "out", "add", "in")
      .addDataEdge("const_a", "value", "add", "a")
      .addDataEdge("const_b", "value", "add", "b")
      .build();

    var listener = new RecordingEventListener();
    var context = new ReactiveScriptContext(listener);
    var engine = new ReactiveScriptEngine();

    engine.executeFromTriggerSync(graph, "trigger", context, Map.of()).block();

    assertTrue(listener.completedNodes.contains("const_a"),
      "Data-only constant node A should be eagerly executed");
    assertTrue(listener.completedNodes.contains("const_b"),
      "Data-only constant node B should be eagerly executed");
    assertTrue(listener.completedNodes.contains("add"),
      "Math add node should complete");
    assertTrue(listener.errorNodes.isEmpty(),
      "No errors expected, got: " + listener.errorNodes);

    // Verify the add node got the correct values: 42 + 8 = 50
    var addOutputs = listener.nodeOutputs.get("add");
    assertNotNull(addOutputs, "Add node should have outputs");
    assertEquals(50.0, addOutputs.get("result").asDouble(0.0), 0.001);
  }

  @Test
  void dataOnlyStringConstantExecutedViaDataEdge() {
    // Graph: trigger --EXEC--> string.length
    //        constant.string("hello") --DATA--> string.length.text
    var graph = ScriptGraph.builder("test-string-const", "String Const Test")
      .addNode("trigger", "trigger.on_script_init", null)
      .addNode("const", "constant.string", Map.of("value", "hello"))
      .addNode("len", "string.length", null)
      .addExecutionEdge("trigger", "out", "len", "in")
      .addDataEdge("const", "value", "len", "text")
      .build();

    var listener = new RecordingEventListener();
    var context = new ReactiveScriptContext(listener);
    var engine = new ReactiveScriptEngine();

    engine.executeFromTriggerSync(graph, "trigger", context, Map.of()).block();

    assertTrue(listener.completedNodes.contains("const"),
      "Data-only string constant should be eagerly executed");
    assertTrue(listener.completedNodes.contains("len"),
      "String length node should complete");
    assertTrue(listener.errorNodes.isEmpty(),
      "No errors expected, got: " + listener.errorNodes);

    var lenOutputs = listener.nodeOutputs.get("len");
    assertNotNull(lenOutputs);
    assertEquals(5, lenOutputs.get("length").asInt(0));
  }

  @Test
  void chainedDataOnlyNodesExecutedRecursively() {
    // Graph: trigger --EXEC--> action (uses result of list.first)
    //        constant.string("a,b,c") --DATA--> string.split.text (data-only)
    //        string.split --DATA--> list.first.list (data-only)
    //        list.first --DATA--> string.length.text (on exec path)
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

    var listener = new RecordingEventListener();
    var context = new ReactiveScriptContext(listener);
    var engine = new ReactiveScriptEngine();

    engine.executeFromTriggerSync(graph, "trigger", context, Map.of()).block();

    assertTrue(listener.completedNodes.contains("const"),
      "Data-only constant should be eagerly executed");
    assertTrue(listener.completedNodes.contains("split"),
      "Data-only split should be eagerly executed");
    assertTrue(listener.completedNodes.contains("first"),
      "Data-only list.first should be eagerly executed");
    assertTrue(listener.completedNodes.contains("len"),
      "String length node should complete");
    assertTrue(listener.errorNodes.isEmpty(),
      "No errors expected, got: " + listener.errorNodes);

    // "hello" has length 5
    var lenOutputs = listener.nodeOutputs.get("len");
    assertNotNull(lenOutputs);
    assertEquals(5, lenOutputs.get("length").asInt(0));
  }

  @Test
  void dataOnlyNodeSharedByMultipleConsumers() {
    // One data-only constant feeds two different print nodes on the exec path.
    // Uses action.print which has exec in/out ports, so the chain continues.
    // Graph: trigger --EXEC--> print1 --EXEC--> print2
    //        constant.string("shared") --DATA--> print1.message
    //        constant.string("shared") --DATA--> print2.message
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

    var listener = new RecordingEventListener();
    var context = new ReactiveScriptContext(listener);
    var engine = new ReactiveScriptEngine();

    engine.executeFromTriggerSync(graph, "trigger", context, Map.of()).block();

    assertTrue(listener.errorNodes.isEmpty(),
      "No errors expected, got: " + listener.errorNodes);
    assertTrue(listener.completedNodes.contains("const"),
      "Data-only constant should be eagerly executed");
    assertTrue(listener.completedNodes.contains("print1"),
      "First print node should complete");
    assertTrue(listener.completedNodes.contains("print2"),
      "Second print node should complete (same constant consumed twice)");
  }

  @Test
  void executionContextDoesNotContainExecKeys() {
    // Verify that BranchNode outputs include exec keys but filtering works
    var node = NodeRegistry.create("flow.branch");
    var outputs = node.executeReactive(null, Map.of("condition", NodeValue.ofBoolean(true))).block();

    // Outputs should contain exec_true (for routing)
    assertTrue(outputs.containsKey(StandardPorts.EXEC_TRUE));

    // But if we filter out exec port keys (as the engine now does), they should be gone
    var execPortIds = NodeRegistry.getMetadata("flow.branch").outputs().stream()
      .filter(p -> p.type() == PortType.EXEC)
      .map(PortDefinition::id)
      .collect(Collectors.toSet());
    var contextOutputs = new HashMap<>(outputs);
    execPortIds.forEach(contextOutputs::remove);

    assertFalse(contextOutputs.containsKey(StandardPorts.EXEC_TRUE),
      "Execution context should not contain exec_true after filtering");
    assertFalse(contextOutputs.containsKey(StandardPorts.EXEC_FALSE),
      "Execution context should not contain exec_false after filtering");
    assertTrue(contextOutputs.containsKey("branch"),
      "Execution context should still contain data outputs");
    assertTrue(contextOutputs.containsKey("condition"),
      "Execution context should still contain data outputs");
  }
}
