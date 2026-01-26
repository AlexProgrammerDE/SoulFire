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

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Utility node that converts any value to a string.
/// Input: value
/// Output: result
public final class ToStringNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("util.to_string")
    .displayName("To String")
    .category(CategoryRegistry.UTILITY)
    .addInputs(
      PortDefinition.input("value", "Value", PortType.ANY, "Value to convert")
    )
    .addOutputs(
      PortDefinition.output("result", "Result", PortType.STRING, "String representation")
    )
    .description("Converts any value to a string")
    .icon("text-cursor")
    .color("#795548")
    .addKeywords("convert", "string", "text", "cast")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var value = inputs.get("value");
    var result = value != null ? value.toString() : "null";
    return completed(result("result", result));
  }
}
