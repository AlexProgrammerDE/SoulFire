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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/// Flow control node that executes multiple branches in sequence.
/// Self-driving: uses runtime.executeDownstream() to fire exec_0, exec_1, etc. in order.
public final class SequenceNode extends AbstractScriptNode {
  private static final int MAX_BRANCHES = 8;
  public static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("flow.sequence")
    .displayName("Sequence")
    .category(CategoryRegistry.FLOW)
    .addInputs(
      PortDefinition.execIn(),
      PortDefinition.inputWithDefault("branchCount", "Branch Count", PortType.NUMBER, "2", "Number of branches to execute")
    )
    .addOutputs(
      PortDefinition.output(StandardPorts.exec("0"), "Then 0", PortType.EXEC, "First branch"),
      PortDefinition.output(StandardPorts.exec("1"), "Then 1", PortType.EXEC, "Second branch"),
      PortDefinition.output(StandardPorts.exec("2"), "Then 2", PortType.EXEC, "Third branch"),
      PortDefinition.output(StandardPorts.exec("3"), "Then 3", PortType.EXEC, "Fourth branch"),
      PortDefinition.output(StandardPorts.exec("4"), "Then 4", PortType.EXEC, "Fifth branch"),
      PortDefinition.output(StandardPorts.exec("5"), "Then 5", PortType.EXEC, "Sixth branch"),
      PortDefinition.output(StandardPorts.exec("6"), "Then 6", PortType.EXEC, "Seventh branch"),
      PortDefinition.output(StandardPorts.exec("7"), "Then 7", PortType.EXEC, "Eighth branch"),
      PortDefinition.output("branchCount", "Branch Count", PortType.NUMBER, "Number of branches")
    )
    .description("Executes multiple branches in order")
    .icon("list-ordered")
    .color("#607D8B")
    .addKeywords("sequence", "order", "steps", "then")
    .supportsMuting(false)
    .build();

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var branchCount = Math.min(getIntInput(inputs, "branchCount", 2), MAX_BRANCHES);

    return Flux.range(0, branchCount)
      .concatMap(i -> runtime.executeDownstream(StandardPorts.exec(String.valueOf(i)), Map.of(
        "branchCount", NodeValue.ofNumber(branchCount)
      )))
      // Return final outputs without exec handle keys
      .then()
      .thenReturn(result("branchCount", branchCount));
  }
}
