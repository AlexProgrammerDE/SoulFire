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

public enum ParkourDirection {
  NORTH,
  SOUTH,
  EAST,
  WEST;

  public static final ParkourDirection[] VALUES = values();

  @SuppressWarnings("DuplicatedCode")
  public SFVec3i offset(SFVec3i vector) {
    return switch (this) {
      case NORTH -> vector.add(0, 0, -1);
      case SOUTH -> vector.add(0, 0, 1);
      case EAST -> vector.add(1, 0, 0);
      case WEST -> vector.add(-1, 0, 0);
    };
  }

  public SkyDirection toSkyDirection() {
    return switch (this) {
      case NORTH -> SkyDirection.NORTH;
      case SOUTH -> SkyDirection.SOUTH;
      case EAST -> SkyDirection.EAST;
      case WEST -> SkyDirection.WEST;
    };
  }
}
