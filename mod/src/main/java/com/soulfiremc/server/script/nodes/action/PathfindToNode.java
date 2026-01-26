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
import com.soulfiremc.server.pathfinding.goals.PosGoal;
import com.soulfiremc.server.pathfinding.graph.constraint.PathConstraintImpl;
import com.soulfiremc.server.script.AbstractScriptNode;
import com.soulfiremc.server.script.ScriptContext;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Action node that pathfinds the bot to a target position.
/// This is an async operation that completes when the bot reaches the destination.
/// Inputs: x, y, z (target coordinates)
/// Output: success (boolean)
public final class PathfindToNode extends AbstractScriptNode {
  public static final String TYPE = "action.pathfind_to";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public Map<String, Object> getDefaultInputs() {
    return Map.of("x", 0, "y", 64, "z", 0);
  }

  @Override
  public CompletableFuture<Map<String, Object>> execute(ScriptContext context, Map<String, Object> inputs) {
    var bot = requireBot(inputs, context);
    var x = getIntInput(inputs, "x", 0);
    var y = getIntInput(inputs, "y", 64);
    var z = getIntInput(inputs, "z", 0);

    var goal = new PosGoal(SFVec3i.from(x, y, z));
    var constraint = new PathConstraintImpl(bot);

    var future = PathExecutor.executePathfinding(bot, goal, constraint)
      .thenApply(v -> result("success", true))
      .exceptionally(e -> result("success", false));

    // Track pending operation for cleanup on deactivation
    context.addPendingOperation(future);
    return future;
  }
}
