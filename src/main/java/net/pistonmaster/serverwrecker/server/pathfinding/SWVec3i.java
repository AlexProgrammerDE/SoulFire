/*
 * ServerWrecker
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
package net.pistonmaster.serverwrecker.server.pathfinding;

import lombok.RequiredArgsConstructor;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;

/**
 * A simple 3D integer vector.
 * This class is used instead of SWVec3i because this uses direct field access instead of getters.
 * Even though the JIT compiler could optimize this, it's still faster to use this class.
 */
@SuppressWarnings("ClassCanBeRecord") // We want direct field access to boost performance
@RequiredArgsConstructor
public class SWVec3i {
    public static final SWVec3i ZERO = new SWVec3i(0, 0, 0);

    public final int x;
    public final int y;
    public final int z;

    public static SWVec3i fromDouble(Vector3d vec) {
        return new SWVec3i(vec.getFloorX(), vec.getFloorY(), vec.getFloorZ());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SWVec3i other)) {
            return false;
        }

        return this.x == other.x && this.y == other.y && this.z == other.z;
    }

    @Override
    public int hashCode() {
        return (x * 211 + y) * 97 + z;
    }

    public SWVec3i add(int x, int y, int z) {
        return new SWVec3i(this.x + x, this.y + y, this.z + z);
    }

    public SWVec3i add(SWVec3i other) {
        return add(other.x, other.y, other.z);
    }

    public SWVec3i sub(int x, int y, int z) {
        return new SWVec3i(this.x - x, this.y - y, this.z - z);
    }

    public Vector3i toVector3i() {
        return Vector3i.from(x, y, z);
    }

    public Vector3d toVector3d() {
        return Vector3d.from(x, y, z);
    }

    @Override
    public String toString() {
        return "SWVec3i(" + x + ", " + y + ", " + z + ")";
    }
}
