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

import java.util.HashMap;
import java.util.Map;

import static com.soulfiremc.test.script.ScriptTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

/// Tests for session and persistent variable nodes.
final class VariableNodeTest {

  @Test
  void sessionBotVariableRoundTrip() {
    var bot = createMockBot();
    var botValue = NodeValue.ofBot(bot);

    var setInputs = new HashMap<String, NodeValue>();
    setInputs.put("bot", botValue);
    setInputs.put("key", NodeValue.ofString("test_key"));
    setInputs.put("value", NodeValue.ofString("hello world"));

    var setResult = executeNode("variable.set_session", null, setInputs);
    assertTrue(setResult.get("success").asBoolean(false), "Set should succeed");

    var getInputs = new HashMap<String, NodeValue>();
    getInputs.put("bot", botValue);
    getInputs.put("key", NodeValue.ofString("test_key"));

    var getResult = executeNode("variable.get_session", null, getInputs);
    assertTrue(getResult.get("found").asBoolean(false), "Variable should be found");
    assertEquals("hello world", getResult.get("value").asString("MISSING"));
  }

  @Test
  void sessionBotVariableNotFound() {
    var bot = createMockBot();
    var botValue = NodeValue.ofBot(bot);

    var getInputs = new HashMap<String, NodeValue>();
    getInputs.put("bot", botValue);
    getInputs.put("key", NodeValue.ofString("nonexistent"));
    getInputs.put("defaultValue", NodeValue.ofString("fallback"));

    var getResult = executeNode("variable.get_session", null, getInputs);
    assertFalse(getResult.get("found").asBoolean(true), "Variable should not be found");
    assertEquals("fallback", getResult.get("value").asString("MISSING"),
      "Should return default value");
  }

  @Test
  void sessionBotVariableEmptyKeyReturnsFalse() {
    var bot = createMockBot();
    var botValue = NodeValue.ofBot(bot);

    var setInputs = new HashMap<String, NodeValue>();
    setInputs.put("bot", botValue);
    setInputs.put("key", NodeValue.ofString(""));
    setInputs.put("value", NodeValue.ofString("value"));

    var setResult = executeNode("variable.set_session", null, setInputs);
    assertFalse(setResult.get("success").asBoolean(true), "Set with empty key should fail");
  }

  @Test
  void persistentBotVariableRoundTrip() {
    var bot = createMockBot();
    var botValue = NodeValue.ofBot(bot);

    var setInputs = new HashMap<String, NodeValue>();
    setInputs.put("bot", botValue);
    setInputs.put("namespace", NodeValue.ofString("script"));
    setInputs.put("key", NodeValue.ofString("score"));
    setInputs.put("value", NodeValue.ofNumber(42));

    var setResult = executeNode("variable.set_persistent", null, setInputs);
    assertTrue(setResult.get("success").asBoolean(false), "Set should succeed");

    var getInputs = new HashMap<String, NodeValue>();
    getInputs.put("bot", botValue);
    getInputs.put("namespace", NodeValue.ofString("script"));
    getInputs.put("key", NodeValue.ofString("score"));

    var getResult = executeNode("variable.get_persistent", null, getInputs);
    assertTrue(getResult.get("found").asBoolean(false), "Variable should be found");
    assertEquals(42, getResult.get("value").asInt(0), "Should return stored value");
  }

  @Test
  void sessionInstanceVariableRoundTrip() {
    var runtime = createRuntimeWithInstance();

    var setInputs = new HashMap<String, NodeValue>();
    setInputs.put("key", NodeValue.ofString("counter"));
    setInputs.put("value", NodeValue.ofNumber(99));

    var setResult = executeNode("variable.set_session_instance", runtime, setInputs);
    assertTrue(setResult.get("success").asBoolean(false), "Set should succeed");

    var getInputs = new HashMap<String, NodeValue>();
    getInputs.put("key", NodeValue.ofString("counter"));

    var getResult = executeNode("variable.get_session_instance", runtime, getInputs);
    assertTrue(getResult.get("found").asBoolean(false), "Variable should be found");
    assertEquals(99, getResult.get("value").asInt(0), "Should return stored value");
  }

  @Test
  void sessionInstanceVariableEmptyKeyReturnsFalse() {
    var setInputs = Map.of(
      "key", NodeValue.ofString(""),
      "value", NodeValue.ofString("value")
    );

    var setResult = executeNode("variable.set_session_instance", null, setInputs);
    assertFalse(setResult.get("success").asBoolean(true), "Set with empty key should fail");
  }

  @Test
  void persistentInstanceVariableRoundTrip() {
    var runtime = createRuntimeWithInstance();

    var setInputs = new HashMap<String, NodeValue>();
    setInputs.put("namespace", NodeValue.ofString("script"));
    setInputs.put("key", NodeValue.ofString("level"));
    setInputs.put("value", NodeValue.ofNumber(5));

    var setResult = executeNode("variable.set_persistent_instance", runtime, setInputs);
    assertTrue(setResult.get("success").asBoolean(false), "Set should succeed");

    var getInputs = new HashMap<String, NodeValue>();
    getInputs.put("namespace", NodeValue.ofString("script"));
    getInputs.put("key", NodeValue.ofString("level"));

    var getResult = executeNode("variable.get_persistent_instance", runtime, getInputs);
    assertTrue(getResult.get("found").asBoolean(false), "Variable should be found");
    assertEquals(5, getResult.get("value").asInt(0), "Should return stored value");
  }
}
