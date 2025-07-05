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
package com.soulfiremc.server.pathfinding.graph.actions;

import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.pathfinding.graph.GraphInstructions;
import com.soulfiremc.server.pathfinding.graph.MinecraftGraph;
import com.soulfiremc.server.pathfinding.graph.actions.movement.ActionDirection;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * A calculated action that the bot can take on a graph world representation.
 */
@Setter
@RequiredArgsConstructor
public abstract sealed class GraphAction
  permits DownMovement, ParkourMovement, SimpleMovement, UpMovement {
  public final ActionDirection actionDirection;
  private int subscriptionCounter;

  public boolean decrementAndIsDone() {
    // Check if this action has all subscriptions fulfilled
    return --subscriptionCounter == 0;
  }

  public abstract List<GraphInstructions> getInstructions(MinecraftGraph graph, SFVec3i node);

  public abstract GraphAction copy();
}
