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

import com.soulfiremc.server.pathfinding.cost.BlockMiningCosts;
import com.soulfiremc.server.pathfinding.world.BlockState;

/// Represents an abstracted inventory for pathfinding purposes.
/// Provides information about available blocks and tools for pathfinding decisions.
public interface ProjectedInventory {
  /// Returns the number of usable block items in the inventory.
  int usableBlockItems();

  /// Returns the mining costs for the given block state.
  /// @param blockState the block state to get mining costs for
  /// @return the mining costs, or null if the block cannot be mined
  BlockMiningCosts getMiningCosts(BlockState blockState);

  /// Returns true if the given block state is a stair block suitable for standing on.
  boolean isStairsBlockToStandOn(BlockState blockState);
}
