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
package com.soulfiremc.server.script.nodes.math;

import com.soulfiremc.server.script.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/// Math node that rounds a number down to the nearest integer.
/// Input: value
/// Output: result
public final class FloorNode extends AbstractScriptNode {
  public static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("math.floor")
    .displayName("Floor")
    .category(CategoryRegistry.MATH)
    .addInputs(
      PortDefinition.inputWithDefault("value", "Value", PortType.NUMBER, "0", "Input number")
    )
    .addOutputs(
      PortDefinition.output("result", "Result", PortType.NUMBER, "Rounded down value")
    )
    .description("Rounds a number down to the nearest integer")
    .icon("arrow-down")
    .color("#2196F3")
    .addKeywords("floor", "round", "down", "integer")
    .build();

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var value = getDoubleInput(inputs, "value", 0.0);
    return completedMono(result("result", Math.floor(value)));
  }
}
