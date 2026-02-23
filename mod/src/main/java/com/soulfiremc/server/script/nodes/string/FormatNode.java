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
import reactor.core.publisher.Mono;

import java.util.Map;

/// String node that formats a template string with placeholders.
/// Placeholders use {0}, {1}, {2}, etc. syntax.
/// Inputs: template, args (list of values)
/// Output: result
public final class FormatNode extends AbstractScriptNode {
  public static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("string.format")
    .displayName("Format")
    .category(CategoryRegistry.STRING)
    .addInputs(
      PortDefinition.inputWithDefault("template", "Template", PortType.STRING, "\"\"", "Template string with {0}, {1}, etc. placeholders"),
      PortDefinition.listInput("args", "Arguments", PortType.ANY, "List of values to substitute into placeholders")
    )
    .addOutputs(
      PortDefinition.output("result", "Result", PortType.STRING, "Formatted string")
    )
    .description("Formats a template string by replacing {0}, {1}, etc. placeholders with arguments")
    .icon("text")
    .color("#8BC34A")
    .addKeywords("string", "format", "template", "placeholder", "interpolate")
    .build();

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var template = getStringInput(inputs, "template", "");
    var args = getListInput(inputs, "args");

    var result = template;
    for (int i = 0; i < args.size(); i++) {
      var placeholder = "{" + i + "}";
      var value = args.get(i);
      result = result.replace(placeholder, value != null ? value.asString(value.toString()) : "null");
    }

    return completedMono(result("result", result));
  }
}
