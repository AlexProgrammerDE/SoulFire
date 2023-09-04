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

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.data.BlockType;
import net.pistonmaster.serverwrecker.protocol.bot.state.LevelState;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.*;

/**
 * An immutable representation of the world state.
 * This takes a world state and projects changes onto it.
 * This way we calculate the way we can do actions after a block was broken/placed.
 */
@EqualsAndHashCode
@RequiredArgsConstructor
public class ProjectedLevelState {
    private final LevelState levelState;
    private final Map<Vector3i, BlockType> blockLookupCache;

    public ProjectedLevelState(LevelState levelState) {
        this(levelState, new HashMap<>());
    }

    public ProjectedLevelState withChange(Vector3i position, BlockType blockType) {
        Map<Vector3i, BlockType> blockStateCache = new HashMap<>(blockLookupCache);
        blockStateCache.put(position, blockType);

        return new ProjectedLevelState(levelState, blockStateCache);
    }

    public BlockType getBlockTypeAt(Vector3i position) {
        return blockLookupCache.compute(position, (vector3i, blockType) -> {
            // If we already know the block type (thanks to cache), return it
            if (blockType != null) {
                return blockType;
            }

            return levelState.getBlockTypeAt(vector3i);
        });
    }
}
