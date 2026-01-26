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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/// String node that splits a string into a list by a delimiter.
/// Inputs: text, delimiter
/// Output: result (List of strings)
public final class SplitNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("string.split")
    .displayName("Split")
    .category(CategoryRegistry.STRING)
    .addInputs(
      PortDefinition.inputWithDefault("text", "Text", PortType.STRING, "\"\"", "Input string to split"),
      PortDefinition.inputWithDefault("delimiter", "Delimiter", PortType.STRING, "\" \"", "Delimiter to split by")
    )
    .addOutputs(
      PortDefinition.listOutput("result", "Result", PortType.STRING, "List of split strings")
    )
    .description("Splits a string into a list by a delimiter")
    .icon("text")
    .color("#8BC34A")
    .addKeywords("string", "split", "divide", "separate", "tokenize")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var text = getStringInput(inputs, "text", "");
    var delimiter = getStringInput(inputs, "delimiter", " ");

    List<String> parts;
    if (text.isEmpty()) {
      parts = List.of();
    } else if (delimiter.isEmpty()) {
      // Split into individual characters
      parts = text.chars().mapToObj(c -> String.valueOf((char) c)).toList();
    } else {
      parts = Arrays.asList(text.split(Pattern.quote(delimiter)));
    }

    return completed(result("result", parts));
  }
}
