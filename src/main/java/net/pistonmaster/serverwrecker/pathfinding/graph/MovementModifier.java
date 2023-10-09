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

@RequiredArgsConstructor
public enum MovementModifier {
    NORMAL,
    FALL_1,
    FALL_2,
    FALL_3,
    JUMP;

    public static final MovementModifier[] VALUES = values();

    public Vector3i offset(Vector3i vector) {
        return switch (this) {
            case NORMAL -> vector;
            case FALL_1 -> vector.add(0, -1, 0);
            case FALL_2 -> vector.add(0, -2, 0);
            case FALL_3 -> vector.add(0, -3, 0);
            case JUMP -> vector.add(0, 1, 0);
        };
    }

    public Vector3d offset(Vector3d vector) {
        return switch (this) {
            case NORMAL -> vector;
            case FALL_1 -> vector.add(0, -1, 0);
            case FALL_2 -> vector.add(0, -2, 0);
            case FALL_3 -> vector.add(0, -3, 0);
            case JUMP -> vector.add(0, 1, 0);
        };
    }

    public Vector3i offsetIfJump(Vector3i vector) {
        return this == MovementModifier.JUMP ? vector.add(0, 1, 0) : vector;
    }
}
