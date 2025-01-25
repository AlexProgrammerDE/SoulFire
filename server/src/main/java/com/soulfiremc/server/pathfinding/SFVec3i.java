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
package com.soulfiremc.server.pathfinding;

import com.soulfiremc.server.util.MathHelper;
import com.soulfiremc.server.util.VectorHelper;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;

/**
 * A simple 3D integer vector. This class is used instead of Vector3i because this uses direct field
 * access instead of getters. Even though the JIT compiler could optimize this, it's still faster to
 * use this class.
 */
@RequiredArgsConstructor
public final class SFVec3i {
  public static final SFVec3i ZERO = new SFVec3i(0, 0, 0);

  public final int x;
  public final int y;
  public final int z;
  private int hashCode;
  private boolean hashCodeSet;
  private long minecraftLong;
  private boolean minecraftLongSet;

  public static SFVec3i fromDouble(Vector3d vec) {
    return fromInt(vec.toInt());
  }

  public static SFVec3i fromInt(Vector3i vec) {
    return from(vec.getX(), vec.getY(), vec.getZ());
  }

  public static SFVec3i from(int x, int y, int z) {
    return new SFVec3i(x, y, z);
  }

  // Long hash as seen in baritone
  public static long longHash(int x, int y, int z) {
    long hash = 3241;
    hash = 3457689L * hash + x;
    hash = 8734625L * hash + y;
    hash = 2873465L * hash + z;
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof SFVec3i other)) {
      return false;
    }

    return this.x == other.x && this.y == other.y && this.z == other.z;
  }

  public boolean minecraftEquals(SFVec3i vec) {
    return asMinecraftLong() == vec.asMinecraftLong();
  }

  @Override
  public int hashCode() {
    if (!hashCodeSet) {
      hashCode = (int) longHash(x, y, z);
      hashCodeSet = true;
    }

    return hashCode;
  }

  public long asMinecraftLong() {
    if (!minecraftLongSet) {
      minecraftLong = VectorHelper.asLong(x, y, z);
      minecraftLongSet = true;
    }

    return minecraftLong;
  }

  public SFVec3i add(int x, int y, int z) {
    return new SFVec3i(this.x + x, this.y + y, this.z + z);
  }

  public SFVec3i add(SFVec3i other) {
    return add(other.x, other.y, other.z);
  }

  public SFVec3i sub(int x, int y, int z) {
    return new SFVec3i(this.x - x, this.y - y, this.z - z);
  }

  public SFVec3i sub(SFVec3i other) {
    return sub(other.x, other.y, other.z);
  }

  public Vector3i toVector3i() {
    return Vector3i.from(x, y, z);
  }

  public Vector3d toVector3d() {
    return Vector3d.from(x, y, z);
  }

  @Override
  public String toString() {
    return "SFVec3i(%d, %d, %d)".formatted(x, y, z);
  }

  public String formatXYZ() {
    return "[%d, %d, %d]".formatted(x, y, z);
  }

  public double distance(SFVec3i goal) {
    return Math.sqrt(
      MathHelper.square(goal.x - x)
        + MathHelper.square(goal.y - y)
        + MathHelper.square(goal.z - z));
  }
}
