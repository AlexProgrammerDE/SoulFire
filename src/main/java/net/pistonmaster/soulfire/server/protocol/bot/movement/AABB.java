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
package net.pistonmaster.soulfire.server.protocol.bot.movement;

import lombok.ToString;
import org.cloudburstmc.math.vector.Vector3d;

import java.util.Optional;

@ToString
public class AABB {
    public final double minX;
    public final double minY;
    public final double minZ;
    public final double maxX;
    public final double maxY;
    public final double maxZ;

    public AABB(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
    }

    public AABB(Vector3d min, Vector3d max) {
        this(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
    }

    public AABB deflate(double x, double y, double z) {
        return new AABB(
                this.minX + x,
                this.minY + y,
                this.minZ + z,
                this.maxX - x,
                this.maxY - y,
                this.maxZ - z
        );
    }

    public AABB expandTowards(Vector3d targetVec) {
        return this.expandTowards(targetVec.getX(), targetVec.getY(), targetVec.getZ());
    }

    public AABB expandTowards(double x, double y, double z) {
        var minX = this.minX;
        var minY = this.minY;
        var minZ = this.minZ;
        var maxX = this.maxX;
        var maxY = this.maxY;
        var maxZ = this.maxZ;

        // Handle expanding of min/max x
        if (x < 0.0) {
            minX += x;
        } else if (x > 0.0) {
            maxX += x;
        }

        // Handle expanding of min/max y
        if (y < 0.0) {
            minY += y;
        } else if (y > 0.0) {
            maxY += y;
        }

        // Handle expanding of min/max z
        if (z < 0.0) {
            minZ += z;
        } else if (z > 0.0) {
            maxZ += z;
        }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public AABB move(Vector3d vec) {
        return this.move(vec.getX(), vec.getY(), vec.getZ());
    }

    public AABB move(double x, double y, double z) {
        return new AABB(
                this.minX + x,
                this.minY + y,
                this.minZ + z,
                this.maxX + x,
                this.maxY + y,
                this.maxZ + z
        );
    }

    public double computeOffsetX(AABB other, double offsetX) {
        if (Math.abs(offsetX) < 1.0E-7) {
            return 0.0;
        }

        var betweenY = other.maxY > this.minY && other.minY < this.maxY;
        var betweenZ = other.maxZ > this.minZ && other.minZ < this.maxZ;
        if (betweenY && betweenZ) {
            if (offsetX > 0.0 && other.maxX <= this.minX) {
                offsetX = Math.min(this.minX - other.maxX, offsetX);
            } else if (offsetX < 0.0 && other.minX >= this.maxX) {
                offsetX = Math.max(this.maxX - other.minX, offsetX);
            }
        }
        return offsetX;
    }

    public double computeOffsetY(AABB other, double offsetY) {
        if (Math.abs(offsetY) < 1.0E-7) {
            return 0.0;
        }

        var betweenX = other.maxX > this.minX && other.minX < this.maxX;
        var betweenZ = other.maxZ > this.minZ && other.minZ < this.maxZ;
        if (betweenX && betweenZ) {
            if (offsetY > 0.0 && other.maxY <= this.minY) {
                offsetY = Math.min(this.minY - other.maxY, offsetY);
            } else if (offsetY < 0.0 && other.minY >= this.maxY) {
                offsetY = Math.max(this.maxY - other.minY, offsetY);
            }
        }
        return offsetY;
    }

    public double computeOffsetZ(AABB other, double offsetZ) {
        if (Math.abs(offsetZ) < 1.0E-7) {
            return 0.0;
        }

        var betweenX = other.maxX > this.minX && other.minX < this.maxX;
        var betweenY = other.maxY > this.minY && other.minY < this.maxY;
        if (betweenX && betweenY) {
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

    public Optional<Vector3d> getIntersection(Vector3d origin, Vector3d direction) {
        var x1 = origin.getX();
        var y1 = origin.getY();
        var z1 = origin.getZ();
        var x2 = direction.getX();
        var y2 = direction.getY();
        var z2 = direction.getZ();

        var txMin = (this.minX - x1) / x2;
        var txMax = (this.maxX - x1) / x2;
        var tyMin = (this.minY - y1) / y2;
        var tyMax = (this.maxY - y1) / y2;
        var tzMin = (this.minZ - z1) / z2;
        var tzMax = (this.maxZ - z1) / z2;

        var tMin = Math.max(Math.max(Math.min(txMin, txMax), Math.min(tyMin, tyMax)), Math.min(tzMin, tzMax));
        var tMax = Math.min(Math.min(Math.max(txMin, txMax), Math.max(tyMin, tyMax)), Math.max(tzMin, tzMax));

        if (tMax < 0 || tMin > tMax) {
            return Optional.empty();
        }

        return Optional.of(origin.add(direction.mul(tMin)));
    }
}
