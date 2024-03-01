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
package com.soulfiremc.server.util;

import com.soulfiremc.server.data.BlockShapeGroup;
import com.soulfiremc.server.data.BlockState;
import org.cloudburstmc.math.vector.Vector3d;

public class VectorHelper {
  private VectorHelper() {}

  public static Vector3d topMiddleOfBlock(Vector3d vector, BlockState blockState) {
    return topMiddleOfBlock(vector, blockState.blockShapeGroup());
  }

  public static Vector3d topMiddleOfBlock(Vector3d vector, BlockShapeGroup blockShapeGroup) {
    return vector.floor().add(0.5, blockShapeGroup.highestY(), 0.5);
  }
}
