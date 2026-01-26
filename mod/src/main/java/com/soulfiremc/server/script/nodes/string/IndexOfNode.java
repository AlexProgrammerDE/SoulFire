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
package com.soulfiremc.server.script.nodes.string;

import com.soulfiremc.server.script.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// String node that finds the index of a substring.
/// Inputs: text, search, ignoreCase
/// Output: index (-1 if not found)
public final class IndexOfNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("string.index_of")
    .displayName("Index Of")
    .category(CategoryRegistry.STRING)
    .addInputs(
      PortDefinition.inputWithDefault("text", "Text", PortType.STRING, "\"\"", "Input string to search in"),
      PortDefinition.inputWithDefault("search", "Search", PortType.STRING, "\"\"", "Substring to search for"),
      PortDefinition.inputWithDefault("ignoreCase", "Ignore Case", PortType.BOOLEAN, "false", "Whether to ignore case when searching")
    )
    .addOutputs(
      PortDefinition.output("index", "Index", PortType.NUMBER, "Index of first occurrence (-1 if not found)")
    )
    .description("Finds the index of the first occurrence of a substring")
    .icon("text")
    .color("#8BC34A")
    .addKeywords("string", "index", "find", "search", "position", "location")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var text = getStringInput(inputs, "text", "");
    var search = getStringInput(inputs, "search", "");
    var ignoreCase = getBooleanInput(inputs, "ignoreCase", false);

    int index;
    if (ignoreCase) {
      index = text.toLowerCase().indexOf(search.toLowerCase());
    } else {
      index = text.indexOf(search);
    }

    return completed(result("index", index));
  }
}
