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

import com.github.steveice10.mc.protocol.data.game.entity.object.Direction;
import com.soulfiremc.server.pathfinding.SFVec3i;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;

public enum BlockFace {
  NORTH,
  SOUTH,
  EAST,
  WEST,
  TOP,
  BOTTOM;

  public static final BlockFace[] VALUES = values();

  public Vector3i offset(Vector3i vector) {
    return switch (this) {
      case NORTH -> vector.add(0, 0, -1);
      case SOUTH -> vector.add(0, 0, 1);
      case EAST -> vector.add(1, 0, 0);
      case WEST -> vector.add(-1, 0, 0);
      case TOP -> vector.add(0, 1, 0);
      case BOTTOM -> vector.add(0, -1, 0);
    };
  }

  public Vector3d getMiddleOfFace(SFVec3i block) {
    var doubleBlock = block.toVector3d();
    return switch (this) {
      case NORTH -> doubleBlock.add(0.5, 0.5, 0);
      case SOUTH -> doubleBlock.add(0.5, 0.5, 1);
      case EAST -> doubleBlock.add(1, 0.5, 0.5);
      case WEST -> doubleBlock.add(0, 0.5, 0.5);
      case TOP -> doubleBlock.add(0.5, 1, 0.5);
      case BOTTOM -> doubleBlock.add(0.5, 0, 0.5);
    };
  }

  public Direction toDirection() {
    return switch (this) {
      case NORTH -> Direction.NORTH;
      case SOUTH -> Direction.SOUTH;
      case EAST -> Direction.EAST;
      case WEST -> Direction.WEST;
      case TOP -> Direction.UP;
      case BOTTOM -> Direction.DOWN;
    };
  }
}
