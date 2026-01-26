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

/// List node that returns the length of a list.
/// Input: list
/// Output: length
public final class ListLengthNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("list.length")
    .displayName("List Length")
    .category(NodeCategory.LIST)
    .addInputs(
      PortDefinition.listInput("list", "List", PortType.ANY, "The input list")
    )
    .addOutputs(
      PortDefinition.output("length", "Length", PortType.NUMBER, "Number of items in the list")
    )
    .description("Returns the length of a list")
    .icon("list")
    .color("#00BCD4")
    .addKeywords("list", "length", "count", "size")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var list = getListInput(inputs, "list");
    return completed(result("length", list.size()));
  }
}
