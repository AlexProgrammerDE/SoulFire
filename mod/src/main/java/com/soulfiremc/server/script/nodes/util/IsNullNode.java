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

/// Utility node that checks if a value is null.
/// Input: value
/// Output: result (boolean)
public final class IsNullNode extends AbstractScriptNode {
  public static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("util.is_null")
    .displayName("Is Null")
    .category(CategoryRegistry.UTILITY)
    .addInputs(
      PortDefinition.input("value", "Value", PortType.ANY, "Value to check for null")
    )
    .addOutputs(
      PortDefinition.output("result", "Result", PortType.BOOLEAN, "True if value is null")
    )
    .description("Checks if a value is null")
    .icon("help-circle")
    .color("#795548")
    .addKeywords("null", "check", "undefined", "missing", "validate")
    .build();

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var value = inputs.get("value");
    return completedMono(result("result", value == null || value.isNull()));
  }
}
