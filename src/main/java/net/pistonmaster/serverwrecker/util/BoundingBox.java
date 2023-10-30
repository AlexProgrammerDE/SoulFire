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
package net.pistonmaster.serverwrecker.util;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.cloudburstmc.math.vector.Vector3d;

import java.util.Optional;

/**
 * Taken from: <a href="https://github.com/LabyStudio/java-minecraft/blob/master/src/main/java/de/labystudio/game/util/BoundingBox.java">LabyStudio/java-minecraft</a>
 */
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class BoundingBox {
    public double minX;
    public double minY;
    public double minZ;
    public double maxX;
    public double maxY;
    public double maxZ;

    public BoundingBox(Vector3d min, Vector3d max) {
        this(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
    }

    /**
     * Expand the bounding box. Positive and negative numbers controls which side of the box should grow.
     *
     * @param x Amount to expand the minX or maxX
     * @param y Amount to expand the minY or maxY
     * @param z Amount to expand the minZ or maxZ
     * @return The expanded bounding box
     */
    public BoundingBox expand(double x, double y, double z) {
        var minX = this.minX;
        var minY = this.minY;
        var minZ = this.minZ;
        var maxX = this.maxX;
        var maxY = this.maxY;
        var maxZ = this.maxZ;

        // Handle expanding of min/max x
        if (x < 0.0F) {
            minX += x;
        } else {
            maxX += x;
        }

        // Handle expanding of min/max y
        if (y < 0.0F) {
            minY += y;
        } else {
            maxY += y;
        }

        // Handle expanding of min/max z
        if (z < 0.0F) {
            minZ += z;
        } else {
            maxZ += z;
        }

        // Create new bounding box
        return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Check for collision on the X axis
     *
     * @param other   The other bounding box that is colliding with this one.
     * @param offsetX Position on the X axis that is colliding
     * @return Returns the corrected X position that collided.
     */
    public double computeOffsetX(BoundingBox other, double offsetX) {
        if (other.maxY > this.minY && other.minY < this.maxY && other.maxZ > this.minZ && other.minZ < this.maxZ) {
            if (offsetX > 0.0 && other.maxX <= this.minX) {
                offsetX = Math.min(this.minX - other.maxX, offsetX);
            } else if (offsetX < 0.0 && other.minX >= this.maxX) {
                offsetX = Math.max(this.maxX - other.minX, offsetX);
            }
        }
        return offsetX;
    }

    /**
     * Check for collision on the Y axis
     *
     * @param other   The other bounding box that is colliding with this one.
     * @param offsetY Position on the Y axis that is colliding
     * @return Returns the corrected Y position that collided.
     */
    public double computeOffsetY(BoundingBox other, double offsetY) {
        if (other.maxX > this.minX && other.minX < this.maxX && other.maxZ > this.minZ && other.minZ < this.maxZ) {
            if (offsetY > 0.0 && other.maxY <= this.minY) {
                offsetY = Math.min(this.minY - other.maxY, offsetY);
            } else if (offsetY < 0.0 && other.minY >= this.maxY) {
                offsetY = Math.max(this.maxY - other.minY, offsetY);
            }
        }
        return offsetY;
    }

    /**
     * Check for collision on the Z axis
     *
     * @param other   The other bounding box that is colliding wit this one.
     * @param offsetZ Position on the Z axis that is colliding
     * @return Returns the corrected Z position that collided.
     */
    public double computeOffsetZ(BoundingBox other, double offsetZ) {
        if (other.maxX > this.minX && other.minX < this.maxX && other.maxY > this.minY && other.minY < this.maxY) {
            if (offsetZ > 0.0 && other.maxZ <= this.minZ) {
                offsetZ = Math.min(this.minZ - other.maxZ, offsetZ);
            } else if (offsetZ < 0.0 && other.minZ >= this.maxZ) {
                offsetZ = Math.max(this.maxZ - other.minZ, offsetZ);
            }
        }
        return offsetZ;
    }

    /**
     * Check if the two boxes are intersecting/overlapping
     *
     * @param otherBoundingBox The other bounding box that could intersect
     * @return The two boxes are overlapping
     */
    public boolean intersects(BoundingBox otherBoundingBox) {
        // Check on X axis
        if (otherBoundingBox.maxX <= this.minX || otherBoundingBox.minX >= this.maxX) {
            return false;
        }

        // Check on Y axis
        if (otherBoundingBox.maxY <= this.minY || otherBoundingBox.minY >= this.maxY) {
            return false;
        }

        // Check on Z axis
        return (!(otherBoundingBox.maxZ <= this.minZ)) && (!(otherBoundingBox.minZ >= this.maxZ));
    }

    /**
     * Move the bounding box relative.
     *
     * @param x Relative offset x
     * @param y Relative offset y
     * @param z Relative offset z
     */
    public void move(double x, double y, double z) {
        this.minX += x;
        this.minY += y;
        this.minZ += z;
        this.maxX += x;
        this.maxY += y;
        this.maxZ += z;
    }

    /**
     * Create a new bounding box with the given offset
     *
     * @param x Relative offset x
     * @param y Relative offset x
     * @param z Relative offset x
     * @return New bounding box with the given offset relative to this bounding box
     */
    public BoundingBox offset(double x, double y, double z) {
        return new BoundingBox(this.minX + x, this.minY + y, this.minZ + z, this.maxX + x, this.maxY + y, this.maxZ + z);
    }

    public Optional<Vector3d> getIntersection(Vector3d origin, Vector3d direction) {
        var x1 = origin.getX();
        var y1 = origin.getY();
        var z1 = origin.getZ();
        var x2 = direction.getX();
        var y2 = direction.getY();
        var z2 = direction.getZ();

        var xMin = this.minX;
        var yMin = this.minY;
        var zMin = this.minZ;
        var xMax = this.maxX;
        var yMax = this.maxY;
        var zMax = this.maxZ;

        var txMin = (xMin - x1) / x2;
        var txMax = (xMax - x1) / x2;
        var tyMin = (yMin - y1) / y2;
        var tyMax = (yMax - y1) / y2;
        var tzMin = (zMin - z1) / z2;
        var tzMax = (zMax - z1) / z2;

        var tMin = Math.max(Math.max(Math.min(txMin, txMax), Math.min(tyMin, tyMax)), Math.min(tzMin, tzMax));
        var tMax = Math.min(Math.min(Math.max(txMin, txMax), Math.max(tyMin, tyMax)), Math.max(tzMin, tzMax));

        if (tMax < 0 || tMin > tMax) {
            return Optional.empty();
        }

        return Optional.of(origin.add(direction.mul(tMin)));
    }

    public BoundingBox copy() {
        return new BoundingBox(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
    }
}
