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

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Math node that calculates the tangent of an angle.
/// Input: angle (in degrees)
/// Output: result
public final class TanNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("math.tan")
    .displayName("Tangent")
    .category(CategoryRegistry.MATH)
    .addInputs(
      PortDefinition.inputWithDefault("angle", "Angle", PortType.NUMBER, "0", "Angle in degrees")
    )
    .addOutputs(
      PortDefinition.output("result", "Result", PortType.NUMBER, "Tangent of angle")
    )
    .description("Calculates the tangent of an angle in degrees")
    .icon("activity")
    .color("#2196F3")
    .addKeywords("tan", "tangent", "trig", "trigonometry")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var angle = getDoubleInput(inputs, "angle", 0.0);
    return completed(result("result", Math.tan(Math.toRadians(angle))));
  }
}
