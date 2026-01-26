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
import com.soulfiremc.server.script.ScriptContext;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Math node that performs linear interpolation between two values.
/// Inputs: a (start), b (end), t (interpolation factor 0-1)
/// Output: result = a + (b - a) * t
public final class LerpNode extends AbstractScriptNode {
  public static final String TYPE = "math.lerp";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public Map<String, NodeValue> getDefaultInputs() {
    return Map.of("a", NodeValue.ofNumber(0.0), "b", NodeValue.ofNumber(1.0), "t", NodeValue.ofNumber(0.5));
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(ScriptContext context, Map<String, NodeValue> inputs) {
    var a = getDoubleInput(inputs, "a", 0.0);
    var b = getDoubleInput(inputs, "b", 1.0);
    var t = getDoubleInput(inputs, "t", 0.5);

    // Clamp t to [0, 1]
    t = Math.max(0.0, Math.min(1.0, t));

    var lerpedValue = a + (b - a) * t;
    return completed(result("result", lerpedValue));
  }
}
