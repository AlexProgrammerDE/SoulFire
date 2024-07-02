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
import com.soulfiremc.server.pathfinding.execution.BlockPlaceAction;
import com.soulfiremc.server.pathfinding.execution.WorldAction;
import com.soulfiremc.server.pathfinding.graph.MinecraftGraph;
import java.util.List;

// TODO: Extract into having more fine behaviour control
public record PlaceBlockGoal(SFVec3i goal) implements GoalScorer {
  public PlaceBlockGoal(int x, int y, int z) {
    this(new SFVec3i(x, y, z));
  }

  @Override
  public double computeScore(MinecraftGraph graph, SFVec3i blockPosition, List<WorldAction> actions) {
    return blockPosition.distance(goal);
  }

  @Override
  public boolean isFinished(MinecraftRouteNode current) {
    for (var action : current.actions()) {
      if (action instanceof BlockPlaceAction placeBlockAction && placeBlockAction.blockPosition().equals(goal)) {
        return true;
      }
    }

    return false;
  }
}
