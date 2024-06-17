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
@Setter
@ToString
@AllArgsConstructor
public class MinecraftRouteNode implements Comparable<MinecraftRouteNode> {
  /**
   * The world state of this node.
   */
  private final NodeState node;

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
  private double sourceCost;

  /**
   * The estimated cost of the route from this node to the target.
   */
  private double totalRouteScore;

  private List<MinecraftRouteNode> children;

  public MinecraftRouteNode(NodeState node, MinecraftRouteNode parent, List<WorldAction> actions,
                            double sourceCost, double totalRouteScore) {
    this.node = node;
    this.parent = parent;
    this.actions = actions;
    this.sourceCost = sourceCost;
    this.totalRouteScore = totalRouteScore;
    this.children = new ArrayList<>();
  }

  public MinecraftRouteNode(NodeState node, List<WorldAction> actions,
                            double sourceCost, double totalRouteScore) {
    this.node = node;
    this.parent = null;
    this.actions = actions;
    this.sourceCost = sourceCost;
    this.totalRouteScore = totalRouteScore;
    this.children = new ArrayList<>();
  }

  @Override
  public int compareTo(MinecraftRouteNode other) {
    return Double.compare(this.totalRouteScore, other.totalRouteScore);
  }
}
