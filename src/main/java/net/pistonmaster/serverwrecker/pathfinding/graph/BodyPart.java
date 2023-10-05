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
import org.cloudburstmc.math.vector.Vector3i;

import java.util.function.Function;

@RequiredArgsConstructor
public enum BodyPart {
    FEET(v -> v),
    HEAD(v -> v.add(0, 1, 0));

    // Iterating over BodyPart.values() is slower than iteration over a static array
    // Reversed because we normally want to see the head block mined before the feet
    public static final BodyPart[] BODY_PARTS_REVERSE = new BodyPart[]{
            BodyPart.HEAD,
            BodyPart.FEET
    };

    private final Function<Vector3i, Vector3i> offset;

    public Vector3i offset(Vector3i position) {
        return offset.apply(position);
    }
}
