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
import com.soulfiremc.server.pathfinding.graph.actions.movement.ActionDirection;
import com.soulfiremc.server.pathfinding.world.BlockAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/// Represents the world as a graph of actions between blocks.
/// A node would be a block and the edges would be the actions that can be performed on that block.
public interface PathfindingGraph {
  /// Returns the number of possible actions from any given node.
  int actionsSize();

  /// Returns the block accessor for this graph.
  BlockAccessor blockAccessor();

  /// Returns the projected inventory for this graph.
  ProjectedInventory inventory();

  /// Returns the path constraint for this graph.
  PathConstraint pathConstraint();

  /// Generates possible actions from the given node position.
  /// @param node The starting position
  /// @param fromDirection The direction we came from (for optimization)
  /// @param callback Consumer to receive generated graph instructions
  void insertActions(SFVec3i node, @Nullable ActionDirection fromDirection, Consumer<GraphInstructions> callback);
}
