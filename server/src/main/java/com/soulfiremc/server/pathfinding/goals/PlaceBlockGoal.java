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

import com.soulfiremc.server.data.BlockType;
import com.soulfiremc.server.pathfinding.BotEntityState;
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.pathfinding.graph.MinecraftGraph;

// TODO: Extract into having more fine behaviour control
public record PlaceBlockGoal(SFVec3i goal, BlockType blockType) implements GoalScorer {
  public PlaceBlockGoal(int x, int y, int z, BlockType blockType) {
    this(new SFVec3i(x, y, z), blockType);
  }

  @Override
  public double computeScore(MinecraftGraph graph, BotEntityState entityState) {
    return entityState.blockPosition().distance(goal);
  }

  @Override
  public boolean isFinished(BotEntityState entityState) {
    return entityState.levelState().isChanged(goal);
  }
}
