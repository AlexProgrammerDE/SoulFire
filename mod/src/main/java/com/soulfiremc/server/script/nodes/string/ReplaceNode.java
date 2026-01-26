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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// String node that replaces occurrences of a substring.
/// Inputs: text, search, replacement, replaceAll (boolean)
/// Output: result
public final class ReplaceNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("string.replace")
    .displayName("Replace")
    .category(CategoryRegistry.STRING)
    .addInputs(
      PortDefinition.inputWithDefault("text", "Text", PortType.STRING, "\"\"", "Input string"),
      PortDefinition.inputWithDefault("search", "Search", PortType.STRING, "\"\"", "Substring to search for"),
      PortDefinition.inputWithDefault("replacement", "Replacement", PortType.STRING, "\"\"", "Replacement string"),
      PortDefinition.inputWithDefault("replaceAll", "Replace All", PortType.BOOLEAN, "true", "Replace all occurrences or just the first")
    )
    .addOutputs(
      PortDefinition.output("result", "Result", PortType.STRING, "String with replacements made")
    )
    .description("Replaces occurrences of a substring with another string")
    .icon("text")
    .color("#8BC34A")
    .addKeywords("string", "replace", "substitute", "swap")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var text = getStringInput(inputs, "text", "");
    var search = getStringInput(inputs, "search", "");
    var replacement = getStringInput(inputs, "replacement", "");
    var replaceAll = getBooleanInput(inputs, "replaceAll", true);

    String resultText;
    if (search.isEmpty()) {
      resultText = text;
    } else if (replaceAll) {
      resultText = text.replace(search, replacement);
    } else {
      resultText = text.replaceFirst(Pattern.quote(search), Matcher.quoteReplacement(replacement));
    }

    return completed(result("result", resultText));
  }
}
