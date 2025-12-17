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
package com.soulfiremc.server.pathfinding.world;

import com.soulfiremc.server.pathfinding.SFVec3i;

/// Provides access to blocks in the world for pathfinding purposes.
/// This is an abstraction that allows the pathfinding library to work
/// without depending on Minecraft-specific code.
public interface BlockAccessor {
  /// Gets the block state at the specified position.
  /// @param position the block position
  /// @return the block state at that position
  BlockState getBlockState(SFVec3i position);

  /// Gets the block state at the specified coordinates.
  /// @param x the x coordinate
  /// @param y the y coordinate
  /// @param z the z coordinate
  /// @return the block state at that position
  default BlockState getBlockState(int x, int y, int z) {
    return getBlockState(SFVec3i.from(x, y, z));
  }

  /// Returns true if the given Y coordinate is outside the build height.
  /// @param y the y coordinate
  /// @return true if outside build height
  boolean isOutsideBuildHeight(int y);
}
