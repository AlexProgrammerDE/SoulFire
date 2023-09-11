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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.math.vector.Vector3i;

@Getter
@RequiredArgsConstructor
public enum BodyPart {
    FEET(Vector3i.from(0, 0, 0)),
    HEAD(Vector3i.from(0, 1, 0));

    // Iterating over BodyPart.values() is slower than iteration over a static array
    public static final Vector3i[] BODY_PARTS = new Vector3i[]{
            BodyPart.FEET.getOffset(),
            BodyPart.HEAD.getOffset()
    };

    private final Vector3i offset;
}
