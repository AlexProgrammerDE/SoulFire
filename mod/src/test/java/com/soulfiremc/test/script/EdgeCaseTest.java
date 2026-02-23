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

import java.util.Map;

import static com.soulfiremc.test.script.ScriptTestHelper.executeNode;
import static org.junit.jupiter.api.Assertions.*;

/// Edge-case tests for node execution that go beyond the basic coverage in NodeExecutionTest.
/// Focuses on boundary values, special floating-point behavior, empty/null inputs, and unicode.
final class EdgeCaseTest {

  // --- Math boundary cases not covered by NodeExecutionTest ---

  @Test
  void addNodeWithVeryLargeNumbers() {
    var outputs = executeNode("math.add", Map.of(
      "a", NodeValue.ofNumber(Double.MAX_VALUE / 2),
      "b", NodeValue.ofNumber(Double.MAX_VALUE / 2)
    ));
    var result = outputs.get("result").asDouble(0);
    assertFalse(Double.isNaN(result), "Adding large numbers should not produce NaN");
  }

  @Test
  void moduloByZero() {
    var outputs = executeNode("math.modulo", Map.of(
      "a", NodeValue.ofNumber(10),
      "b", NodeValue.ofNumber(0)
    ));
    var result = outputs.get("result").asDouble(-1);
    assertTrue(Double.isNaN(result) || result == 0,
      "Modulo by zero should produce NaN or 0");
  }

  @Test
  void absNegativeZero() {
    var outputs = executeNode("math.abs", Map.of(
      "value", NodeValue.ofNumber(-0.0)
    ));
    assertEquals(0, outputs.get("result").asDouble(Double.NaN), 0.001);
  }

  @Test
  void ceilAndFloorOfIntegers() {
    // ceil(5) = 5, floor(5) = 5 -- integer inputs should pass through unchanged
    var ceil = executeNode("math.ceil", Map.of("value", NodeValue.ofNumber(5)));
    assertEquals(5.0, ceil.get("result").asDouble(Double.NaN), 0.001);

    var floor = executeNode("math.floor", Map.of("value", NodeValue.ofNumber(5)));
    assertEquals(5.0, floor.get("result").asDouble(Double.NaN), 0.001);
  }

  @Test
  void roundOfZero() {
    var outputs = executeNode("math.round", Map.of(
      "value", NodeValue.ofNumber(0)
    ));
    assertEquals(0.0, outputs.get("result").asDouble(Double.NaN), 0.001);
  }

  @Test
  void powZeroToZero() {
    var outputs = executeNode("math.pow", Map.of(
      "base", NodeValue.ofNumber(0),
      "exponent", NodeValue.ofNumber(0)
    ));
    assertEquals(1.0, outputs.get("result").asDouble(-1), 0.001, "0^0 should be 1");
  }

  // --- String edge cases not covered by NodeExecutionTest ---

  @Test
  void stringConcatEmptyStrings() {
    var outputs = executeNode("string.concat", Map.of(
      "a", NodeValue.ofString(""),
      "b", NodeValue.ofString("")
    ));
    assertEquals("", outputs.get("result").asString("X"));
  }

  @Test
  void stringLengthUnicode() {
    var outputs = executeNode("string.length", Map.of(
      "text", NodeValue.ofString("\u00e4\u00f6\u00fc")
    ));
    assertEquals(3, outputs.get("length").asInt(-1));
  }

  @Test
  void stringTrimAlreadyTrimmed() {
    var outputs = executeNode("string.trim", Map.of(
      "text", NodeValue.ofString("hello")
    ));
    assertEquals("hello", outputs.get("result").asString(""));
  }

  @Test
  void stringSplitEmptyDelimiter() {
    var outputs = executeNode("string.split", Map.of(
      "text", NodeValue.ofString("abc"),
      "delimiter", NodeValue.ofString("")
    ));
    var result = outputs.get("result").asList();
    assertFalse(result.isEmpty(), "Splitting by empty delimiter should produce results");
  }

  @Test
  void stringReplaceNoMatch() {
    var outputs = executeNode("string.replace", Map.of(
      "text", NodeValue.ofString("hello"),
      "search", NodeValue.ofString("xyz"),
      "replacement", NodeValue.ofString("abc")
    ));
    assertEquals("hello", outputs.get("result").asString(""),
      "Replace with no match should return original");
  }

  // --- List edge cases not covered by NodeExecutionTest ---

  @Test
  void listFirstOnEmptyList() {
    var outputs = executeNode("list.first", Map.of(
      "list", NodeValue.of(java.util.List.of())
    ));
    assertTrue(outputs.get("item").isNull(),
      "First on empty list should return null");
  }

  @Test
  void listLastOnEmptyList() {
    var outputs = executeNode("list.last", Map.of(
      "list", NodeValue.of(java.util.List.of())
    ));
    assertTrue(outputs.get("item").isNull(),
      "Last on empty list should return null");
  }

  @Test
  void listGetAtOutOfBounds() {
    var outputs = executeNode("list.get_at", Map.of(
      "list", NodeValue.of(java.util.List.of(1, 2, 3)),
      "index", NodeValue.ofNumber(10)
    ));
    assertTrue(outputs.get("item").isNull(),
      "Get at out-of-bounds index should return null");
  }

  @Test
  void listLengthEmpty() {
    var outputs = executeNode("list.length", Map.of(
      "list", NodeValue.of(java.util.List.of())
    ));
    assertEquals(0, outputs.get("length").asInt(-1));
  }

  // --- Type coercion edge cases not covered by NodeExecutionTest ---

  @Test
  void toStringFromBoolean() {
    var outputs = executeNode("util.to_string", Map.of(
      "value", NodeValue.ofBoolean(true)
    ));
    assertEquals("true", outputs.get("result").asString(""));
  }

  @Test
  void toNumberFromInvalidString() {
    var outputs = executeNode("util.to_number", Map.of(
      "value", NodeValue.ofString("not a number")
    ));
    var result = outputs.get("result").asDouble(Double.NaN);
    assertTrue(result == 0 || Double.isNaN(result),
      "Invalid string to number should produce 0 or NaN");
  }

  @Test
  void isNullWithJsonNull() {
    // NodeValue.ofNull() produces Json(JsonNull.INSTANCE), which is not Java null.
    // IsNullNode checks for Java null (missing from map), not JSON null.
    var outputs = executeNode("util.is_null", Map.of(
      "value", NodeValue.ofNull()
    ));
    assertFalse(outputs.get("result").asBoolean(true),
      "JSON null is a present value, not a missing input");
  }
}
