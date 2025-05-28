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

@Getter
@RequiredArgsConstructor
public enum DiagonalDirection {
  NORTH_EAST(new BlockPos(1, 0, -1), SkyDirection.NORTH, SkyDirection.EAST),
  NORTH_WEST(new BlockPos(-1, 0, -1), SkyDirection.NORTH, SkyDirection.WEST),
  SOUTH_EAST(new BlockPos(1, 0, 1), SkyDirection.SOUTH, SkyDirection.EAST),
  SOUTH_WEST(new BlockPos(-1, 0, 1), SkyDirection.SOUTH, SkyDirection.WEST);

  public static final DiagonalDirection[] VALUES = values();
  private final BlockPos offsetVector;
  private final SkyDirection leftSide;
  private final SkyDirection rightSide;

  public BlockPos offset(BlockPos vector) {
    return vector.offset(offsetVector);
  }

  public SkyDirection side(MovementSide side) {
    if (side == MovementSide.LEFT) {
      return leftSide;
    } else {
      return rightSide;
    }
  }
}
