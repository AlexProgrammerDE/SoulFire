/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.server.data;

import net.pistonmaster.serverwrecker.server.util.MathHelper;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;

public record OffsetData(OffsetType type, float maxHorizontalOffset, float maxVerticalOffset) {
    public Vector3d getOffsetForBlock(Vector3i block) {
        return switch (type) {
            case XYZ -> {
                var seed = MathHelper.getSeed(block.getX(), 0, block.getZ());
                var yOffset = ((double) ((float) (seed >> 4 & 15L) / 15.0F) - 1.0) * (double) maxVerticalOffset;
                var xOffset = GenericMath.clamp(((double) ((float) (seed & 15L) / 15.0F) - 0.5) * 0.5, -maxHorizontalOffset, maxHorizontalOffset);
                var zOffset = GenericMath.clamp(((double) ((float) (seed >> 8 & 15L) / 15.0F) - 0.5) * 0.5, -maxHorizontalOffset, maxHorizontalOffset);
                yield Vector3d.from(xOffset, yOffset, zOffset);
            }
            case XZ -> {
                var seed = MathHelper.getSeed(block.getX(), 0, block.getZ());
                var xOffset = GenericMath.clamp(((double) ((float) (seed & 15L) / 15.0F) - 0.5) * 0.5, -maxHorizontalOffset, maxHorizontalOffset);
                var zOffset = GenericMath.clamp(((double) ((float) (seed >> 8 & 15L) / 15.0F) - 0.5) * 0.5, -maxHorizontalOffset, maxHorizontalOffset);
                yield Vector3d.from(xOffset, 0.0, zOffset);
            }
        };
    }

    public enum OffsetType {
        XZ,
        XYZ
    }
}
