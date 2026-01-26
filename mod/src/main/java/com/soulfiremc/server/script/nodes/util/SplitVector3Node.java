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
package com.soulfiremc.server.script.nodes.util;

import com.soulfiremc.server.script.*;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Utility node that splits a Vec3 into x, y, z components.
/// Input: vector
/// Outputs: x, y, z
public final class SplitVector3Node extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("util.split_vector3")
    .displayName("Split Vector3")
    .category(CategoryRegistry.UTILITY)
    .addInputs(
      PortDefinition.input("vector", "Vector", PortType.VECTOR3, "3D vector to split")
    )
    .addOutputs(
      PortDefinition.output("x", "X", PortType.NUMBER, "X component"),
      PortDefinition.output("y", "Y", PortType.NUMBER, "Y component"),
      PortDefinition.output("z", "Z", PortType.NUMBER, "Z component")
    )
    .description("Splits a 3D vector into x, y, z components")
    .icon("scissors")
    .color("#9C27B0")
    .addKeywords("vector", "vec3", "position", "coordinates", "xyz", "split", "decompose")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var vector = getInput(inputs, "vector", Vec3.ZERO);
    return completed(results("x", vector.x(), "y", vector.y(), "z", vector.z()));
  }
}
