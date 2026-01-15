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
package com.soulfiremc.server.pathfinding.graph.actions.movement;

import com.soulfiremc.server.pathfinding.SFVec3i;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum MovementModifier {
  NORMAL,
  JUMP_UP_BLOCK,
  FALL_1,
  FALL_2,
  FALL_3;

  public static final MovementModifier[] VALUES = values();

  public SFVec3i offset(SFVec3i vector) {
    return switch (this) {
      case NORMAL -> vector;
      case JUMP_UP_BLOCK -> vector.add(0, 1, 0);
      case FALL_1 -> vector.sub(0, 1, 0);
      case FALL_2 -> vector.sub(0, 2, 0);
      case FALL_3 -> vector.sub(0, 3, 0);
    };
  }

  public SFVec3i offsetIfJump(SFVec3i vector) {
    return this == MovementModifier.JUMP_UP_BLOCK ? vector.add(0, 1, 0) : vector;
  }
}
