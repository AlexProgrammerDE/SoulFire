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

import com.soulfiremc.server.pathfinding.SFVec3i;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum MovementDirection {
  NORTH(new SFVec3i(0, 0, -1), SkyDirection.NORTH, null, ActionDirection.NORTH),
  SOUTH(new SFVec3i(0, 0, 1), SkyDirection.SOUTH, null, ActionDirection.SOUTH),
  EAST(new SFVec3i(1, 0, 0), SkyDirection.EAST, null, ActionDirection.EAST),
  WEST(new SFVec3i(-1, 0, 0), SkyDirection.WEST, null, ActionDirection.WEST),
  NORTH_EAST(new SFVec3i(1, 0, -1), null, DiagonalDirection.NORTH_EAST, ActionDirection.NORTH_EAST),
  NORTH_WEST(new SFVec3i(-1, 0, -1), null, DiagonalDirection.NORTH_WEST, ActionDirection.NORTH_WEST),
  SOUTH_EAST(new SFVec3i(1, 0, 1), null, DiagonalDirection.SOUTH_EAST, ActionDirection.SOUTH_EAST),
  SOUTH_WEST(new SFVec3i(-1, 0, 1), null, DiagonalDirection.SOUTH_WEST, ActionDirection.SOUTH_WEST);

  public static final MovementDirection[] VALUES = values();

  static {
    NORTH.opposite = SOUTH;
    SOUTH.opposite = NORTH;
    EAST.opposite = WEST;
    WEST.opposite = EAST;
    NORTH_EAST.opposite = SOUTH_WEST;
    NORTH_WEST.opposite = SOUTH_EAST;
    SOUTH_EAST.opposite = NORTH_WEST;
    SOUTH_WEST.opposite = NORTH_EAST;
  }

  @Getter
  private final SFVec3i offsetVector;
  private final SkyDirection skyDirection;
  private final DiagonalDirection diagonalDirection;
  @Getter
  private final ActionDirection actionDirection;
  @Getter
  private MovementDirection opposite;

  public SFVec3i offset(SFVec3i vector) {
    return vector.add(offsetVector);
  }

  public SkyDirection toSkyDirection() {
    if (skyDirection == null) {
      throw new IllegalStateException("Unexpected value: " + this);
    }

    return skyDirection;
  }

  public DiagonalDirection toDiagonalDirection() {
    if (diagonalDirection == null) {
      throw new IllegalStateException("Unexpected value: " + this);
    }

    return diagonalDirection;
  }

  public boolean isDiagonal() {
    return diagonalDirection != null;
  }
}
