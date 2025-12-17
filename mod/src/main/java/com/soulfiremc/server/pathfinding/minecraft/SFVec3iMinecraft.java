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
package com.soulfiremc.server.pathfinding.minecraft;

import com.soulfiremc.server.pathfinding.SFVec3i;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/// Utility class for converting between SFVec3i and Minecraft types.
public final class SFVec3iMinecraft {
  private SFVec3iMinecraft() {}

  /// Creates an SFVec3i from a Minecraft BlockPos.
  public static SFVec3i fromBlockPos(BlockPos pos) {
    return SFVec3i.from(pos.getX(), pos.getY(), pos.getZ());
  }

  /// Converts an SFVec3i to a Minecraft BlockPos.
  public static BlockPos toBlockPos(SFVec3i vec) {
    return new BlockPos(vec.x, vec.y, vec.z);
  }

  /// Converts an SFVec3i to a Minecraft Vec3.
  public static Vec3 toVec3(SFVec3i vec) {
    return new Vec3(vec.x, vec.y, vec.z);
  }

  /// Creates an SFVec3i from a Minecraft Vec3 (floors the coordinates).
  public static SFVec3i fromVec3(Vec3 vec) {
    return SFVec3i.fromDouble(vec.x, vec.y, vec.z);
  }
}
