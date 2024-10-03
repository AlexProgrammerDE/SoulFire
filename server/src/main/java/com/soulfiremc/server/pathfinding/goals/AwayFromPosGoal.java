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

/**
 * Goal to get minRadius away from the origin.
 * Usually used for anti-afk where a player should move x blocks away from where they are.
 *
 * @param origin    the origin to move away from
 * @param minRadius the minimum radius to move away from the origin
 */
public record AwayFromPosGoal(SFVec3i origin, int minRadius) implements GoalScorer {
  @Override
  public double computeScore(MinecraftGraph graph, SFVec3i blockPosition, List<WorldAction> actions) {
    return Math.max(0, minRadius - blockPosition.distance(origin));
  }

  @Override
  public boolean isFinished(MinecraftRouteNode current) {
    return current.node().blockPosition().distance(origin) >= minRadius;
  }
}
