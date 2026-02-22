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

import com.soulfiremc.server.script.NodeValue;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/// Tests for {@link NodeValue} creation, conversion, and list handling.
final class NodeValueTest {

  @Test
  void nodeValueOfString() {
    var value = NodeValue.ofString("hello");
    assertEquals("hello", value.asString("default"), "String value should be 'hello'");
    assertFalse(value.isNull(), "String value should not be null");
  }

  @Test
  void nodeValueOfNumber() {
    var value = NodeValue.ofNumber(42);
    assertEquals(42, value.asInt(0), "Int value should be 42");
    assertEquals(42.0, value.asDouble(0.0), "Double value should be 42.0");
    assertEquals(42L, value.asLong(0L), "Long value should be 42");
  }

  @Test
  void nodeValueOfBoolean() {
    var trueValue = NodeValue.ofBoolean(true);
    var falseValue = NodeValue.ofBoolean(false);

    assertTrue(trueValue.asBoolean(false), "True value should be true");
    assertFalse(falseValue.asBoolean(true), "False value should be false");
  }

  @Test
  void nodeValueOfNull() {
    var value = NodeValue.ofNull();
    assertTrue(value.isNull(), "Null value should be null");
    assertEquals("default", value.asString("default"), "Null value should return default string");
    assertEquals(0, value.asInt(0), "Null value should return default int");
  }

  @Test
  void nodeValueOfList() {
    var list = List.of(NodeValue.ofNumber(1), NodeValue.ofNumber(2), NodeValue.ofNumber(3));
    var value = NodeValue.ofList(list);

    var result = value.asList();
    assertEquals(3, result.size(), "List should have 3 elements");
    assertEquals(1, result.getFirst().asInt(0), "First element should be 1");
    assertEquals(2, result.get(1).asInt(0), "Second element should be 2");
    assertEquals(3, result.get(2).asInt(0), "Third element should be 3");
  }

  @Test
  void nodeValueOfMap() {
    var map = Map.of("key1", "value1", "key2", 42);
    var value = NodeValue.of(map);

    assertFalse(value.isNull(), "Map value should not be null");
    assertNotNull(value.asJsonElement(), "Map value should have a JSON element");
    assertTrue(value.asJsonElement().isJsonObject(), "Map value should be a JSON object");
  }

  @Test
  void nodeValueConversionFromObject() {
    assertEquals("test", NodeValue.of("test").asString(""), "String conversion should work");
    assertEquals(123, NodeValue.of(123).asInt(0), "Integer conversion should work");
    assertTrue(NodeValue.of(true).asBoolean(false), "Boolean conversion should work");
    assertTrue(NodeValue.of(null).isNull(), "Null conversion should produce null value");
  }

  @Test
  void nodeValueStringList() {
    var value = NodeValue.of(List.of("a", "b", "c"));
    var strings = value.asStringList();

    assertEquals(3, strings.size(), "String list should have 3 elements");
    assertEquals("a", strings.getFirst(), "First string should be 'a'");
    assertEquals("b", strings.get(1), "Second string should be 'b'");
    assertEquals("c", strings.get(2), "Third string should be 'c'");
  }

  @Test
  void nodeValueOfListPreservesBotValues() {
    var jsonValues = List.of(NodeValue.ofNumber(1), NodeValue.ofString("hello"));
    var jsonList = NodeValue.ofList(jsonValues);
    var result = jsonList.asList();
    assertEquals(2, result.size(), "JSON list should have 2 elements");
    assertEquals(1, result.getFirst().asInt(0), "First element should be 1");
    assertEquals("hello", result.get(1).asString(""), "Second element should be 'hello'");
  }

  @Test
  void nodeValueValueListPreservesItems() {
    var items = List.of(NodeValue.ofNumber(1), NodeValue.ofString("test"), NodeValue.ofBoolean(true));
    var valueList = new NodeValue.ValueList(items);
    var result = valueList.asList();

    assertEquals(3, result.size(), "ValueList should have 3 items");
    assertEquals(1, result.getFirst().asInt(0), "First item should be 1");
    assertEquals("test", result.get(1).asString(""), "Second item should be 'test'");
    assertTrue(result.get(2).asBoolean(false), "Third item should be true");
  }
}
