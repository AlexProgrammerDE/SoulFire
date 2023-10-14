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
package net.pistonmaster.serverwrecker.pathfinding.graph.actions.block;

import org.cloudburstmc.math.vector.Vector3i;

public enum BlockModifier {
    HEAD,
    FEET,
    FLOOR;

    public static final BlockModifier[] VALUES = values();

    @SuppressWarnings("DuplicatedCode")
    public Vector3i offset(Vector3i vector) {
        return switch (this) {
            case HEAD -> vector.add(0, 1, 0);
            case FEET -> vector;
            case FLOOR -> vector.add(0, -1, 0);
        };
    }
}
