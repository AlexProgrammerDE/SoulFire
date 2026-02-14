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
import org.checkerframework.checker.units.qual.min;
import reactor.core.publisher.Mono;

import java.util.Map;

/// Math node that constrains a value within a range.
/// Inputs: value, min, max
/// Output: result = clamped value within [min, max]
public final class ClampNode extends AbstractScriptNode {
  public static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("math.clamp")
    .displayName("Clamp")
    .category(CategoryRegistry.MATH)
    .addInputs(
      PortDefinition.inputWithDefault("value", "Value", PortType.NUMBER, "0", "Value to clamp"),
      PortDefinition.inputWithDefault("min", "Min", PortType.NUMBER, "0", "Minimum bound"),
      PortDefinition.inputWithDefault("max", "Max", PortType.NUMBER, "1", "Maximum bound")
    )
    .addOutputs(
      PortDefinition.output("result", "Result", PortType.NUMBER, "Clamped value")
    )
    .description("Constrains a value within a range")
    .icon("git-commit")
    .color("#2196F3")
    .addKeywords("clamp", "constrain", "limit", "bound")
    .build();

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var value = getDoubleInput(inputs, "value", 0.0);
    var min = getDoubleInput(inputs, "min", 0.0);
    var max = getDoubleInput(inputs, "max", 1.0);

    var clampedValue = Math.max(min, Math.min(max, value));
    return completedMono(result("result", clampedValue));
  }
}
