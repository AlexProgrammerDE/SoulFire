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
package net.pistonmaster.soulfire.server.pathfinding;

import lombok.RequiredArgsConstructor;
import net.pistonmaster.soulfire.server.util.MathHelper;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;

/**
 * A simple 3D integer vector.
 * This class is used instead of Vector3i because this uses direct field access instead of getters.
 * Even though the JIT compiler could optimize this, it's still faster to use this class.
 */
@RequiredArgsConstructor
public class SWVec3i {
    public static final SWVec3i ZERO = new SWVec3i(0, 0, 0);

    public final int x;
    public final int y;
    public final int z;
    private int hashCode;
    private boolean hashCodeSet;

    public static SWVec3i fromDouble(Vector3d vec) {
        return fromInt(vec.toInt());
    }

    public static SWVec3i fromInt(Vector3i vec) {
        return from(vec.getX(), vec.getY(), vec.getZ());
    }

    public static SWVec3i from(int x, int y, int z) {
        return new SWVec3i(x, y, z);
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
        if (!hashCodeSet) {
            hashCode = (x * 211 + y) * 97 + z;
            hashCodeSet = true;
        }

        return hashCode;
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

    public String formatXYZ() {
        return "[" + x + ", " + y + ", " + z + "]";
    }

    public double distance(SWVec3i goal) {
        return Math.sqrt(MathHelper.square(goal.x - x) + MathHelper.square(goal.y - y) + MathHelper.square(goal.z - z));
    }
}
