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
import com.soulfiremc.server.pathfinding.graph.BlockFace;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;

@Getter
@RequiredArgsConstructor
public enum SkyDirection {
  NORTH(new SFVec3i(0, 0, -1), Direction.NORTH, BlockFace.NORTH),
  SOUTH(new SFVec3i(0, 0, 1), Direction.SOUTH, BlockFace.SOUTH),
  EAST(new SFVec3i(1, 0, 0), Direction.EAST, BlockFace.EAST),
  WEST(new SFVec3i(-1, 0, 0), Direction.WEST, BlockFace.WEST);

  public static final SkyDirection[] VALUES = values();
  private final SFVec3i offsetVector;
  private final Direction direction;
  private final BlockFace blockFace;

  public SFVec3i offset(SFVec3i vector) {
    return vector.add(offsetVector);
  }

  public SkyDirection opposite() {
    return switch (this) {
      case NORTH -> SOUTH;
      case SOUTH -> NORTH;
      case EAST -> WEST;
      case WEST -> EAST;
    };
  }
}
