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
package net.pistonmaster.serverwrecker.server.pathfinding.graph;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.server.data.BlockShapeType;
import net.pistonmaster.serverwrecker.server.data.BlockType;
import net.pistonmaster.serverwrecker.server.pathfinding.Costs;
import net.pistonmaster.serverwrecker.server.pathfinding.SWVec3i;
import net.pistonmaster.serverwrecker.server.protocol.bot.block.BlockStateMeta;
import net.pistonmaster.serverwrecker.server.protocol.bot.state.ChunkHolder;
import net.pistonmaster.serverwrecker.server.protocol.bot.state.LevelState;
import net.pistonmaster.serverwrecker.server.util.VectorHelper;

/**
 * An immutable representation of the world state.
 * This takes a world state and projects changes onto it.
 * This way we calculate the way we can do actions after a block was broken/placed.
 */
@RequiredArgsConstructor
public class ProjectedLevelState {
    private static final BlockStateMeta AIR_BLOCK_STATE = new BlockStateMeta(BlockType.AIR, BlockShapeType.getById(0));

    private final LevelState levelState;
    private final ChunkHolder chunkHolder;
    private final Object2ObjectOpenCustomHashMap<SWVec3i, BlockStateMeta> blockChanges;

    public ProjectedLevelState(LevelState levelState) {
        this.levelState = levelState;
        this.chunkHolder = levelState.chunks().immutableCopy();
        this.blockChanges = new Object2ObjectOpenCustomHashMap<>(VectorHelper.VECTOR3I_HASH_STRATEGY);
    }

    public ProjectedLevelState withChangeToSolidBlock(SWVec3i position) {
        var blockChanges = this.blockChanges.clone();
        blockChanges.put(position, Costs.SOLID_PLACED_BLOCK_STATE);

        return new ProjectedLevelState(levelState, chunkHolder, blockChanges);
    }

    public ProjectedLevelState withChangeToAir(SWVec3i position) {
        var blockChanges = this.blockChanges.clone();
        blockChanges.put(position, AIR_BLOCK_STATE);

        return new ProjectedLevelState(levelState, chunkHolder, blockChanges);
    }

    public ProjectedLevelState withChanges(SWVec3i[] air, SWVec3i solid) {
        var blockChanges = this.blockChanges.clone();
        blockChanges.ensureCapacity(blockChanges.size()
                + (air != null ? air.length : 0)
                + (solid != null ? 1 : 0));

        if (air != null) {
            for (var position : air) {
                if (position == null) {
                    continue;
                }

                blockChanges.put(position, AIR_BLOCK_STATE);
            }
        }

        if (solid != null) {
            blockChanges.put(solid, Costs.SOLID_PLACED_BLOCK_STATE);
        }

        return new ProjectedLevelState(levelState, chunkHolder, blockChanges);
    }

    public boolean isOutsideBuildHeight(SWVec3i position) {
        return levelState.isOutsideBuildHeight(position.y);
    }

    public BlockStateMeta getBlockStateAt(SWVec3i position) {
        var blockChange = blockChanges.get(position);
        if (blockChange != null) {
            return blockChange;
        }

        return chunkHolder.getBlockStateAt(position.x, position.y, position.z);
    }

    public boolean isChanged(SWVec3i position) {
        return blockChanges.containsKey(position);
    }
}
