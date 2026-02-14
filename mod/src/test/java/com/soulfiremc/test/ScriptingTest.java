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

import com.soulfiremc.server.script.ExecutionContext;
import com.soulfiremc.server.script.NodeValue;
import com.soulfiremc.server.script.ScriptGraph;
import com.soulfiremc.server.script.nodes.NodeRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class ScriptingTest {

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
      .addNode("node1", "trigger.on_tick", null)
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
      .addNode("node1", "trigger.on_tick", null)
      .build();

    var node = graph.getNode("node1");
    assertNotNull(node);
    assertEquals("node1", node.id());
    assertEquals("trigger.on_tick", node.type());

    assertNull(graph.getNode("nonexistent"));
  }

  @Test
  void scriptGraphFindTriggerNodes() {
    var graph = ScriptGraph.builder("test-id", "Test Script")
      .addNode("trigger1", "trigger.on_tick", null)
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
      .addNode("trigger", "trigger.on_tick", null)
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
      .addNode("trigger", "trigger.on_tick", null)
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
    assertTrue(NodeRegistry.isRegistered("trigger.on_tick"));
    assertTrue(NodeRegistry.isRegistered("trigger.on_join"));
  }

  @Test
  void nodeRegistryCreateReturnsNewInstance() {
    var node1 = NodeRegistry.create("math.add");
    var node2 = NodeRegistry.create("math.add");

    assertNotNull(node1);
    assertNotNull(node2);
    assertNotSame(node1, node2);
    assertEquals("math.add", node1.getId());
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

    var result = node.execute(null, inputs).join();

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

    var result = node.execute(null, inputs).join();

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

    var result = node.execute(null, inputs).join();

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

    var result = node.execute(null, inputs).join();

    assertEquals(expected, result.get("result").asDouble(0.0), 0.001);
  }

  @Test
  void divideByZeroReturnsZero() {
    var node = NodeRegistry.create("math.divide");
    var inputs = Map.of(
      "a", NodeValue.ofNumber(10),
      "b", NodeValue.ofNumber(0)
    );

    var result = node.execute(null, inputs).join();

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

    var result = node.execute(null, inputs).join();

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

    var result = node.execute(null, inputs).join();

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

    var result = node.execute(null, inputs).join();

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

    var result = node.execute(null, inputs).join();

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

    var result = node.execute(null, inputs).join();

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

    var result = node.execute(null, inputs).join();

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

    var result = node.execute(null, inputs).join();

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

    var result = node.execute(null, inputs).join();

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

    var result = node.execute(null, inputs).join();

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

    var result = node.execute(null, inputs).join();

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

    var result = node.execute(null, inputs).join();

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

    var result = node.execute(null, inputs).join();

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

    var result = node.execute(null, inputs).join();

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

    var result = node.execute(null, inputs).join();

    assertEquals(expected, result.get("result").asBoolean(false));
  }

  // ==================== Flow Node Tests ====================

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void branchNodeExecutes(boolean condition) {
    var node = NodeRegistry.create("flow.branch");
    var inputs = Map.of("condition", NodeValue.ofBoolean(condition));

    var result = node.execute(null, inputs).join();

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

    var result = node.execute(null, inputs).join();

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

    var result = node.execute(null, inputs).join();

    assertEquals("Hello Universe", result.get("result").asString(""));
  }

  @Test
  void splitNodeExecutes() {
    var node = NodeRegistry.create("string.split");
    var inputs = Map.of(
      "text", NodeValue.ofString("a,b,c"),
      "delimiter", NodeValue.ofString(",")
    );

    var result = node.execute(null, inputs).join();
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

    var result = node.execute(null, inputs).join();

    assertEquals("Hello", result.get("result").asString(""));
  }

  @Test
  void stringLengthNodeExecutes() {
    var node = NodeRegistry.create("string.length");
    var inputs = Map.of("text", NodeValue.ofString("Hello"));

    var result = node.execute(null, inputs).join();

    assertEquals(5, result.get("length").asInt(0));
  }

  @Test
  void toLowerCaseNodeExecutes() {
    var node = NodeRegistry.create("string.to_lower_case");
    var inputs = Map.of("text", NodeValue.ofString("HELLO"));

    var result = node.execute(null, inputs).join();

    assertEquals("hello", result.get("result").asString(""));
  }

  @Test
  void toUpperCaseNodeExecutes() {
    var node = NodeRegistry.create("string.to_upper_case");
    var inputs = Map.of("text", NodeValue.ofString("hello"));

    var result = node.execute(null, inputs).join();

    assertEquals("HELLO", result.get("result").asString(""));
  }

  @Test
  void trimNodeExecutes() {
    var node = NodeRegistry.create("string.trim");
    var inputs = Map.of("text", NodeValue.ofString("  hello  "));

    var result = node.execute(null, inputs).join();

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

    var result = node.execute(null, inputs).join();

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

    var result = node.execute(null, inputs).join();

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

    var result = node.execute(null, inputs).join();

    assertEquals(expected, result.get("result").asBoolean(false));
  }

  // ==================== List Node Tests ====================

  @Test
  void listLengthNodeExecutes() {
    var node = NodeRegistry.create("list.length");
    var inputs = Map.of(
      "list", NodeValue.of(List.of(1, 2, 3, 4, 5))
    );

    var result = node.execute(null, inputs).join();

    assertEquals(5, result.get("length").asInt(0));
  }

  @Test
  void getAtNodeExecutes() {
    var node = NodeRegistry.create("list.get_at");
    var inputs = Map.of(
      "list", NodeValue.of(List.of("a", "b", "c")),
      "index", NodeValue.ofNumber(1)
    );

    var result = node.execute(null, inputs).join();

    assertEquals("b", result.get("item").asString(""));
  }

  @Test
  void firstNodeExecutes() {
    var node = NodeRegistry.create("list.first");
    var inputs = Map.of(
      "list", NodeValue.of(List.of("first", "second", "third"))
    );

    var result = node.execute(null, inputs).join();

    assertEquals("first", result.get("item").asString(""));
  }

  @Test
  void lastNodeExecutes() {
    var node = NodeRegistry.create("list.last");
    var inputs = Map.of(
      "list", NodeValue.of(List.of("first", "second", "third"))
    );

    var result = node.execute(null, inputs).join();

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

    var result = node.execute(null, inputs).join();

    assertEquals(expected, result.get("result").asBoolean(false));
  }

  @Test
  void joinToStringNodeExecutes() {
    var node = NodeRegistry.create("list.join");
    var inputs = Map.of(
      "list", NodeValue.of(List.of("a", "b", "c")),
      "separator", NodeValue.ofString(", ")
    );

    var result = node.execute(null, inputs).join();

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

    var result = node.execute(null, inputs).join();
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

    var result = node.execute(null, inputs).join();

    assertEquals(42, result.get("value").asInt(0));
  }

  @Test
  void stringConstantNodeExecutes() {
    var node = NodeRegistry.create("constant.string");
    var inputs = Map.of("value", NodeValue.ofString("test"));

    var result = node.execute(null, inputs).join();

    assertEquals("test", result.get("value").asString(""));
  }

  @Test
  void booleanConstantNodeExecutes() {
    var node = NodeRegistry.create("constant.boolean");
    var inputs = Map.of("value", NodeValue.ofBoolean(true));

    var result = node.execute(null, inputs).join();

    assertTrue(result.get("value").asBoolean(false));
  }

  // ==================== Utility Node Tests ====================

  @Test
  void toStringNodeExecutes() {
    var node = NodeRegistry.create("util.to_string");
    var inputs = Map.of("value", NodeValue.ofNumber(42));

    var result = node.execute(null, inputs).join();

    assertEquals("42", result.get("result").asString(""));
  }

  @Test
  void toNumberNodeExecutes() {
    var node = NodeRegistry.create("util.to_number");
    var inputs = Map.of("value", NodeValue.ofString("42.5"));

    var result = node.execute(null, inputs).join();

    assertEquals(42.5, result.get("result").asDouble(0.0), 0.001);
  }

  @Test
  void isNullNodeExecutesWithMissingValue() {
    var node = NodeRegistry.create("util.is_null");
    // IsNullNode checks if the input key is absent (Java null), not JSON null
    var inputs = Map.<String, NodeValue>of();

    var result = node.execute(null, inputs).join();

    assertTrue(result.get("result").asBoolean(false));
  }

  @Test
  void isNullNodeExecutesWithPresentValue() {
    var node = NodeRegistry.create("util.is_null");
    var inputs = Map.of("value", NodeValue.ofString("hello"));

    var result = node.execute(null, inputs).join();

    assertFalse(result.get("result").asBoolean(true));
  }

  @Test
  void isEmptyNodeExecutesWithEmptyString() {
    var node = NodeRegistry.create("util.is_empty");
    var inputs = Map.of("value", NodeValue.ofString(""));

    var result = node.execute(null, inputs).join();

    assertTrue(result.get("result").asBoolean(false));
  }

  @Test
  void isEmptyNodeExecutesWithNonEmptyString() {
    var node = NodeRegistry.create("util.is_empty");
    var inputs = Map.of("value", NodeValue.ofString("hello"));

    var result = node.execute(null, inputs).join();

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
      var node = NodeRegistry.create(type);
      var metadata = node.getMetadata();

      assertNotNull(metadata, "Node " + type + " has no metadata");
      assertNotNull(metadata.type(), "Node " + type + " has no type");
      assertNotNull(metadata.displayName(), "Node " + type + " has no display name");
      assertNotNull(metadata.category(), "Node " + type + " has no category");
      assertNotNull(metadata.description(), "Node " + type + " has no description");
      assertEquals(type, metadata.type(), "Node metadata type doesn't match registered type");
    }
  }

  @Test
  void allNodesHaveConsistentId() {
    for (var type : NodeRegistry.getRegisteredTypes()) {
      var node = NodeRegistry.create(type);
      assertEquals(type, node.getId(), "Node getId() doesn't match registered type");
    }
  }
}
