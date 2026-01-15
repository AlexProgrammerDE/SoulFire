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
package com.soulfiremc.server.pathfinding;

import lombok.RequiredArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/// A simple 3D integer vector. This class is used instead of BlockPos because this uses direct field
/// access instead of getters. Even though the JIT compiler could optimize this, it's still faster to
/// use this class.
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

  public static SFVec3i fromDouble(Vec3 vec) {
    return fromInt(BlockPos.containing(vec));
  }

  public static SFVec3i fromInt(BlockPos vec) {
    return from(vec.getX(), vec.getY(), vec.getZ());
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
      hashCode = Long.hashCode(asMinecraftLong());
      hashCodeSet = true;
    }

    return hashCode;
  }

  public long asMinecraftLong() {
    if (!minecraftLongSet) {
      minecraftLong = BlockPos.asLong(x, y, z);
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

  public Vec3 toVec3() {
    return new Vec3(x, y, z);
  }

  public BlockPos toBlockPos() {
    return new BlockPos(x, y, z);
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
      Mth.square(goal.x - x)
        + Mth.square(goal.y - y)
        + Mth.square(goal.z - z));
  }
}
