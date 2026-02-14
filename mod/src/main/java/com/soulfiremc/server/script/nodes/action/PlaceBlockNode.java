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
package com.soulfiremc.server.script.nodes.action;

import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.pathfinding.execution.PathExecutor;
import com.soulfiremc.server.pathfinding.goals.PlaceBlockGoal;
import com.soulfiremc.server.pathfinding.graph.constraint.PathConstraintImpl;
import com.soulfiremc.server.script.*;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Action node that places a block at a specified position.
/// This is an async operation that pathfinds to the location and places a block.
/// Inputs: position (Vec3 target block coordinates)
/// Output: success (boolean)
public final class PlaceBlockNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
          .type("action.place_block")
          .displayName("Place Block")
          .category(CategoryRegistry.ACTIONS)
          .addInputs(
                  PortDefinition.execIn(),
                  PortDefinition.input("position", "Position", PortType.VECTOR3, "Block position to place at")
          )
          .addOutputs(
                  PortDefinition.execOut(),
                  PortDefinition.output("success", "Success", PortType.BOOLEAN, "Whether the block was placed")
          )
          .description("Pathfinds to and places a block at the specified position")
          .icon("box")
          .color("#FF9800")
          .addKeywords("place", "build", "put", "block")
          .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var bot = requireBot(inputs);
    var position = getInput(inputs, "position", Vec3.ZERO);

    var goal = new PlaceBlockGoal(SFVec3i.fromDouble(position));
    var constraint = new PathConstraintImpl(bot);

    return PathExecutor.executePathfinding(bot, goal, constraint)
      .thenApply(_ -> result("success", true))
      .exceptionally(_ -> result("success", false));
  }
}
