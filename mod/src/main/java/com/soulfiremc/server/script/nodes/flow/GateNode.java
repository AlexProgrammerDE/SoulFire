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
package com.soulfiremc.server.script.nodes.flow;

import com.soulfiremc.server.script.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/// Flow control node that conditionally passes through execution.
/// Input: condition (boolean) - whether to allow execution to pass through
/// Input: value (any) - the value to pass through
/// Output: passed (boolean) - whether execution was allowed
/// Output: value (any) - the passed-through value (only if condition is true)
///
/// Routes to exec_allowed if condition is true, exec_blocked if false.
public final class GateNode extends AbstractScriptNode {
  public static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("flow.gate")
    .displayName("Gate")
    .category(CategoryRegistry.FLOW)
    .addInputs(
      PortDefinition.execIn(),
      PortDefinition.inputWithDefault("condition", "Condition", PortType.BOOLEAN, "true", "Whether to allow pass-through"),
      PortDefinition.input("value", "Value", PortType.ANY, "Value to pass through")
    )
    .addOutputs(
      PortDefinition.output(StandardPorts.EXEC_ALLOWED, "Allowed", PortType.EXEC, "Executes if condition is true"),
      PortDefinition.output(StandardPorts.EXEC_BLOCKED, "Blocked", PortType.EXEC, "Executes if condition is false"),
      PortDefinition.output("passed", "Passed", PortType.BOOLEAN, "Whether execution was allowed"),
      PortDefinition.output("value", "Value", PortType.ANY, "The passed-through value")
    )
    .description("Conditionally passes through execution and values")
    .icon("toggle-right")
    .color("#607D8B")
    .addKeywords("gate", "filter", "pass", "block", "conditional")
    .build();

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var condition = getBooleanInput(inputs, "condition", true);
    var value = inputs.get("value");

    var outputs = new HashMap<String, NodeValue>();
    outputs.put("passed", NodeValue.of(condition));
    outputs.put("value", condition ? value : NodeValue.ofNull());
    outputs.put(condition ? StandardPorts.EXEC_ALLOWED : StandardPorts.EXEC_BLOCKED, NodeValue.ofBoolean(true));
    return completedMono(outputs);
  }
}
