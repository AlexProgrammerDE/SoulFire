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
package net.pistonmaster.serverwrecker.pathfinding.graph;

import lombok.RequiredArgsConstructor;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.function.Function;

@RequiredArgsConstructor
public enum MovementDirection {
    NORTH(vector -> vector.add(0, 0, -1), vector -> vector.add(0, 0, -1)),
    SOUTH(vector -> vector.add(0, 0, 1), vector -> vector.add(0, 0, 1)),
    EAST(vector -> vector.add(1, 0, 0), vector -> vector.add(1, 0, 0)),
    WEST(vector -> vector.add(-1, 0, 0), vector -> vector.add(-1, 0, 0)),
    NORTH_EAST(vector -> vector.add(1, 0, -1), vector -> vector.add(1, 0, -1)),
    NORTH_WEST(vector -> vector.add(-1, 0, -1), vector -> vector.add(-1, 0, -1)),
    SOUTH_EAST(vector -> vector.add(1, 0, 1), vector -> vector.add(1, 0, 1)),
    SOUTH_WEST(vector -> vector.add(-1, 0, 1), vector -> vector.add(-1, 0, 1));

    public static final MovementDirection[] VALUES = values();
    private final Function<Vector3i, Vector3i> offsetInteger;
    private final Function<Vector3d, Vector3d> offsetDouble;

    public Vector3i offset(Vector3i vector) {
        return offsetInteger.apply(vector);
    }

    public Vector3d offset(Vector3d vector) {
        return offsetDouble.apply(vector);
    }

    public boolean isDiagonal() {
        return this == NORTH_EAST || this == NORTH_WEST || this == SOUTH_EAST || this == SOUTH_WEST;
    }
}
