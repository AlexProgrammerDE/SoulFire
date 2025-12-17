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
package com.soulfiremc.server.pathfinding.graph;

import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.pathfinding.execution.WorldAction;
import com.soulfiremc.server.pathfinding.graph.actions.movement.ActionDirection;

import java.util.List;

/// Instructions for a single step in the pathfinding graph.
/// Contains the target position, cost, and actions to perform.
///
/// @param blockPosition The target block position for this step
/// @param deltaUsableBlockItems Change in usable block items after this step
/// @param requiresOneBlock Whether this step requires at least one block in inventory
/// @param moveDirection The direction of movement for this step
/// @param actionCost The cost of this step for pathfinding heuristics
/// @param actions The list of actions to perform for this step
public record GraphInstructions(
  SFVec3i blockPosition,
  int deltaUsableBlockItems,
  boolean requiresOneBlock,
  ActionDirection moveDirection,
  double actionCost,
  List<WorldAction> actions
) {
  /// Returns a copy of this instruction with a modified action cost.
  public GraphInstructions withActionCost(double newActionCost) {
    return new GraphInstructions(blockPosition, deltaUsableBlockItems, requiresOneBlock, moveDirection, newActionCost, actions);
  }
}
