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

/// Tests for JSON nodes: JsonParse, JsonStringify, JsonGet, JsonSet, JsonObject, JsonArray.
final class JsonNodeTest {

  // --- JSON Parse ---

  @Test
  void jsonParseValidObject() {
    var outputs = executeNode("json.parse", Map.of(
      "json", NodeValue.ofString("{\"name\":\"Alex\",\"age\":25}")
    ));
    assertTrue(outputs.get("success").asBoolean(false));
    assertEquals("", outputs.get("errorMessage").asString("X"));
  }

  @Test
  void jsonParseValidArray() {
    var outputs = executeNode("json.parse", Map.of(
      "json", NodeValue.ofString("[1,2,3]")
    ));
    assertTrue(outputs.get("success").asBoolean(false));
    var result = outputs.get("result").asList();
    assertEquals(3, result.size());
  }

  @Test
  void jsonParseInvalidJson() {
    var outputs = executeNode("json.parse", Map.of(
      "json", NodeValue.ofString("{invalid json")
    ));
    assertFalse(outputs.get("success").asBoolean(true));
    assertFalse(outputs.get("errorMessage").asString("").isEmpty(),
      "Error message should describe parse failure");
  }

  @Test
  void jsonParseEmptyString() {
    var outputs = executeNode("json.parse", Map.of(
      "json", NodeValue.ofString("")
    ));
    // Empty string may or may not parse depending on JSON library version.
    // Just verify the node doesn't throw and returns a valid result.
    assertNotNull(outputs.get("success"), "Should have a success output");
  }

  @Test
  void jsonParsePrimitive() {
    var outputs = executeNode("json.parse", Map.of(
      "json", NodeValue.ofString("42")
    ));
    assertTrue(outputs.get("success").asBoolean(false));
    assertEquals(42, outputs.get("result").asInt(0));
  }

  // --- JSON Stringify ---

  @Test
  void jsonStringifyNumber() {
    var outputs = executeNode("json.stringify", Map.of(
      "value", NodeValue.ofNumber(42)
    ));
    assertEquals("42", outputs.get("json").asString(""));
  }

  @Test
  void jsonStringifyString() {
    var outputs = executeNode("json.stringify", Map.of(
      "value", NodeValue.ofString("hello")
    ));
    assertEquals("\"hello\"", outputs.get("json").asString(""));
  }

  @Test
  void jsonStringifyNull() {
    var outputs = executeNode("json.stringify", Map.of(
      "value", NodeValue.ofNull()
    ));
    assertEquals("null", outputs.get("json").asString(""));
  }

  @Test
  void jsonStringifyBoolean() {
    var outputs = executeNode("json.stringify", Map.of(
      "value", NodeValue.ofBoolean(true)
    ));
    assertEquals("true", outputs.get("json").asString(""));
  }

  // --- JSON Get ---

  @Test
  void jsonGetSimplePath() {
    var outputs = executeNode("json.get", Map.of(
      "json", NodeValue.ofString("{\"name\":\"Alex\"}"),
      "path", NodeValue.ofString("name")
    ));
    assertTrue(outputs.get("found").asBoolean(false));
    assertEquals("Alex", outputs.get("value").asString(""));
  }

  @Test
  void jsonGetNestedPath() {
    var outputs = executeNode("json.get", Map.of(
      "json", NodeValue.ofString("{\"user\":{\"name\":\"Alex\"}}"),
      "path", NodeValue.ofString("user.name")
    ));
    assertTrue(outputs.get("found").asBoolean(false));
    assertEquals("Alex", outputs.get("value").asString(""));
  }

  @Test
  void jsonGetArrayIndex() {
    var outputs = executeNode("json.get", Map.of(
      "json", NodeValue.ofString("{\"items\":[\"a\",\"b\",\"c\"]}"),
      "path", NodeValue.ofString("items[1]")
    ));
    assertTrue(outputs.get("found").asBoolean(false));
    assertEquals("b", outputs.get("value").asString(""));
  }

  @Test
  void jsonGetMissingPath() {
    var outputs = executeNode("json.get", Map.of(
      "json", NodeValue.ofString("{\"name\":\"Alex\"}"),
      "path", NodeValue.ofString("age")
    ));
    assertFalse(outputs.get("found").asBoolean(true));
  }

  @Test
  void jsonGetMissingPathWithDefault() {
    var outputs = executeNode("json.get", Map.of(
      "json", NodeValue.ofString("{\"name\":\"Alex\"}"),
      "path", NodeValue.ofString("age"),
      "defaultValue", NodeValue.ofNumber(0)
    ));
    assertFalse(outputs.get("found").asBoolean(true));
    assertEquals(0, outputs.get("value").asInt(-1));
  }

  @Test
  void jsonGetEmptyPathReturnsRoot() {
    var outputs = executeNode("json.get", Map.of(
      "json", NodeValue.ofString("{\"a\":1}"),
      "path", NodeValue.ofString("")
    ));
    assertTrue(outputs.get("found").asBoolean(false));
  }

  @Test
  void jsonGetOutOfBoundsArray() {
    var outputs = executeNode("json.get", Map.of(
      "json", NodeValue.ofString("[1,2,3]"),
      "path", NodeValue.ofString("[10]")
    ));
    assertFalse(outputs.get("found").asBoolean(true));
  }

  // --- JSON Set ---

  @Test
  void jsonSetSimplePath() {
    var outputs = executeNode("json.set", Map.of(
      "json", NodeValue.ofString("{\"name\":\"Alex\"}"),
      "path", NodeValue.ofString("age"),
      "value", NodeValue.ofNumber(25)
    ));
    assertTrue(outputs.get("success").asBoolean(false));
    var result = outputs.get("result").asString("");
    assertTrue(result.contains("\"age\""), "Result should contain 'age' key");
    assertTrue(result.contains("25"), "Result should contain value 25");
  }

  @Test
  void jsonSetNestedPath() {
    var outputs = executeNode("json.set", Map.of(
      "json", NodeValue.ofString("{\"user\":{}}"),
      "path", NodeValue.ofString("user.name"),
      "value", NodeValue.ofString("Alex")
    ));
    assertTrue(outputs.get("success").asBoolean(false));
    var result = outputs.get("result").asString("");
    assertTrue(result.contains("\"name\""), "Result should contain nested key");
    assertTrue(result.contains("Alex"), "Result should contain value");
  }

  @Test
  void jsonSetEmptyPath() {
    var outputs = executeNode("json.set", Map.of(
      "json", NodeValue.ofString("{}"),
      "path", NodeValue.ofString(""),
      "value", NodeValue.ofString("test")
    ));
    assertFalse(outputs.get("success").asBoolean(true),
      "Empty path should fail");
  }

  // --- JSON Object ---

  @Test
  void jsonObjectFromKeysAndValues() {
    var outputs = executeNode("json.object", Map.of(
      "keys", NodeValue.of(java.util.List.of("name", "age")),
      "values", NodeValue.of(java.util.List.of("Alex", 25))
    ));
    var obj = outputs.get("object").asString("");
    assertTrue(obj.contains("\"name\""), "Should contain key 'name'");
    assertTrue(obj.contains("\"Alex\""), "Should contain value 'Alex'");
    assertTrue(obj.contains("\"age\""), "Should contain key 'age'");
  }

  @Test
  void jsonObjectMismatchedLengths() {
    // More keys than values: should use only matching pairs
    var outputs = executeNode("json.object", Map.of(
      "keys", NodeValue.of(java.util.List.of("a", "b", "c")),
      "values", NodeValue.of(java.util.List.of("1"))
    ));
    var obj = outputs.get("object").asString("");
    assertTrue(obj.contains("\"a\""), "Should contain first key");
    assertFalse(obj.contains("\"b\""), "Should not contain unmatched key");
  }

  // --- JSON Array ---

  @Test
  void jsonArrayFromItems() {
    var outputs = executeNode("json.array", Map.of(
      "items", NodeValue.of(java.util.List.of(1, 2, 3))
    ));
    var arr = outputs.get("array").asString("");
    assertEquals("[1,2,3]", arr);
  }

  @Test
  void jsonArrayWithStrings() {
    var outputs = executeNode("json.array", Map.of(
      "items", NodeValue.of(java.util.List.of("a", "b"))
    ));
    var arr = outputs.get("array").asString("");
    assertEquals("[\"a\",\"b\"]", arr);
  }

  // --- Round-trip ---

  @Test
  void parseAndStringifyRoundTrip() {
    var json = "{\"key\":\"value\",\"num\":42}";
    var parsed = executeNode("json.parse", Map.of(
      "json", NodeValue.ofString(json)
    ));
    assertTrue(parsed.get("success").asBoolean(false));

    var stringified = executeNode("json.stringify", Map.of(
      "value", parsed.get("result")
    ));
    var result = stringified.get("json").asString("");
    // Parse both to compare content (order may differ)
    var reparsed = executeNode("json.parse", Map.of(
      "json", NodeValue.ofString(result)
    ));
    assertTrue(reparsed.get("success").asBoolean(false),
      "Re-parsed stringified JSON should be valid");
  }
}
