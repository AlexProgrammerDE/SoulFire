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
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Math node that calculates the distance between two 3D points.
/// Inputs: a (Vec3), b (Vec3)
/// Output: distance
public final class DistanceNode extends AbstractScriptNode {
  public static final String TYPE = "math.distance";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public Map<String, NodeValue> getDefaultInputs() {
    var zeroVec = NodeValue.ofList(List.of(
      NodeValue.ofNumber(0.0), NodeValue.ofNumber(0.0), NodeValue.ofNumber(0.0)
    ));
    return Map.of("a", zeroVec, "b", zeroVec);
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(ScriptContext context, Map<String, NodeValue> inputs) {
    var a = getInput(inputs, "a", Vec3.ZERO);
    var b = getInput(inputs, "b", Vec3.ZERO);
    var distance = a.distanceTo(b);
    return completed(result("distance", distance));
  }
}
