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

import com.soulfiremc.server.script.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Constant node that outputs a configurable 3D vector (x, y, z).
/// Outputs: x, y, z (number)
public final class Vector3ConstantNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("constant.vector3")
    .displayName("Vector3")
    .category(NodeCategory.CONSTANTS)
    .addInputs(
      PortDefinition.inputWithDefault("x", "X", PortType.NUMBER, "0", "X component"),
      PortDefinition.inputWithDefault("y", "Y", PortType.NUMBER, "0", "Y component"),
      PortDefinition.inputWithDefault("z", "Z", PortType.NUMBER, "0", "Z component")
    )
    .addOutputs(
      PortDefinition.output("x", "X", PortType.NUMBER, "X component"),
      PortDefinition.output("y", "Y", PortType.NUMBER, "Y component"),
      PortDefinition.output("z", "Z", PortType.NUMBER, "Z component")
    )
    .description("Outputs a constant 3D vector with x, y, z components")
    .icon("move-3d")
    .color("#9C27B0")
    .addKeywords("vector", "vector3", "xyz", "position", "coordinate", "constant")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var x = getDoubleInput(inputs, "x", 0.0);
    var y = getDoubleInput(inputs, "y", 0.0);
    var z = getDoubleInput(inputs, "z", 0.0);
    return completed(results("x", x, "y", y, "z", z));
  }
}
