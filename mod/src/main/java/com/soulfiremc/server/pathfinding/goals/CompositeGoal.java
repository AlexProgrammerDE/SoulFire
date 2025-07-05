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

import com.soulfiremc.server.pathfinding.MinecraftRouteNode;
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.pathfinding.execution.WorldAction;
import com.soulfiremc.server.pathfinding.graph.MinecraftGraph;

import java.util.List;
import java.util.Set;

/**
 * Goal that is a composite of multiple goals.
 * This goal is completed if any of the goals are completed.
 * The closest goal is always chosen as the current goal.
 *
 * @param goals the goals to composite
 */
public record CompositeGoal(Set<GoalScorer> goals) implements GoalScorer {
  @Override
  public double computeScore(MinecraftGraph graph, SFVec3i blockPosition, List<WorldAction> actions) {
    return goals.stream().mapToDouble(goal -> goal.computeScore(graph, blockPosition, actions))
      .min().orElseThrow(() -> new IllegalStateException("No goals provided"));
  }

  @Override
  public boolean isFinished(MinecraftRouteNode current) {
    return goals.stream().anyMatch(goal -> goal.isFinished(current));
  }

  public GoalScorer getFinishedGoal(MinecraftRouteNode current) {
    return goals.stream().filter(goal -> goal.isFinished(current))
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("No goals finished"));
  }
}
