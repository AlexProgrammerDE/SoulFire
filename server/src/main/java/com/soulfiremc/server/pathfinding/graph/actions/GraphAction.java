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

/**
 * A calculated action that the bot can take on a graph world representation.
 */
public abstract sealed class GraphAction
  permits SimpleMovement, ParkourMovement, UpMovement, DownMovement {
  private int subscriptionCounter;

  public void subscribe() {
    // Shall only be called in the precautions of the graph action
    subscriptionCounter++;
  }

  public boolean decrementAndIsDone() {
    // Check if this action has all subscriptions fulfilled
    return --subscriptionCounter == 0;
  }

  // A step further than isImpossible, for block placing this also considers no block
  // to place against found.
  public boolean impossibleToComplete() {
    return false;
  }

  public abstract GraphInstructions getInstructions(SFVec3i node);

  public abstract GraphAction copy();
}
