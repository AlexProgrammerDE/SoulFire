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

/// Math node that constrains a value within a range.
/// Inputs: value, min, max
/// Output: result = clamped value within [min, max]
public final class ClampNode extends AbstractScriptNode {
  public static final String TYPE = "math.clamp";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public Map<String, NodeValue> getDefaultInputs() {
    return Map.of("value", NodeValue.ofNumber(0.0), "min", NodeValue.ofNumber(0.0), "max", NodeValue.ofNumber(1.0));
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(ScriptContext context, Map<String, NodeValue> inputs) {
    var value = getDoubleInput(inputs, "value", 0.0);
    var min = getDoubleInput(inputs, "min", 0.0);
    var max = getDoubleInput(inputs, "max", 1.0);

    var clampedValue = Math.max(min, Math.min(max, value));
    return completed(result("result", clampedValue));
  }
}
