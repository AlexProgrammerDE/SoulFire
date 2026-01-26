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

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Flow control node that conditionally passes through execution.
/// Input: condition (boolean) - whether to allow execution to pass through
/// Input: value (any) - the value to pass through
/// Output: passed (boolean) - whether execution was allowed
/// Output: value (any) - the passed-through value (only if condition is true)
///
/// Acts as a conditional gate - if condition is false, the output stops here.
public final class GateNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("flow.gate")
    .displayName("Gate")
    .category(CategoryRegistry.FLOW)
    .addInputs(
      PortDefinition.execIn(),
      PortDefinition.inputWithDefault("condition", "Condition", PortType.BOOLEAN, "true", "Whether to allow pass-through"),
      PortDefinition.input("value", "Value", PortType.ANY, "Value to pass through")
    )
    .addOutputs(
      PortDefinition.execOut(),
      PortDefinition.output("passed", "Passed", PortType.BOOLEAN, "Whether execution was allowed"),
      PortDefinition.output("value", "Value", PortType.ANY, "The passed-through value")
    )
    .description("Conditionally passes through execution and values")
    .icon("toggle-right")
    .color("#607D8B")
    .addKeywords("gate", "filter", "pass", "block", "conditional")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var condition = getBooleanInput(inputs, "condition", true);
    var value = inputs.get("value");

    return completed(results(
      "passed", condition,
      "value", condition ? value : null
    ));
  }
}
