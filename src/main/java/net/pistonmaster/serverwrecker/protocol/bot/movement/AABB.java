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
package net.pistonmaster.serverwrecker.protocol.bot.movement;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AABB {
    public double minX;
    public double minY;
    public double minZ;
    public double maxX;
    public double maxY;
    public double maxZ;

    public void floor() {
        this.minX = Math.floor(this.minX);
        this.minY = Math.floor(this.minY);
        this.minZ = Math.floor(this.minZ);
        this.maxX = Math.floor(this.maxX);
        this.maxY = Math.floor(this.maxY);
        this.maxZ = Math.floor(this.maxZ);
    }

    public AABB contract(double x, double y, double z) {
        this.minX += x;
        this.minY += y;
        this.minZ += z;
        this.maxX -= x;
        this.maxY -= y;
        this.maxZ -= z;
        return this;
    }

    public AABB extend(double x, double y, double z) {
        this.minX -= x;
        this.minY -= y;
        this.minZ -= z;
        this.maxX += x;
        this.maxY += y;
        this.maxZ += z;
        return this;
    }

    public AABB offset(double x, double y, double z) {
        this.minX += x;
        this.minY += y;
        this.minZ += z;
        this.maxX += x;
        this.maxY += y;
        this.maxZ += z;
        return this;
    }

    public double computeOffsetX(AABB other, double offsetX) {
        if (other.maxY > this.minY && other.minY < this.maxY && other.maxZ > this.minZ && other.minZ < this.maxZ) {
            if (offsetX > 0.0 && other.maxX <= this.minX) {
                offsetX = Math.min(this.minX - other.maxX, offsetX);
            } else if (offsetX < 0.0 && other.minX >= this.maxX) {
                offsetX = Math.max(this.maxX - other.minX, offsetX);
            }
        }
        return offsetX;
    }

    public double computeOffsetY(AABB other, double offsetY) {
        if (other.maxX > this.minX && other.minX < this.maxX && other.maxZ > this.minZ && other.minZ < this.maxZ) {
            if (offsetY > 0.0 && other.maxY <= this.minY) {
                offsetY = Math.min(this.minY - other.maxY, offsetY);
            } else if (offsetY < 0.0 && other.minY >= this.maxY) {
                offsetY = Math.max(this.maxY - other.minY, offsetY);
            }
        }
        return offsetY;
    }

    public double computeOffsetZ(AABB other, double offsetZ) {
        if (other.maxX > this.minX && other.minX < this.maxX && other.maxY > this.minY && other.minY < this.maxY) {
            if (offsetZ > 0.0 && other.maxZ <= this.minZ) {
                offsetZ = Math.min(this.minZ - other.maxZ, offsetZ);
            } else if (offsetZ < 0.0 && other.minZ >= this.maxZ) {
                offsetZ = Math.max(this.maxZ - other.minZ, offsetZ);
            }
        }
        return offsetZ;
    }

    public boolean intersects(AABB other) {
        return this.minX < other.maxX && this.maxX > other.minX &&
                this.minY < other.maxY && this.maxY > other.minY &&
                this.minZ < other.maxZ && this.maxZ > other.minZ;
    }

    public boolean collides(AABB other) {
        return this.minX <= other.maxX && this.maxX >= other.minX &&
                this.minY <= other.maxY && this.maxY >= other.minY &&
                this.minZ <= other.maxZ && this.maxZ >= other.minZ;
    }

    public AABB copy() {
        return new AABB(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
    }
}
