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
import com.soulfiremc.server.pathfinding.SFVec3i;
import org.cloudburstmc.math.vector.Vector2d;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;

public class VectorHelper {
  public static final int PACKED_HORIZONTAL_LENGTH = 1 + MathHelper.log2(MathHelper.smallestEncompassingPowerOfTwo(30000000));
  public static final int PACKED_Y_LENGTH = 64 - 2 * PACKED_HORIZONTAL_LENGTH;
  private static final long PACKED_Y_MASK = (1L << PACKED_Y_LENGTH) - 1L;
  private static final int Z_OFFSET = PACKED_Y_LENGTH;
  private static final int X_OFFSET = PACKED_Y_LENGTH + PACKED_HORIZONTAL_LENGTH;
  public static final int MAX_HORIZONTAL_COORDINATE = (1 << PACKED_HORIZONTAL_LENGTH) / 2 - 1;
  private static final long PACKED_X_MASK = (1L << PACKED_HORIZONTAL_LENGTH) - 1L;
  private static final long PACKED_Z_MASK = (1L << PACKED_HORIZONTAL_LENGTH) - 1L;
  private static final int Y_OFFSET = 0;

  private VectorHelper() {}

  public static Vector2d toVector2dXZ(Vector3d vector) {
    return Vector2d.from(vector.getX(), vector.getZ());
  }

  public static Vector3d topMiddleOfBlock(SFVec3i vector, BlockState blockState) {
    return topMiddleOfBlock(vector, blockState.collisionShape());
  }

  public static Vector3d topMiddleOfBlock(SFVec3i vector, BlockShapeGroup blockShapeGroup) {
    return Vector3d.from(
      vector.x + 0.5,
      vector.y + blockShapeGroup.blockShapes().stream().mapToDouble(a -> a.maxY).max().orElse(0),
      vector.z + 0.5
    );
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

  public static double horizontalDistance(Vector3d vec) {
    return Math.sqrt(vec.getX() * vec.getX() + vec.getZ() * vec.getZ());
  }

  public static double horizontalDistanceSqr(Vector3d vec) {
    return vec.getX() * vec.getX() + vec.getZ() * vec.getZ();
  }

  public static long asLong(Vector3i vec) {
    return asLong(vec.getX(), vec.getY(), vec.getZ());
  }

  public static long asLong(int x, int y, int z) {
    var l = 0L;
    l |= ((long) x & PACKED_X_MASK) << X_OFFSET;
    l |= ((long) y & PACKED_Y_MASK) << Y_OFFSET;
    return l | ((long) z & PACKED_Z_MASK) << Z_OFFSET;
  }
}
