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
package com.soulfiremc.server.data;

import com.soulfiremc.server.protocol.bot.state.Level;
import org.cloudburstmc.math.vector.Vector3i;

public record FluidState(
  FluidType type,
  int amount,
  float ownHeight,
  boolean source,
  boolean empty
) {
  private boolean hasSameAbove(Level level, Vector3i blockPos) {
    return type.equals(level.getBlockState(blockPos.add(0, 1, 0)).fluidState().type());
  }

  public float getHeight(Level level, Vector3i blockPos) {
    if (type == FluidType.EMPTY) {
      return 0.0F;
    } else {
      return hasSameAbove(level, blockPos) ? 1.0F : ownHeight;
    }
  }
}
