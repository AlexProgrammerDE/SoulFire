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

/// Math node that performs B-spline curve interpolation.
/// Inputs: controlPoints (list of doubles), t (0-1)
/// Output: result (interpolated value along the curve)
///
/// Uses cubic B-spline basis functions for smooth interpolation.
public final class BSplineNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("math.bspline")
    .displayName("B-Spline")
    .category(NodeCategory.MATH)
    .addInputs(
      PortDefinition.inputWithDefault("p0", "P0", PortType.NUMBER, "0", "First control point"),
      PortDefinition.inputWithDefault("p1", "P1", PortType.NUMBER, "0", "Second control point"),
      PortDefinition.inputWithDefault("p2", "P2", PortType.NUMBER, "1", "Third control point"),
      PortDefinition.inputWithDefault("p3", "P3", PortType.NUMBER, "1", "Fourth control point"),
      PortDefinition.inputWithDefault("t", "T", PortType.NUMBER, "0.5", "Interpolation factor (0-1)")
    )
    .addOutputs(
      PortDefinition.output("result", "Result", PortType.NUMBER, "Interpolated value")
    )
    .description("Performs cubic B-spline curve interpolation")
    .icon("bezier-curve")
    .color("#2196F3")
    .addKeywords("bspline", "spline", "curve", "interpolate")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    // Get control points
    var p0 = getDoubleInput(inputs, "p0", 0.0);
    var p1 = getDoubleInput(inputs, "p1", 0.0);
    var p2 = getDoubleInput(inputs, "p2", 1.0);
    var p3 = getDoubleInput(inputs, "p3", 1.0);
    var t = getDoubleInput(inputs, "t", 0.5);

    // Clamp t to [0, 1]
    t = Math.max(0.0, Math.min(1.0, t));

    // Cubic B-spline basis functions
    var t2 = t * t;
    var t3 = t2 * t;
    var mt = 1.0 - t;
    var mt2 = mt * mt;
    var mt3 = mt2 * mt;

    // B-spline formula for 4 control points
    var b0 = mt3 / 6.0;
    var b1 = (3.0 * t3 - 6.0 * t2 + 4.0) / 6.0;
    var b2 = (-3.0 * t3 + 3.0 * t2 + 3.0 * t + 1.0) / 6.0;
    var b3 = t3 / 6.0;

    var splineValue = p0 * b0 + p1 * b1 + p2 * b2 + p3 * b3;
    return completed(result("result", splineValue));
  }
}
