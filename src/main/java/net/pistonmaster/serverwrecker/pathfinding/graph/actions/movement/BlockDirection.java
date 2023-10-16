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

@Getter
@RequiredArgsConstructor
public enum BlockDirection {
    NORTH(Direction.NORTH),
    SOUTH(Direction.SOUTH),
    EAST(Direction.EAST),
    WEST(Direction.WEST);

    public static final BlockDirection[] VALUES = values();
    private final Direction direction;

    @SuppressWarnings("DuplicatedCode")
    public Vector3i offset(Vector3i vector) {
        return switch (this) {
            case NORTH -> vector.add(0, 0, -1);
            case SOUTH -> vector.add(0, 0, 1);
            case EAST -> vector.add(1, 0, 0);
            case WEST -> vector.add(-1, 0, 0);
        };
    }

    public BlockDirection opposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case EAST -> WEST;
            case WEST -> EAST;
        };
    }

    public BlockDirection leftSide() {
        return switch (this) {
            case NORTH -> WEST;
            case SOUTH -> EAST;
            case EAST -> NORTH;
            case WEST -> SOUTH;
        };
    }

    public BlockDirection rightSide() {
        return switch (this) {
            case NORTH -> EAST;
            case SOUTH -> WEST;
            case EAST -> SOUTH;
            case WEST -> NORTH;
        };
    }
}
