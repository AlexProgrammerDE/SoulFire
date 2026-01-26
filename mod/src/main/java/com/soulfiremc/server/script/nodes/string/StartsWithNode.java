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

/// String node that checks if a string starts with a prefix.
/// Inputs: text, prefix, ignoreCase
/// Output: result (boolean)
public final class StartsWithNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("string.starts_with")
    .displayName("Starts With")
    .category(CategoryRegistry.STRING)
    .addInputs(
      PortDefinition.inputWithDefault("text", "Text", PortType.STRING, "\"\"", "Input string to check"),
      PortDefinition.inputWithDefault("prefix", "Prefix", PortType.STRING, "\"\"", "Prefix to check for"),
      PortDefinition.inputWithDefault("ignoreCase", "Ignore Case", PortType.BOOLEAN, "false", "Whether to ignore case when checking")
    )
    .addOutputs(
      PortDefinition.output("result", "Result", PortType.BOOLEAN, "True if text starts with prefix")
    )
    .description("Checks if a string starts with a prefix")
    .icon("text")
    .color("#8BC34A")
    .addKeywords("string", "starts", "prefix", "begin")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var text = getStringInput(inputs, "text", "");
    var prefix = getStringInput(inputs, "prefix", "");
    var ignoreCase = getBooleanInput(inputs, "ignoreCase", false);

    boolean result;
    if (ignoreCase) {
      result = text.toLowerCase().startsWith(prefix.toLowerCase());
    } else {
      result = text.startsWith(prefix);
    }

    return completed(result("result", result));
  }
}
