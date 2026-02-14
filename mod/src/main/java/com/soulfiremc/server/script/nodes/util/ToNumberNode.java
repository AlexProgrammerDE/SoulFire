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
package com.soulfiremc.server.script.nodes.util;

import com.soulfiremc.server.script.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/// Utility node that parses a string to a number.
/// Input: value, default
/// Outputs: result, success (boolean)
public final class ToNumberNode extends AbstractScriptNode {
  public static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("util.to_number")
    .displayName("To Number")
    .category(CategoryRegistry.UTILITY)
    .addInputs(
      PortDefinition.inputWithDefault("value", "Value", PortType.STRING, "", "Value to parse as number"),
      PortDefinition.inputWithDefault("default", "Default", PortType.NUMBER, "0", "Default value if parsing fails")
    )
    .addOutputs(
      PortDefinition.output("result", "Result", PortType.NUMBER, "Parsed number or default"),
      PortDefinition.output("success", "Success", PortType.BOOLEAN, "Whether parsing succeeded")
    )
    .description("Parses a string to a number")
    .icon("hash")
    .color("#795548")
    .addKeywords("convert", "number", "parse", "cast", "integer", "float")
    .build();

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var value = inputs.get("value");
    var defaultValue = getDoubleInput(inputs, "default", 0.0);

    if (value != null && !value.isNull()) {
      // Try to get as number directly
      var asNum = value.asDouble(Double.NaN);
      if (!Double.isNaN(asNum)) {
        return completedMono(results("result", asNum, "success", true));
      }

      // Try to parse as string
      try {
        var parsed = Double.parseDouble(value.asString("").trim());
        return completedMono(results("result", parsed, "success", true));
      } catch (NumberFormatException _) {
        // Fall through to default
      }
    }

    return completedMono(results("result", defaultValue, "success", false));
  }
}
