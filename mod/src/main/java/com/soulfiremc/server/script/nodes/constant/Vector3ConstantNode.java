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
package com.soulfiremc.server.script.nodes.constant;

import com.soulfiremc.server.script.AbstractScriptNode;
import com.soulfiremc.server.script.ScriptContext;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Constant node that outputs a configurable 3D vector (x, y, z).
/// Outputs: x, y, z (number)
public final class Vector3ConstantNode extends AbstractScriptNode {
  public static final String TYPE = "constant.vector3";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public Map<String, Object> getDefaultInputs() {
    return Map.of("x", 0.0, "y", 0.0, "z", 0.0);
  }

  @Override
  public CompletableFuture<Map<String, Object>> execute(ScriptContext context, Map<String, Object> inputs) {
    var x = getDoubleInput(inputs, "x", 0.0);
    var y = getDoubleInput(inputs, "y", 0.0);
    var z = getDoubleInput(inputs, "z", 0.0);
    return completed(results("x", x, "y", y, "z", z));
  }
}
