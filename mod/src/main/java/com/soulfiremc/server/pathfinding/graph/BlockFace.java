/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.pathfinding.graph;

import com.soulfiremc.server.pathfinding.SFVec3i;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public enum BlockFace {
  NORTH,
  SOUTH,
  EAST,
  WEST,
  TOP,
  BOTTOM;

  public static final BlockFace[] VALUES = values();

  public BlockPos offset(BlockPos vector) {
    return switch (this) {
      case NORTH -> vector.offset(0, 0, -1);
      case SOUTH -> vector.offset(0, 0, 1);
      case EAST -> vector.offset(1, 0, 0);
      case WEST -> vector.offset(-1, 0, 0);
      case TOP -> vector.offset(0, 1, 0);
      case BOTTOM -> vector.offset(0, -1, 0);
    };
  }

  public Vec3 getMiddleOfFace(SFVec3i block) {
    var doubleBlock = block.toVec3();
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
