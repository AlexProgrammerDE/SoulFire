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
package net.pistonmaster.serverwrecker.data;

import net.pistonmaster.serverwrecker.protocol.bot.movement.AABB;
import net.pistonmaster.serverwrecker.util.BoundingBox;

public record BlockShape(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
    public boolean isFullBlock() {
        return minX == 0 && minY == 0 && minZ == 0 && maxX == 1 && maxY == 1 && maxZ == 1;
    }

    public BoundingBox createBoundingBoxAt(double x, double y, double z) {
        return new BoundingBox(x + minX, y + minY, z + minZ, x + maxX, y + maxY, z + maxZ);
    }

    public AABB createAABBAt(double x, double y, double z) {
        return new AABB(x + minX, y + minY, z + minZ, x + maxX, y + maxY, z + maxZ);
    }
}
