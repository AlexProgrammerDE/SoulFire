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

/// Flow control node that repeats execution a specified number of times.
/// Self-driving: uses runtime.executeDownstream() to iterate over the loop body,
/// then fires exec_done when complete.
public final class LoopNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("flow.loop")
    .displayName("Loop")
    .category(CategoryRegistry.FLOW)
    .addInputs(
      PortDefinition.execIn(),
      PortDefinition.inputWithDefault("count", "Count", PortType.NUMBER, "10", "Number of iterations")
    )
    .addOutputs(
      PortDefinition.output(StandardPorts.EXEC_LOOP, "Loop", PortType.EXEC, "Executes for each iteration"),
      PortDefinition.output(StandardPorts.EXEC_DONE, "Done", PortType.EXEC, "Executes when loop completes"),
      PortDefinition.output("index", "Index", PortType.NUMBER, "Current iteration index"),
      PortDefinition.output("isComplete", "Complete", PortType.BOOLEAN, "Whether loop is done"),
      PortDefinition.output("count", "Count", PortType.NUMBER, "Total iteration count")
    )
    .description("Repeats execution a specified number of times")
    .icon("rotate-cw")
    .color("#607D8B")
    .addKeywords("loop", "repeat", "for", "iterate", "count")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var count = getIntInput(inputs, "count", 10);

    return Flux.range(0, count)
      .concatMap(i -> runtime.executeDownstream(StandardPorts.EXEC_LOOP, Map.of(
        "index", NodeValue.ofNumber(i),
        "isComplete", NodeValue.ofBoolean(false),
        "count", NodeValue.ofNumber(count)
      )))
      .then(runtime.executeDownstream(StandardPorts.EXEC_DONE, Map.of(
        "index", NodeValue.ofNumber(count),
        "isComplete", NodeValue.ofBoolean(true),
        "count", NodeValue.ofNumber(count)
      )))
      // Return final outputs without exec handle keys so the engine's
      // dynamic routing finds nothing active and exits cleanly
      .thenReturn(results("index", count, "isComplete", true, "count", count));
  }
}
