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

/// Flow control node that branches execution based on a condition.
/// Input: condition (boolean)
/// Outputs: trueOut (fires if condition is true), falseOut (fires if condition is false)
///
/// The script executor should use the output values to determine which branch to execute.
public final class BranchNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("flow.branch")
    .displayName("Branch")
    .category(CategoryRegistry.FLOW)
    .addInputs(
      PortDefinition.execIn(),
      PortDefinition.inputWithDefault("condition", "Condition", PortType.BOOLEAN, "false", "Condition to evaluate")
    )
    .addOutputs(
      PortDefinition.output("exec_true", "True", PortType.EXEC, "Executes if condition is true"),
      PortDefinition.output("exec_false", "False", PortType.EXEC, "Executes if condition is false"),
      PortDefinition.output("branch", "Branch", PortType.STRING, "Which branch was taken"),
      PortDefinition.output("condition", "Condition", PortType.BOOLEAN, "The evaluated condition")
    )
    .description("Branches execution based on a boolean condition")
    .icon("git-branch")
    .color("#607D8B")
    .addKeywords("branch", "if", "condition", "switch", "split")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var condition = getBooleanInput(inputs, "condition", false);

    // Include exec handle key so the engine follows the correct branch
    var outputs = new HashMap<String, NodeValue>();
    outputs.put("branch", NodeValue.of(condition ? "true" : "false"));
    outputs.put("condition", NodeValue.of(condition));
    outputs.put(condition ? "exec_true" : "exec_false", NodeValue.ofBoolean(true));
    return completedMono(outputs);
  }
}
