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

import it.unimi.dsi.fastutil.Hash;
import net.pistonmaster.serverwrecker.server.pathfinding.SWVec3i;
import org.cloudburstmc.math.vector.Vector2d;
import org.cloudburstmc.math.vector.Vector3d;

public class VectorHelper {
    public static final Hash.Strategy<SWVec3i> VECTOR3I_HASH_STRATEGY = new Hash.Strategy<>() {
        @Override
        public int hashCode(SWVec3i o) {
            return o.hashCode();
        }

        @Override
        public boolean equals(SWVec3i a, SWVec3i b) {
            if (b == null) {
                return false;
            }

            if (a == b) {
                return true;
            }

            return a.x == b.x
                    && a.y == b.y
                    && a.z == b.z;
        }
    };

    private VectorHelper() {
    }

    public static Vector3d middleOfBlockNormalize(Vector3d vector) {
        return vector.floor().add(0.5, 0, 0.5);
    }

    public static Vector2d middleOfBlockNormalize(Vector2d vector) {
        return vector.floor().add(0.5, 0.5);
    }
}
