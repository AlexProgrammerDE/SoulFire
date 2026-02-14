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

/// Math node that performs linear interpolation between two values.
/// Inputs: a (start), b (end), t (interpolation factor 0-1)
/// Output: result = a + (b - a) * t
public final class LerpNode extends AbstractScriptNode {
  public static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("math.lerp")
    .displayName("Lerp")
    .category(CategoryRegistry.MATH)
    .addInputs(
      PortDefinition.inputWithDefault("a", "A", PortType.NUMBER, "0", "Start value"),
      PortDefinition.inputWithDefault("b", "B", PortType.NUMBER, "1", "End value"),
      PortDefinition.inputWithDefault("t", "T", PortType.NUMBER, "0.5", "Interpolation factor (0-1)")
    )
    .addOutputs(
      PortDefinition.output("result", "Result", PortType.NUMBER, "Interpolated value")
    )
    .description("Linearly interpolates between two values")
    .icon("trending-up")
    .color("#2196F3")
    .addKeywords("lerp", "interpolate", "blend", "mix")
    .build();

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var a = getDoubleInput(inputs, "a", 0.0);
    var b = getDoubleInput(inputs, "b", 1.0);
    var t = getDoubleInput(inputs, "t", 0.5);

    // Clamp t to [0, 1]
    t = Math.max(0.0, Math.min(1.0, t));

    var lerpedValue = a + (b - a) * t;
    return completedMono(result("result", lerpedValue));
  }
}
