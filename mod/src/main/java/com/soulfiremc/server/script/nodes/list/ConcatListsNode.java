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
package com.soulfiremc.server.script.nodes.list;

import com.soulfiremc.server.script.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Concatenates multiple lists into one (Blender-style multi-input socket).
/// Similar to Blender's "Join Geometry" node, this accepts multiple connections
/// to a single input socket, and all connected values are merged into one list.
///
/// The multi-input socket displays as a "pill-shaped" socket in the UI,
/// indicating it can accept multiple connections.
public final class ConcatListsNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("list.concat")
    .displayName("Concat Lists")
    .category(CategoryRegistry.LIST)
    .addInputs(
      PortDefinition.execIn(),
      // Multi-input socket - accepts multiple connections, all values collected into a list
      PortDefinition.multiInput("lists", "Lists", PortType.ANY,
        "Lists to concatenate. Connect multiple outputs to this input - all will be combined.")
    )
    .addOutputs(
      PortDefinition.execOut(),
      PortDefinition.listOutput("result", "Result", PortType.ANY, "Concatenated list of all inputs")
    )
    .description("Concatenates multiple lists into a single list. " +
      "Accepts multiple connections to the 'Lists' input (pill-shaped socket). " +
      "Non-list values are wrapped as single-element lists before concatenation.")
    .icon("layers")
    .color("#9C27B0")
    .addKeywords("concat", "join", "merge", "combine", "lists", "multi-input")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    // The "lists" input contains all connected values as a list (handled by ReactiveScriptEngine)
    var inputLists = getListInput(inputs, "lists");

    var result = new ArrayList<NodeValue>();
    for (var item : inputLists) {
      // If the item is itself a list, flatten it into the result
      var subList = item.asList();
      if (!subList.isEmpty()) {
        result.addAll(subList);
      } else if (!item.isNull()) {
        // Non-list, non-null values are added directly
        result.add(item);
      }
    }

    return completed(result("result", result));
  }
}
