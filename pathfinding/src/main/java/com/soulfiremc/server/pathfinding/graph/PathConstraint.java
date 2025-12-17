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
import com.soulfiremc.server.pathfinding.world.BlockState;

/// Defines constraints and capabilities for pathfinding.
/// Implementations can control what actions are allowed during pathfinding.
@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public interface PathConstraint {
  /// Returns true if placing blocks decreases the usable block count.
  /// (False in creative mode)
  boolean doUsableBlocksDecreaseWhenPlaced();

  /// Returns true if breaking blocks can drop items.
  /// (False in creative mode)
  boolean canBlocksDropWhenBroken();

  /// Returns true if the pathfinder is allowed to break blocks.
  boolean canBreakBlocks();

  /// Returns true if the pathfinder is allowed to place blocks.
  boolean canPlaceBlocks();

  /// Returns true if the given Y coordinate is outside the level.
  boolean isOutOfLevel(BlockState blockState, SFVec3i pos);

  /// Returns true if the block at the given position can be broken.
  boolean canBreakBlock(SFVec3i pos, BlockState blockState);

  /// Returns true if a block can be placed at the given position.
  boolean canPlaceBlock(SFVec3i pos);

  /// Returns true if the entity would collide with the block during diagonal movement.
  boolean collidesWithAtEdge(CollisionData collisionData);

  /// Modifies the graph instructions as needed (e.g., adding penalties).
  GraphInstructions modifyAsNeeded(GraphInstructions instruction);

  /// Returns the cost penalty for breaking a block during pathfinding.
  double breakBlockPenalty();

  /// Returns the cost penalty for placing a block during pathfinding.
  double placeBlockPenalty();

  /// Returns the maximum time in seconds before pathfinding gives up.
  int expireTimeout();

  /// Returns whether pruning of the pathfinding search space is disabled.
  boolean disablePruning();
}
