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
package net.pistonmaster.serverwrecker.pathfinding.minecraft;

import net.pistonmaster.serverwrecker.util.VectorHelper;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.HashSet;
import java.util.Set;

public record PlayerMovement(Vector3d from, BasicMovementAction action) implements MinecraftAction {
    @Override
    public Vector3d getTargetPos() {
        // Make from be either dividable by 0.5 or 1
        // This way we can have clean positions
        Vector3d normalizedFrom = VectorHelper.halfBlockNormalize(this.from);

        return switch (action) {
            case NORTH -> normalizedFrom.add(0, 0, -0.5);
            case SOUTH -> normalizedFrom.add(0, 0, 0.5);
            case EAST -> normalizedFrom.add(0.5, 0, 0);
            case WEST -> normalizedFrom.add(-0.5, 0, 0);
            case NORTH_EAST -> normalizedFrom.add(0.5, 0, -0.5);
            case NORTH_WEST -> normalizedFrom.add(-0.5, 0, -0.5);
            case SOUTH_EAST -> normalizedFrom.add(0.5, 0, 0.5);
            case SOUTH_WEST -> normalizedFrom.add(-0.5, 0, 0.5);
        };
    }

    public Set<Vector3i> requiredFreeBlocks() {
        Set<Vector3i> requiredFreeBlocks = new HashSet<>();
        Vector3i targetPos = getTargetPos().toInt();

        // Add the block that is required to be free for straight movement
        requiredFreeBlocks.add(targetPos);

        // Add the blocks that are required to be free for diagonal movement
        switch (action) {
            case NORTH_EAST -> {
                requiredFreeBlocks.add(targetPos.add(0, 0, -1));
                requiredFreeBlocks.add(targetPos.add(1, 0, 0));
            }
            case NORTH_WEST -> {
                requiredFreeBlocks.add(targetPos.add(0, 0, -1));
                requiredFreeBlocks.add(targetPos.add(-1, 0, 0));
            }
            case SOUTH_EAST -> {
                requiredFreeBlocks.add(targetPos.add(0, 0, 1));
                requiredFreeBlocks.add(targetPos.add(1, 0, 0));
            }
            case SOUTH_WEST -> {
                requiredFreeBlocks.add(targetPos.add(0, 0, 1));
                requiredFreeBlocks.add(targetPos.add(-1, 0, 0));
            }
        }

        // Add the blocks that are required to be free for the head of the player
        for (Vector3i requiredFreeBlock : Set.copyOf(requiredFreeBlocks)) {
            requiredFreeBlocks.add(requiredFreeBlock.add(0, 1, 0));
        }

        return requiredFreeBlocks;
    }
}
