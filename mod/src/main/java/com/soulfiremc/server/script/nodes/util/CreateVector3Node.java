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
import reactor.core.publisher.Mono;

import java.util.Map;

/// Utility node that creates a Vec3 from x, y, z components.
/// Inputs: x, y, z
/// Output: vector
public final class CreateVector3Node extends AbstractScriptNode {
  public static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("util.create_vector3")
    .displayName("Create Vector3")
    .category(CategoryRegistry.UTILITY)
    .addInputs(
      PortDefinition.inputWithDefault("x", "X", PortType.NUMBER, "0", "X component"),
      PortDefinition.inputWithDefault("y", "Y", PortType.NUMBER, "0", "Y component"),
      PortDefinition.inputWithDefault("z", "Z", PortType.NUMBER, "0", "Z component")
    )
    .addOutputs(
      PortDefinition.output("vector", "Vector", PortType.VECTOR3, "Created 3D vector")
    )
    .description("Creates a 3D vector from x, y, z components")
    .icon("box")
    .color("#9C27B0")
    .addKeywords("vector", "vec3", "position", "coordinates", "xyz", "create")
    .build();

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var x = getDoubleInput(inputs, "x", 0.0);
    var y = getDoubleInput(inputs, "y", 0.0);
    var z = getDoubleInput(inputs, "z", 0.0);
    return completedMono(result("vector", new Vec3(x, y, z)));
  }
}
