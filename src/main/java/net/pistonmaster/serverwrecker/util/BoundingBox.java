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
public class BoundingBox implements Cloneable {
    private static final double epsilon = 0.0F;

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
     * Expand the bounding box on both sides.
     * The center is always fixed when using grow.
     *
     * @param x Amount to expand the minX and maxX
     * @param y Amount to expand the minY and maxY
     * @param z Amount to expand the minZ and maxZ
     * @return The expanded bounding box
     */
    public BoundingBox grow(double x, double y, double z) {
        return new BoundingBox(this.minX - x, this.minY - y,
                this.minZ - z, this.maxX + x,
                this.maxY + y, this.maxZ + z);
    }

    /**
     * Check for collision on the X axis
     *
     * @param otherBoundingBox The other bounding box that is colliding with this one.
     * @param x                Position on the X axis that is colliding
     * @return Returns the corrected x position that collided.
     */
    public double clipXCollide(BoundingBox otherBoundingBox, double x) {
        // Check if the boxes are colliding on the Y axis
        if (otherBoundingBox.maxY <= this.minY || otherBoundingBox.minY >= this.maxY) {
            return x;
        }

        // Check if the boxes are colliding on the Z axis
        if (otherBoundingBox.maxZ <= this.minZ || otherBoundingBox.minZ >= this.maxZ) {
            return x;
        }

        // Check for collision if the X axis of the current box is bigger
        if (x > 0.0F && otherBoundingBox.maxX <= this.minX) {
            var max = this.minX - otherBoundingBox.maxX - epsilon;
            if (max < x) {
                x = max;
            }
        }

        // Check for collision if the X axis of the current box is smaller
        if (x < 0.0F && otherBoundingBox.minX >= this.maxX) {
            var max = this.maxX - otherBoundingBox.minX + epsilon;
            if (max > x) {
                x = max;
            }
        }

        return x;
    }

    /**
     * Check for collision on the Y axis
     *
     * @param otherBoundingBox The other bounding box that is colliding with this one.
     * @param y                Position on the Y axis that is colliding
     * @return Returns the corrected y position that collided.
     */
    public double clipYCollide(BoundingBox otherBoundingBox, double y) {
        // Check if the boxes are colliding on the X axis
        if (otherBoundingBox.maxX <= this.minX || otherBoundingBox.minX >= this.maxX) {
            return y;
        }

        // Check if the boxes are colliding on the Z axis
        if (otherBoundingBox.maxZ <= this.minZ || otherBoundingBox.minZ >= this.maxZ) {
            return y;
        }

        // Check for collision if the Y axis of the current box is bigger
        if (y > 0.0F && otherBoundingBox.maxY <= this.minY) {
            var max = this.minY - otherBoundingBox.maxY - epsilon;
            if (max < y) {
                y = max;
            }
        }

        // Check for collision if the Y axis of the current box is bigger
        if (y < 0.0F && otherBoundingBox.minY >= this.maxY) {
            var max = this.maxY - otherBoundingBox.minY + epsilon;
            if (max > y) {
                y = max;
            }
        }

        return y;
    }

    /**
     * Check for collision on the Z axis
     *
     * @param otherBoundingBox The other bounding box that is colliding wit this one.
     * @param z                Position on the X axis that is colliding
     * @return Returns the corrected x position that collided.
     */
    public double clipZCollide(BoundingBox otherBoundingBox, double z) {
        // Check if the boxes are colliding on the X axis
        if (otherBoundingBox.maxX <= this.minX || otherBoundingBox.minX >= this.maxX) {
            return z;
        }

        // Check if the boxes are colliding on the Y axis
        if (otherBoundingBox.maxY <= this.minY || otherBoundingBox.minY >= this.maxY) {
            return z;
        }

        // Check for collision if the Z axis of the current box is bigger
        if (z > 0.0F && otherBoundingBox.maxZ <= this.minZ) {
            var max = this.minZ - otherBoundingBox.maxZ - epsilon;
            if (max < z) {
                z = max;
            }
        }

        // Check for collision if the Z axis of the current box is bigger
        if (z < 0.0F && otherBoundingBox.minZ >= this.maxZ) {
            var max = this.maxZ - otherBoundingBox.minZ + epsilon;
            if (max > z) {
                z = max;
            }
        }

        return z;
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

    @Override
    public BoundingBox clone() {
        try {
            return (BoundingBox) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
