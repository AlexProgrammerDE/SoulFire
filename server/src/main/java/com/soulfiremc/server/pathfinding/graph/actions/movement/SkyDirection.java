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
  NORTH(Direction.NORTH),
  SOUTH(Direction.SOUTH),
  EAST(Direction.EAST),
  WEST(Direction.WEST);

  public static final SkyDirection[] VALUES = values();
  private final Direction direction;

  @SuppressWarnings("DuplicatedCode")
  public SFVec3i offset(SFVec3i vector) {
    return switch (this) {
      case NORTH -> vector.add(0, 0, -1);
      case SOUTH -> vector.add(0, 0, 1);
      case EAST -> vector.add(1, 0, 0);
      case WEST -> vector.add(-1, 0, 0);
    };
  }

  public SkyDirection opposite() {
    return switch (this) {
      case NORTH -> SOUTH;
      case SOUTH -> NORTH;
      case EAST -> WEST;
      case WEST -> EAST;
    };
  }

  public SkyDirection leftSide() {
    return switch (this) {
      case NORTH -> WEST;
      case SOUTH -> EAST;
      case EAST -> NORTH;
      case WEST -> SOUTH;
    };
  }

  public SkyDirection rightSide() {
    return switch (this) {
      case NORTH -> EAST;
      case SOUTH -> WEST;
      case EAST -> SOUTH;
      case WEST -> NORTH;
    };
  }

  public BlockFace toBlockFace() {
    return switch (this) {
      case NORTH -> BlockFace.NORTH;
      case SOUTH -> BlockFace.SOUTH;
      case EAST -> BlockFace.EAST;
      case WEST -> BlockFace.WEST;
    };
  }
}
