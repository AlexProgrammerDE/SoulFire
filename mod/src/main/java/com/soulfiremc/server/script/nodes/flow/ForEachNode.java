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

/// Flow control node that iterates over a collection.
/// Input: items (list of items to iterate over)
/// Input: currentIndex (current iteration, managed by script executor)
/// Output: item (current item), index (current index)
/// Output: isComplete (boolean, true when iteration is done)
///
/// The script executor is responsible for managing the iteration state.
public final class ForEachNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("flow.foreach")
    .displayName("For Each")
    .category(NodeCategory.FLOW)
    .addInputs(
      PortDefinition.execIn(),
      PortDefinition.listInput("items", "Items", PortType.ANY, "List of items to iterate"),
      PortDefinition.inputWithDefault("currentIndex", "Current Index", PortType.NUMBER, "0", "Current iteration index")
    )
    .addOutputs(
      PortDefinition.output("exec_loop", "Loop", PortType.EXEC, "Executes for each item"),
      PortDefinition.output("exec_done", "Done", PortType.EXEC, "Executes when iteration completes"),
      PortDefinition.output("item", "Item", PortType.ANY, "Current item"),
      PortDefinition.output("index", "Index", PortType.NUMBER, "Current index"),
      PortDefinition.output("isComplete", "Complete", PortType.BOOLEAN, "Whether iteration is done"),
      PortDefinition.output("nextIndex", "Next Index", PortType.NUMBER, "Next iteration index"),
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
    var currentIndex = getIntInput(inputs, "currentIndex", 0);

    var isComplete = currentIndex >= items.size();
    var currentItem = isComplete ? NodeValue.ofNull() : items.get(currentIndex);

    return completed(results(
      "item", currentItem,
      "index", currentIndex,
      "isComplete", isComplete,
      "nextIndex", currentIndex + 1,
      "size", items.size()
    ));
  }
}
