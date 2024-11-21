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
import org.cloudburstmc.math.vector.Vector3i;

public class VectorHelper {
  private VectorHelper() {}

  public static Vector3d topMiddleOfBlock(Vector3d vector, BlockState blockState) {
    return topMiddleOfBlock(vector, blockState.blockCollisionShapeGroup());
  }

  public static Vector3d topMiddleOfBlock(Vector3d vector, BlockShapeGroup blockShapeGroup) {
    return vector.floor().add(0.5, blockShapeGroup.highestY(), 0.5);
  }

  public static Vector3d xRot(Vector3d base, float pitch) {
    var g = MathHelper.cos(pitch);
    var h = MathHelper.sin(pitch);
    var d = base.getX();
    var e = base.getY() * (double) g + base.getZ() * (double) h;
    var i = base.getZ() * (double) g - base.getY() * (double) h;
    return Vector3d.from(d, e, i);
  }

  public static Vector3d yRot(Vector3d base, float yaw) {
    var g = MathHelper.cos(yaw);
    var h = MathHelper.sin(yaw);
    var d = base.getX() * (double) g + base.getZ() * (double) h;
    var e = base.getY();
    var i = base.getZ() * (double) g - base.getX() * (double) h;
    return Vector3d.from(d, e, i);
  }

  public static Vector3d zRot(Vector3d base, float roll) {
    var g = MathHelper.cos(roll);
    var h = MathHelper.sin(roll);
    var d = base.getX() * (double) g + base.getY() * (double) h;
    var e = base.getY() * (double) g - base.getX() * (double) h;
    var i = base.getZ();
    return Vector3d.from(d, e, i);
  }

  public static Vector3d normalizeSafe(Vector3d vec) {
    var length = vec.length();
    return length < 1.0E-5F ? Vector3d.ZERO : Vector3d.from(vec.getX() / length, vec.getY() / length, vec.getZ() / length);
  }

  public static double distToCenterSqr(Vector3i current, Vector3d other) {
    var x = current.getX() + 0.5 - other.getX();
    var y = current.getY() + 0.5 - other.getY();
    var z = current.getZ() + 0.5 - other.getZ();
    return x * x + y * y + z * z;
  }
}
