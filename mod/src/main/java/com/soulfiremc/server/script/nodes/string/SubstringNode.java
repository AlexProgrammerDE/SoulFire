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

/// String node that extracts a substring.
/// Inputs: text, start (index), end (index, -1 for end of string)
/// Output: result
public final class SubstringNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("string.substring")
    .displayName("Substring")
    .category(CategoryRegistry.STRING)
    .addInputs(
      PortDefinition.inputWithDefault("text", "Text", PortType.STRING, "\"\"", "Input string"),
      PortDefinition.inputWithDefault("start", "Start", PortType.NUMBER, "0", "Start index (inclusive)"),
      PortDefinition.inputWithDefault("end", "End", PortType.NUMBER, "-1", "End index (exclusive, -1 for end of string)")
    )
    .addOutputs(
      PortDefinition.output("result", "Result", PortType.STRING, "Extracted substring")
    )
    .description("Extracts a substring from start index to end index")
    .icon("text")
    .color("#8BC34A")
    .addKeywords("string", "substring", "slice", "extract", "sub")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var text = getStringInput(inputs, "text", "");
    var start = getIntInput(inputs, "start", 0);
    var end = getIntInput(inputs, "end", -1);

    if (text.isEmpty()) {
      return completed(result("result", ""));
    }

    // Clamp start to valid range
    start = Math.max(0, Math.min(start, text.length()));

    // Handle end index
    if (end < 0) {
      end = text.length();
    } else {
      end = Math.min(end, text.length());
    }

    // Ensure start <= end
    if (start > end) {
      return completed(result("result", ""));
    }

    return completed(result("result", text.substring(start, end)));
  }
}
