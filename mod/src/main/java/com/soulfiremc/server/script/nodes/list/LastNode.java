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
import reactor.core.publisher.Mono;

import java.util.Map;

/// List node that gets the last item in a list.
/// Input: list
/// Outputs: item, found (boolean)
public final class LastNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("list.last")
    .displayName("Last")
    .category(CategoryRegistry.LIST)
    .addInputs(
      PortDefinition.listInput("list", "List", PortType.ANY, "The input list")
    )
    .addOutputs(
      PortDefinition.output("item", "Item", PortType.ANY, "The last item in the list"),
      PortDefinition.output("found", "Found", PortType.BOOLEAN, "Whether the list was non-empty")
    )
    .description("Gets the last item in a list")
    .icon("arrow-down-to-line")
    .color("#00BCD4")
    .addKeywords("list", "last", "tail", "back", "end")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var list = getListInput(inputs, "list");

    if (!list.isEmpty()) {
      return completedMono(results("item", list.getLast(), "found", true));
    } else {
      return completedMono(results("item", null, "found", false));
    }
  }
}
