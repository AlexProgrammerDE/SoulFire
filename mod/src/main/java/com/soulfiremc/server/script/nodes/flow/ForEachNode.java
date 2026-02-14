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
import java.util.concurrent.CompletableFuture;

/// Flow control node that iterates over a collection.
/// Self-driving: uses runtime.executeDownstream() to iterate over items,
/// then fires exec_done when complete.
public final class ForEachNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("flow.foreach")
    .displayName("For Each")
    .category(CategoryRegistry.FLOW)
    .addInputs(
      PortDefinition.execIn(),
      PortDefinition.listInput("items", "Items", PortType.ANY, "List of items to iterate")
    )
    .addOutputs(
      PortDefinition.output("exec_loop", "Loop", PortType.EXEC, "Executes for each item"),
      PortDefinition.output("exec_done", "Done", PortType.EXEC, "Executes when iteration completes"),
      PortDefinition.output("item", "Item", PortType.ANY, "Current item"),
      PortDefinition.output("index", "Index", PortType.NUMBER, "Current index"),
      PortDefinition.output("isComplete", "Complete", PortType.BOOLEAN, "Whether iteration is done"),
      PortDefinition.output("size", "Size", PortType.NUMBER, "Total items count")
    )
    .description("Iterates over each item in a list")
    .icon("repeat")
    .color("#607D8B")
    .addKeywords("foreach", "loop", "iterate", "list", "array")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var items = getListInput(inputs, "items");
    return completed(results(
      "item", items.isEmpty() ? NodeValue.ofNull() : items.getFirst(),
      "index", 0,
      "isComplete", items.isEmpty(),
      "size", items.size()
    ));
  }

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var items = getListInput(inputs, "items");
    var size = items.size();

    return Flux.range(0, size)
      .concatMap(i -> runtime.executeDownstream("exec_loop", Map.of(
        "item", items.get(i),
        "index", NodeValue.ofNumber(i),
        "isComplete", NodeValue.ofBoolean(false),
        "size", NodeValue.ofNumber(size)
      )))
      .then(runtime.executeDownstream("exec_done", Map.of(
        "item", NodeValue.ofNull(),
        "index", NodeValue.ofNumber(size),
        "isComplete", NodeValue.ofBoolean(true),
        "size", NodeValue.ofNumber(size)
      )))
      .thenReturn(results("index", size, "isComplete", true, "size", size));
  }
}
