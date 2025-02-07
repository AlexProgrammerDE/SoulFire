/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.pathfinding.goals;

import com.soulfiremc.server.pathfinding.Costs;
import com.soulfiremc.server.pathfinding.MinecraftRouteNode;
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.pathfinding.execution.WorldAction;
import com.soulfiremc.server.pathfinding.graph.MinecraftGraph;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;

public record PosGoal(SFVec3i goal) implements GoalScorer {
  @VisibleForTesting
  public PosGoal(int x, int y, int z) {
    this(SFVec3i.from(x, y, z));
  }

  @Override
  public double computeScore(MinecraftGraph graph, SFVec3i blockPosition, List<WorldAction> actions) {
    var score = Math.sqrt(Math.pow(blockPosition.x - goal.x, 2) + Math.pow(blockPosition.z - goal.z, 2));
    var yDiff = blockPosition.y - goal.y;
    var yAbsolute = Math.abs(yDiff);
    if (yDiff > 0) {
      score += yAbsolute * Costs.JUMP_UP_BLOCK;
    } else if (yDiff < 0) {
      score += yAbsolute * Costs.FALL_1;
    }

    return score;
  }

  @Override
  public boolean isFinished(MinecraftRouteNode current) {
    return current.node().blockPosition().equals(goal);
  }
}
