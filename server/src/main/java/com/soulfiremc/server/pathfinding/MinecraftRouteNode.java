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
import com.soulfiremc.server.pathfinding.graph.actions.movement.ActionDirection;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Getter
@ToString
@AllArgsConstructor
public final class MinecraftRouteNode implements Comparable<MinecraftRouteNode> {
  /**
   * The world state of this node.
   */
  private final NodeState node;

  /**
   * The currently best known node to this node.
   */
  private @Nullable MinecraftRouteNode parent;

  /**
   * The direction from the parent to this node.
   */
  private @Nullable ActionDirection parentToNodeDirection;

  /**
   * The actions from the previous node to this node that were used to get to this node.
   */
  private List<WorldAction> actions;

  /**
   * The cost of the route from the start node to this node.
   */
  private double sourceCost;

  /**
   * The cost of the route from this node to the target.
   */
  private double targetCost;

  /**
   * The estimated cost of the route from this node to the target + the source cost.
   */
  private double totalRouteScore;

  public MinecraftRouteNode(NodeState node, List<WorldAction> actions,
                            double sourceCost, double targetCost,
                            double totalRouteScore) {
    this(
      node,
      null,
      null,
      actions,
      sourceCost,
      targetCost,
      totalRouteScore
    );
  }

  @Override
  public int compareTo(MinecraftRouteNode other) {
    return Double.compare(this.totalRouteScore, other.totalRouteScore);
  }

  public void setBetterParent(MinecraftRouteNode parent,
                              ActionDirection moveDirection,
                              List<WorldAction> actions,
                              double sourceCost, double targetCost,
                              double totalRouteScore) {
    this.parent = parent;
    this.parentToNodeDirection = moveDirection;
    this.actions = actions;
    this.sourceCost = sourceCost;
    this.targetCost = targetCost;
    this.totalRouteScore = totalRouteScore;
  }
}
