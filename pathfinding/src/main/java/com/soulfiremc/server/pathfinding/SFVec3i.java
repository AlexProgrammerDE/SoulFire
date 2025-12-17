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

import lombok.RequiredArgsConstructor;

/// A simple 3D integer vector. This class is used for block positions in pathfinding.
/// Uses direct field access for performance optimization.
@RequiredArgsConstructor
public final class SFVec3i {
  public static final SFVec3i ZERO = new SFVec3i(0, 0, 0);

  public final int x;
  public final int y;
  public final int z;
  private int hashCode;
  private boolean hashCodeSet;
  private long packedLong;
  private boolean packedLongSet;

  public static SFVec3i fromDouble(double x, double y, double z) {
    return new SFVec3i((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
  }

  public static SFVec3i from(int x, int y, int z) {
    return new SFVec3i(x, y, z);
  }

  public boolean equals(SFVec3i other) {
    return x == other.x && y == other.y && z == other.z;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof SFVec3i other)) {
      return false;
    }

    return this.x == other.x && this.y == other.y && this.z == other.z;
  }

  @Override
  public int hashCode() {
    if (!hashCodeSet) {
      hashCode = Long.hashCode(asLong());
      hashCodeSet = true;
    }

    return hashCode;
  }

  /// Pack coordinates into a long value for efficient storage and lookup.
  /// Uses 21 bits for X and Z, 22 bits for Y (allowing -2097152 to 2097151 range).
  public long asLong() {
    if (!packedLongSet) {
      packedLong = packLong(x, y, z);
      packedLongSet = true;
    }

    return packedLong;
  }

  /// Pack coordinates into a long value.
  /// Format: X (21 bits) | Z (21 bits) | Y (22 bits)
  public static long packLong(int x, int y, int z) {
    long l = 0L;
    l |= ((long) x & 0x1FFFFFL) << 38;
    l |= ((long) y & 0x3FFFFFL);
    l |= ((long) z & 0x1FFFFFL) << 12;
    return l;
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

  @Override
  public String toString() {
    return "SFVec3i(%d, %d, %d)".formatted(x, y, z);
  }

  public String formatXYZ() {
    return "[%d, %d, %d]".formatted(x, y, z);
  }

  public double distance(SFVec3i goal) {
    return Math.sqrt(distanceSquared(goal));
  }

  public double distanceSquared(SFVec3i goal) {
    var dx = (double) (goal.x - x);
    var dy = (double) (goal.y - y);
    var dz = (double) (goal.z - z);
    return dx * dx + dy * dy + dz * dz;
  }

  public double getX() {
    return x;
  }

  public double getY() {
    return y;
  }

  public double getZ() {
    return z;
  }
}
