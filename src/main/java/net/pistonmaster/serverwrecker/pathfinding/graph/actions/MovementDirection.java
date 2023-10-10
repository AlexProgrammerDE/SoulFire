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
package net.pistonmaster.serverwrecker.pathfinding.graph.actions;

import lombok.RequiredArgsConstructor;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;

@RequiredArgsConstructor
public enum MovementDirection {
    NORTH,
    SOUTH,
    EAST,
    WEST,
    NORTH_EAST,
    NORTH_WEST,
    SOUTH_EAST,
    SOUTH_WEST;

    public static final MovementDirection[] VALUES = values();

    @SuppressWarnings("DuplicatedCode")
    public Vector3i offset(Vector3i vector) {
        return switch (this) {
            case NORTH -> vector.add(0, 0, -1);
            case SOUTH -> vector.add(0, 0, 1);
            case EAST -> vector.add(1, 0, 0);
            case WEST -> vector.add(-1, 0, 0);
            case NORTH_EAST -> vector.add(1, 0, -1);
            case NORTH_WEST -> vector.add(-1, 0, -1);
            case SOUTH_EAST -> vector.add(1, 0, 1);
            case SOUTH_WEST -> vector.add(-1, 0, 1);
        };
    }

    @SuppressWarnings("DuplicatedCode")
    public Vector3d offset(Vector3d vector) {
        return switch (this) {
            case NORTH -> vector.add(0, 0, -1);
            case SOUTH -> vector.add(0, 0, 1);
            case EAST -> vector.add(1, 0, 0);
            case WEST -> vector.add(-1, 0, 0);
            case NORTH_EAST -> vector.add(1, 0, -1);
            case NORTH_WEST -> vector.add(-1, 0, -1);
            case SOUTH_EAST -> vector.add(1, 0, 1);
            case SOUTH_WEST -> vector.add(-1, 0, 1);
        };
    }

    public boolean isDiagonal() {
        return this == NORTH_EAST || this == NORTH_WEST || this == SOUTH_EAST || this == SOUTH_WEST;
    }
}
