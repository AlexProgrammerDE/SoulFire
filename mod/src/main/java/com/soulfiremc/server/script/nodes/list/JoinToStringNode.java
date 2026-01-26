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

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/// List node that joins list items into a string with a separator.
/// Inputs: list, separator
/// Output: result
public final class JoinToStringNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("list.join")
    .displayName("Join to String")
    .category(CategoryRegistry.LIST)
    .addInputs(
      PortDefinition.listInput("list", "List", PortType.ANY, "The list to join"),
      PortDefinition.inputWithDefault("separator", "Separator", PortType.STRING, "\", \"", "The separator between items")
    )
    .addOutputs(
      PortDefinition.output("result", "Result", PortType.STRING, "The joined string")
    )
    .description("Joins list items into a string with a separator")
    .icon("text")
    .color("#00BCD4")
    .addKeywords("list", "join", "string", "concat", "combine", "separator")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var list = getListInput(inputs, "list");
    var separator = getStringInput(inputs, "separator", ", ");

    var result = list.stream()
      .map(item -> item != null ? item.toString() : "null")
      .collect(Collectors.joining(separator));

    return completed(result("result", result));
  }
}
