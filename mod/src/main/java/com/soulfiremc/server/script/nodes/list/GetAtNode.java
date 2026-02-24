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

/// List node that gets an item at a specific index.
/// Inputs: list, index
/// Outputs: item, found (boolean)
public final class GetAtNode extends AbstractScriptNode {
  public static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("list.get_at")
    .displayName("Get At Index")
    .category(CategoryRegistry.LIST)
    .addInputs(
      PortDefinition.genericListInput("list", "List", TypeDescriptor.typeVar("T"), "The input list"),
      PortDefinition.inputWithDefault("index", "Index", PortType.NUMBER, "0", "The index to retrieve (0-based)")
    )
    .addOutputs(
      PortDefinition.genericOutput("item", "Item", TypeDescriptor.typeVar("T"), "The item at the specified index"),
      PortDefinition.output("found", "Found", PortType.BOOLEAN, "Whether the index was valid")
    )
    .description("Gets an item at a specific index in a list")
    .icon("list-ordered")
    .color("#00BCD4")
    .addKeywords("list", "get", "index", "at", "element", "access")
    .build();

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var list = getListInput(inputs, "list");
    var index = getIntInput(inputs, "index", 0);

    if (index >= 0 && index < list.size()) {
      return completedMono(results("item", list.get(index), "found", true));
    } else {
      return completedMono(results("item", null, "found", false));
    }
  }
}
