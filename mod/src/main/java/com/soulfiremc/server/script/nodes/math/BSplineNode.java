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

import com.soulfiremc.server.script.AbstractScriptNode;
import com.soulfiremc.server.script.NodeValue;
import com.soulfiremc.server.script.NodeRuntime;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Math node that performs B-spline curve interpolation.
/// Inputs: controlPoints (list of doubles), t (0-1)
/// Output: result (interpolated value along the curve)
///
/// Uses cubic B-spline basis functions for smooth interpolation.
public final class BSplineNode extends AbstractScriptNode {
  public static final String TYPE = "math.bspline";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public Map<String, NodeValue> getDefaultInputs() {
    return Map.of(
      "p0", NodeValue.ofNumber(0.0),
      "p1", NodeValue.ofNumber(0.0),
      "p2", NodeValue.ofNumber(1.0),
      "p3", NodeValue.ofNumber(1.0),
      "t", NodeValue.ofNumber(0.5)
    );
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
