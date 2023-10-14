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
package net.pistonmaster.serverwrecker.pathfinding.graph.actions.movement;

import com.github.steveice10.mc.protocol.data.game.entity.object.Direction;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.function.Function;

@RequiredArgsConstructor
public enum SWDirection {
    DOWN(Direction.DOWN, pos -> pos.add(0, 1, 0)),
    UP(Direction.UP, pos -> pos.add(0, -1, 0)),
    NORTH(Direction.NORTH, pos -> pos.add(0, 0, 1)),
    SOUTH(Direction.SOUTH, pos -> pos.add(0, 0, -1)),
    WEST(Direction.WEST, pos -> pos.add(1, 0, 0)),
    EAST(Direction.EAST, pos -> pos.add(-1, 0, 0));

    public static final SWDirection[] VALUES = values();

    @Getter
    private final Direction direction;
    private final Function<Vector3i, Vector3i> offsetFunction;

    public Vector3i offset(Vector3i pos) {
        return offsetFunction.apply(pos);
    }
}
