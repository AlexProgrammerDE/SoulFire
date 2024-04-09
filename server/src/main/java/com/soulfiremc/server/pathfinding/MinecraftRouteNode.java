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
package com.soulfiremc.server.pathfinding;

import com.soulfiremc.server.pathfinding.execution.WorldAction;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public class MinecraftRouteNode implements Comparable<MinecraftRouteNode> {
  /**
   * The world state of this node.
   */
  private final SFVec3i blockPosition;

  /**
   * The currently best known node to this node.
   */
  private MinecraftRouteNode parent;

  /**
   * The actions from the previous node to this node that were used to get to this node.
   */
  private List<WorldAction> actions;

  /**
   * The cost of the route from the start node to this node.
   */
  @Setter
  private double sourceCost;

  /**
   * The estimated cost of the route from this node to the target.
   */
  @Setter
  private double totalRouteScore;

  private List<MinecraftRouteNode> children;

  private BotEntityState predictedState;

  private boolean predicatedStateValid;

  private boolean predicting;

  public MinecraftRouteNode(SFVec3i blockPosition, MinecraftRouteNode parent, List<WorldAction> actions,
                            double sourceCost, double totalRouteScore) {
    this.blockPosition = blockPosition;
    this.parent = parent;
    this.actions = actions;
    this.sourceCost = sourceCost;
    this.totalRouteScore = totalRouteScore;
    this.children = new ArrayList<>();

    predictState();
  }

  public MinecraftRouteNode(SFVec3i blockPosition, BotEntityState entityState, List<WorldAction> actions,
                            double sourceCost, double totalRouteScore) {
    this.blockPosition = blockPosition;
    this.parent = null;
    this.actions = actions;
    this.sourceCost = sourceCost;
    this.totalRouteScore = totalRouteScore;
    this.children = new ArrayList<>();

    this.predictedState = entityState;
    this.predicatedStateValid = true;
  }

  public void predictState() {
    if (predicting) {
      // Prevent recursion
      return;
    }

    predicting = true;

    if (!parent.predicatedStateValid) {
      predicatedStateValid = false;
    } else {
      var currentEntityState = parent.predictedState;
      for (var action : actions) {
        currentEntityState = action.simulate(currentEntityState);
      }

      predictedState = currentEntityState;
      predicatedStateValid = predictedState.inventory().isValid();
    }

    // Update children whose state depends on this node
    for (var child : children) {
      child.predictState();
    }

    predicting = false;
  }

  public void parent(MinecraftRouteNode parent) {
    this.parent.children.remove(this);
    this.parent = parent;
    this.parent.children.add(this);
  }

  public void actions(List<WorldAction> actions) {
    this.actions = actions;
    predictState();
  }

  @Override
  public int compareTo(MinecraftRouteNode other) {
    return Double.compare(this.totalRouteScore, other.totalRouteScore);
  }
}
