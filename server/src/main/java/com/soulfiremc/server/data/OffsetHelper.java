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

import com.soulfiremc.server.util.MathHelper;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;

public class OffsetHelper {
  public static Vector3d getOffsetForBlock(BlockType blockType, Vector3i block) {
    var offsetData = blockType.offsetData();
    if (offsetData == null) {
      return Vector3d.ZERO;
    }

    var maxHorizontalOffset = offsetData.maxHorizontalOffset();
    var seed = MathHelper.getSeed(block.getX(), 0, block.getZ());
    return switch (offsetData.offsetType()) {
      case XYZ -> {
        var maxVerticalOffset = offsetData.maxVerticalOffset();
        var yOffset =
          ((double) ((float) (seed >> 4 & 15L) / 15.0F) - 1.0) * (double) maxVerticalOffset;
        var xOffset =
          MathHelper.clamp(
            ((double) ((float) (seed & 15L) / 15.0F) - 0.5) * 0.5,
            -maxHorizontalOffset,
            maxHorizontalOffset);
        var zOffset =
          MathHelper.clamp(
            ((double) ((float) (seed >> 8 & 15L) / 15.0F) - 0.5) * 0.5,
            -maxHorizontalOffset,
            maxHorizontalOffset);
        yield Vector3d.from(xOffset, yOffset, zOffset);
      }
      case XZ -> {
        var xOffset =
          MathHelper.clamp(
            ((double) ((float) (seed & 15L) / 15.0F) - 0.5) * 0.5,
            -maxHorizontalOffset,
            maxHorizontalOffset);
        var zOffset =
          MathHelper.clamp(
            ((double) ((float) (seed >> 8 & 15L) / 15.0F) - 0.5) * 0.5,
            -maxHorizontalOffset,
            maxHorizontalOffset);
        yield Vector3d.from(xOffset, 0.0, zOffset);
      }
    };
  }
}
