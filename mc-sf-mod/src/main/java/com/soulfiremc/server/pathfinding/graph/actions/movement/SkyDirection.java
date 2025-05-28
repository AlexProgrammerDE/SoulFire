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
package com.soulfiremc.server.pathfinding.graph.actions.movement;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

@Getter
@RequiredArgsConstructor
public enum SkyDirection {
  NORTH(new BlockPos(0, 0, -1), Direction.NORTH),
  SOUTH(new BlockPos(0, 0, 1), Direction.SOUTH),
  EAST(new BlockPos(1, 0, 0), Direction.EAST),
  WEST(new BlockPos(-1, 0, 0), Direction.WEST);

  public static final SkyDirection[] VALUES = values();

  static {
    // Enums can't reference each other in their constructors, so we have to do this manually
    NORTH.opposite = SOUTH;
    SOUTH.opposite = NORTH;
    EAST.opposite = WEST;
    WEST.opposite = EAST;
  }

  private final BlockPos offsetVector;
  private final Direction direction;
  private SkyDirection opposite;

  public BlockPos offset(BlockPos vector) {
    return vector.offset(offsetVector);
  }
}
