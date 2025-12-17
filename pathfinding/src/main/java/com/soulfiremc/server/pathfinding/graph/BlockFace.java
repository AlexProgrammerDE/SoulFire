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

/// Represents a face of a block for pathfinding purposes.
public enum BlockFace {
  NORTH(0, 0, -1),
  SOUTH(0, 0, 1),
  EAST(1, 0, 0),
  WEST(-1, 0, 0),
  TOP(0, 1, 0),
  BOTTOM(0, -1, 0);

  public static final BlockFace[] VALUES = values();

  private final int offsetX;
  private final int offsetY;
  private final int offsetZ;

  BlockFace(int offsetX, int offsetY, int offsetZ) {
    this.offsetX = offsetX;
    this.offsetY = offsetY;
    this.offsetZ = offsetZ;
  }

  public SFVec3i offset(SFVec3i vector) {
    return vector.add(offsetX, offsetY, offsetZ);
  }

  public int getOffsetX() {
    return offsetX;
  }

  public int getOffsetY() {
    return offsetY;
  }

  public int getOffsetZ() {
    return offsetZ;
  }

  public double[] getMiddleOfFace(SFVec3i block) {
    return switch (this) {
      case NORTH -> new double[]{block.x + 0.5, block.y + 0.5, block.z};
      case SOUTH -> new double[]{block.x + 0.5, block.y + 0.5, block.z + 1};
      case EAST -> new double[]{block.x + 1, block.y + 0.5, block.z + 0.5};
      case WEST -> new double[]{block.x, block.y + 0.5, block.z + 0.5};
      case TOP -> new double[]{block.x + 0.5, block.y + 1, block.z + 0.5};
      case BOTTOM -> new double[]{block.x + 0.5, block.y, block.z + 0.5};
    };
  }
}
