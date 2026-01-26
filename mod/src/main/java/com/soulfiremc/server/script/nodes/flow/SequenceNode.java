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

/// Flow control node that executes multiple branches in sequence.
/// This node simply passes execution through and is used to organize sequential execution flow.
/// The script executor handles connecting multiple outputs from this node to execute them in order.
///
/// Output: out0, out1, out2, etc. (execution outputs for each branch)
public final class SequenceNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("flow.sequence")
    .displayName("Sequence")
    .category(NodeCategory.FLOW)
    .addInputs(
      PortDefinition.execIn(),
      PortDefinition.inputWithDefault("branchCount", "Branch Count", PortType.NUMBER, "2", "Number of branches to execute")
    )
    .addOutputs(
      PortDefinition.output("exec_0", "Then 0", PortType.EXEC, "First branch"),
      PortDefinition.output("exec_1", "Then 1", PortType.EXEC, "Second branch"),
      PortDefinition.output("branchCount", "Branch Count", PortType.NUMBER, "Number of branches")
    )
    .description("Executes multiple branches in order")
    .icon("list-ordered")
    .color("#607D8B")
    .addKeywords("sequence", "order", "steps", "then")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var branchCount = getIntInput(inputs, "branchCount", 2);

    // Simply output the branch count so the executor knows how many branches to execute
    return completed(result("branchCount", branchCount));
  }
}
