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
import org.cloudburstmc.math.vector.Vector2d;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;

public class VectorHelper {
    public static final Hash.Strategy<Vector3i> VECTOR3I_HASH_STRATEGY = new Hash.Strategy<>() {
        @Override
        public int hashCode(Vector3i o) {
            var hash = 17;
            hash = 31 * hash + o.getX();
            hash = 31 * hash + o.getY();
            hash = 31 * hash + o.getZ();
            return hash;
        }

        @Override
        public boolean equals(Vector3i a, Vector3i b) {
            if (a == null || b == null) {
                return false;
            }

            return a.getX() == b.getX()
                    && a.getY() == b.getY()
                    && a.getZ() == b.getZ();
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
