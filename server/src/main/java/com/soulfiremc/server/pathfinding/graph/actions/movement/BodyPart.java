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
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum BodyPart {
  HEAD {
    @Override
    public SFVec3i offset(SFVec3i position) {
      return position.add(0, 1, 0);
    }
  },
  FEET {
    @Override
    public SFVec3i offset(SFVec3i position) {
      return position;
    }
  };

  public static final BodyPart[] VALUES = values();

  public abstract SFVec3i offset(SFVec3i position);
}
