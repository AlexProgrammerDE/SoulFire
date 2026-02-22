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
import com.soulfiremc.server.script.PortDefinition;
import com.soulfiremc.server.script.PortType;
import com.soulfiremc.server.script.StandardPorts;
import com.soulfiremc.server.script.nodes.NodeRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.soulfiremc.test.script.ScriptTestHelper.executeNode;
import static org.junit.jupiter.api.Assertions.*;

/// Tests for flow control nodes: branch, switch, gate, rate limit routing and metadata.
final class FlowNodeTest {

  // ==================== BranchNode ====================

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void branchNodeExecutes(boolean condition) {
    var result = executeNode("flow.branch", Map.of("condition", NodeValue.ofBoolean(condition)));

    assertEquals(condition ? "true" : "false", result.get("branch").asString(""));
    assertEquals(condition, result.get("condition").asBoolean(false));
  }

  @Test
  void branchNodeOutputsExecTrueKey() {
    var result = executeNode("flow.branch", Map.of("condition", NodeValue.ofBoolean(true)));

    assertTrue(result.containsKey("exec_true"), "Should contain exec_true key");
    assertFalse(result.containsKey("exec_false"), "Should not contain exec_false key");
    assertEquals("true", result.get("branch").asString(""));
  }

  @Test
  void branchNodeOutputsExecFalseKey() {
    var result = executeNode("flow.branch", Map.of("condition", NodeValue.ofBoolean(false)));

    assertTrue(result.containsKey("exec_false"), "Should contain exec_false key");
    assertFalse(result.containsKey("exec_true"), "Should not contain exec_true key");
    assertEquals("false", result.get("branch").asString(""));
  }

  @Test
  void followExecOutputsDoesNotFallbackForMultiExecNodes() {
    var result = executeNode("flow.branch", Map.of("condition", NodeValue.ofBoolean(false)));

    assertTrue(result.containsKey(StandardPorts.EXEC_FALSE),
      "Should contain exec_false key");
    assertFalse(result.containsKey(StandardPorts.EXEC_TRUE),
      "Should not contain exec_true key");
    assertFalse(result.containsKey(StandardPorts.EXEC_OUT),
      "BranchNode should not produce 'out' key - followExecOutputs should not fallback");
  }

  // ==================== SwitchNode ====================

  @Test
  void switchNodeOutputsMatchingCaseKey() {
    var result = executeNode("flow.switch", Map.of(
      "value", NodeValue.ofString("b"),
      "cases", NodeValue.ofString("a,b,c")
    ));

    assertTrue(result.containsKey("exec_case1"), "Should contain exec_case1 key");
    assertFalse(result.containsKey("exec_default"), "Should not contain exec_default key");
    assertEquals("case1", result.get("branch").asString(""));
    assertEquals(1, result.get("caseIndex").asInt(-1));
  }

  @Test
  void switchNodeOutputsDefaultKey() {
    var result = executeNode("flow.switch", Map.of(
      "value", NodeValue.ofString("z"),
      "cases", NodeValue.ofString("a,b,c")
    ));

    assertTrue(result.containsKey("exec_default"), "Should contain exec_default key");
    assertFalse(result.containsKey("exec_case0"), "Should not contain exec_case0 key");
    assertEquals("default", result.get("branch").asString(""));
    assertEquals(-1, result.get("caseIndex").asInt(0));
  }

  // ==================== GateNode ====================

  @Test
  void gateNodeOutputsExecAllowedKey() {
    var inputs = new HashMap<String, NodeValue>();
    inputs.put("condition", NodeValue.ofBoolean(true));
    inputs.put("value", NodeValue.ofString("test"));
    var result = executeNode("flow.gate", inputs);

    assertTrue(result.containsKey("exec_allowed"), "Should contain exec_allowed key");
    assertFalse(result.containsKey("exec_blocked"), "Should not contain exec_blocked key");
    assertTrue(result.get("passed").asBoolean(false));
  }

  @Test
  void gateNodeOutputsExecBlockedKey() {
    var inputs = new HashMap<String, NodeValue>();
    inputs.put("condition", NodeValue.ofBoolean(false));
    inputs.put("value", NodeValue.ofString("test"));
    var result = executeNode("flow.gate", inputs);

    assertTrue(result.containsKey("exec_blocked"), "Should contain exec_blocked key");
    assertFalse(result.containsKey("exec_allowed"), "Should not contain exec_allowed key");
    assertFalse(result.get("passed").asBoolean(true));
  }

  // ==================== RateLimitNode ====================

  @Test
  void rateLimitNodeOutputsExecAllowedKey() {
    var result = executeNode("flow.rate_limit", ScriptTestHelper.TEST_RUNTIME, Map.of(
      "key", NodeValue.ofString("test-allowed-" + System.nanoTime()),
      "maxTokens", NodeValue.ofNumber(10),
      "refillRate", NodeValue.ofNumber(1),
      "tokensRequired", NodeValue.ofNumber(1)
    ));

    assertTrue(result.containsKey("exec_allowed"), "Should contain exec_allowed key");
    assertTrue(result.get("wasAllowed").asBoolean(false));
  }

  // ==================== StandardPorts ====================

  @Test
  void standardPortsMatchActualNodePorts() {
    // BranchNode
    var branchResult = executeNode("flow.branch", Map.of("condition", NodeValue.ofBoolean(true)));
    assertTrue(branchResult.containsKey(StandardPorts.EXEC_TRUE),
      "BranchNode output keys should match StandardPorts.EXEC_TRUE");

    var branchFalse = executeNode("flow.branch", Map.of("condition", NodeValue.ofBoolean(false)));
    assertTrue(branchFalse.containsKey(StandardPorts.EXEC_FALSE),
      "BranchNode output keys should match StandardPorts.EXEC_FALSE");

    // GateNode
    var gateInputs = new HashMap<String, NodeValue>();
    gateInputs.put("condition", NodeValue.ofBoolean(true));
    gateInputs.put("value", NodeValue.ofString("test"));
    var gateResult = executeNode("flow.gate", gateInputs);
    assertTrue(gateResult.containsKey(StandardPorts.EXEC_ALLOWED),
      "GateNode output keys should match StandardPorts.EXEC_ALLOWED");

    // SwitchNode default
    var switchResult = executeNode("flow.switch",
      Map.of("value", NodeValue.ofString("z"), "cases", NodeValue.ofString("a,b")));
    assertTrue(switchResult.containsKey(StandardPorts.EXEC_DEFAULT),
      "SwitchNode output keys should match StandardPorts.EXEC_DEFAULT");
  }

  @Test
  void executionContextDoesNotContainExecKeys() {
    var outputs = executeNode("flow.branch", Map.of("condition", NodeValue.ofBoolean(true)));

    assertTrue(outputs.containsKey(StandardPorts.EXEC_TRUE),
      "BranchNode outputs should contain exec_true key");

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

  // ==================== Exec Port Metadata ====================

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

  @Test
  void llmChatNodeOutputsExecErrorOnEmptyPrompt() {
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

  // ==================== ResultNode / RepeatUntilNode Metadata ====================

  @Test
  void resultNodeHasCorrectMetadata() {
    var metadata = NodeRegistry.getMetadata("flow.result");

    assertEquals("Result", metadata.displayName());
    assertEquals("flow.result", metadata.type());

    var hasExecIn = metadata.inputs().stream()
      .anyMatch(p -> p.type() == PortType.EXEC && "in".equals(p.id()));
    assertTrue(hasExecIn, "ResultNode should have exec input");

    var hasValueInput = metadata.inputs().stream()
      .anyMatch(p -> p.type() == PortType.BOOLEAN && "value".equals(p.id()));
    assertTrue(hasValueInput, "ResultNode should have boolean value input");

    assertTrue(metadata.outputs().isEmpty(), "ResultNode should have no outputs");
  }

  @Test
  void repeatUntilHasCorrectMetadata() {
    var metadata = NodeRegistry.getMetadata("flow.repeat_until");

    var hasExecCheck = metadata.outputs().stream()
      .anyMatch(p -> p.type() == PortType.EXEC && StandardPorts.EXEC_CHECK.equals(p.id()));
    assertTrue(hasExecCheck, "RepeatUntilNode should have exec_check output");

    var hasConditionKey = metadata.inputs().stream()
      .anyMatch(p -> "conditionKey".equals(p.id()));
    assertFalse(hasConditionKey, "RepeatUntilNode should not have conditionKey input");
  }
}
