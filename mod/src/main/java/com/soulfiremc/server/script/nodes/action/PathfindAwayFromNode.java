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
import com.soulfiremc.server.pathfinding.goals.AwayFromPosGoal;
import com.soulfiremc.server.pathfinding.graph.constraint.PathConstraintImpl;
import com.soulfiremc.server.script.*;
import net.minecraft.world.phys.Vec3;
import reactor.core.publisher.Mono;

import java.util.Map;

/// Action node that pathfinds the bot away from a position.
/// Uses AwayFromPosGoal to move at least the specified distance away.
public final class PathfindAwayFromNode extends AbstractScriptNode {
  public static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("action.pathfind_away_from")
    .displayName("Pathfind Away From")
    .category(CategoryRegistry.ACTIONS)
    .addInputs(
      PortDefinition.execIn(),
      PortDefinition.botIn(),
      PortDefinition.input("position", "Position", PortType.VECTOR3, "Position to move away from"),
      PortDefinition.inputWithDefault("distance", "Distance", PortType.NUMBER, "10", "Minimum distance to move away")
    )
    .addOutputs(
      PortDefinition.execOut(),
      PortDefinition.output(StandardPorts.EXEC_ERROR, "Error", PortType.EXEC, "Executes on failure"),
      PortDefinition.output("success", "Success", PortType.BOOLEAN, "Whether it succeeded"),
      PortDefinition.output("errorMessage", "Error Message", PortType.STRING, "Error details")
    )
    .description("Moves the bot away from a position using pathfinding")
    .icon("move-diagonal")
    .color("#FF9800")
    .addKeywords("pathfind", "away", "flee", "escape", "move", "afk")
    .build();

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var bot = requireBot(inputs);
    var position = getInput(inputs, "position", Vec3.ZERO);
    var distance = getIntInput(inputs, "distance", 10);

    var goal = new AwayFromPosGoal(SFVec3i.fromDouble(position), distance);
    var constraint = new PathConstraintImpl(bot);

    return Mono.fromFuture(PathExecutor.executePathfinding(bot, goal, constraint))
      .map(_ -> results(
        StandardPorts.EXEC_OUT, true,
        "success", true,
        "errorMessage", ""
      ))
      .onErrorResume(e -> completedMono(results(
        StandardPorts.EXEC_ERROR, true,
        "success", false,
        "errorMessage", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()
      )));
  }
}
