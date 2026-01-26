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
import com.soulfiremc.server.pathfinding.goals.BreakBlockPosGoal;
import com.soulfiremc.server.pathfinding.graph.constraint.PathConstraintImpl;
import com.soulfiremc.server.script.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Action node that breaks a block at a specified position.
/// This is an async operation that pathfinds to the block and breaks it.
/// Inputs: x, y, z (block coordinates)
/// Output: success (boolean)
public final class BreakBlockNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("action.break_block")
    .displayName("Break Block")
    .category(NodeCategory.ACTIONS)
    .addInputs(
      PortDefinition.execIn(),
      PortDefinition.input("bot", "Bot", PortType.BOT, "The bot to control"),
      PortDefinition.inputWithDefault("x", "X", PortType.NUMBER, "0", "Block X coordinate"),
      PortDefinition.inputWithDefault("y", "Y", PortType.NUMBER, "64", "Block Y coordinate"),
      PortDefinition.inputWithDefault("z", "Z", PortType.NUMBER, "0", "Block Z coordinate")
    )
    .addOutputs(
      PortDefinition.execOut(),
      PortDefinition.output("success", "Success", PortType.BOOLEAN, "Whether the block was broken")
    )
    .description("Pathfinds to and breaks a block at the specified position")
    .icon("pickaxe")
    .color("#FF9800")
    .addKeywords("break", "mine", "dig", "destroy", "block")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var bot = requireBot(inputs);
    var x = getIntInput(inputs, "x", 0);
    var y = getIntInput(inputs, "y", 64);
    var z = getIntInput(inputs, "z", 0);

    var goal = new BreakBlockPosGoal(SFVec3i.from(x, y, z));
    var constraint = new PathConstraintImpl(bot);

    var future = PathExecutor.executePathfinding(bot, goal, constraint)
      .thenApply(v -> result("success", true))
      .exceptionally(e -> result("success", false));

    // Track pending operation for cleanup on deactivation
    runtime.addPendingOperation(future);
    return future;
  }
}
