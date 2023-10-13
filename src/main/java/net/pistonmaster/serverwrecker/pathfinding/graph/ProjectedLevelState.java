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

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.pathfinding.Costs;
import net.pistonmaster.serverwrecker.protocol.bot.block.BlockStateMeta;
import net.pistonmaster.serverwrecker.protocol.bot.state.ChunkHolder;
import net.pistonmaster.serverwrecker.protocol.bot.state.LevelState;
import net.pistonmaster.serverwrecker.util.VectorHelper;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.Optional;

/**
 * An immutable representation of the world state.
 * This takes a world state and projects changes onto it.
 * This way we calculate the way we can do actions after a block was broken/placed.
 */
@RequiredArgsConstructor
public class ProjectedLevelState {
    private final ChunkHolder chunkHolder;
    private final Object2ObjectMap<Vector3i, BlockStateMeta> blockChanges;
    private final int blockChangesHash;

    public ProjectedLevelState(LevelState levelState) {
        this.chunkHolder = levelState.getChunks().immutableCopy();
        this.blockChanges = Object2ObjectMaps.emptyMap();
        this.blockChangesHash = blockChanges.hashCode();
    }

    public ProjectedLevelState withChangeToSolidBlock(Vector3i position) {
        var blockChanges = new Object2ObjectOpenCustomHashMap<Vector3i, BlockStateMeta>(
                this.blockChanges.size() + 1, VectorHelper.VECTOR3I_HASH_STRATEGY);
        blockChanges.putAll(this.blockChanges);
        blockChanges.put(position, Costs.SOLID_PLACED_BLOCK_STATE);

        return new ProjectedLevelState(chunkHolder, blockChanges, blockChanges.hashCode());
    }

    public ProjectedLevelState withChangeToAir(Vector3i position) {
        var blockChanges = new Object2ObjectOpenCustomHashMap<Vector3i, BlockStateMeta>(
                this.blockChanges.size() + 1, VectorHelper.VECTOR3I_HASH_STRATEGY);
        blockChanges.putAll(this.blockChanges);
        blockChanges.put(position, BlockStateMeta.AIR_BLOCK_STATE);

        return new ProjectedLevelState(chunkHolder, blockChanges, blockChanges.hashCode());
    }

    public Optional<BlockStateMeta> getBlockStateAt(Vector3i position) {
        var blockChange = blockChanges.get(position);
        if (blockChange != null) {
            return Optional.of(blockChange);
        }

        return chunkHolder.getBlockStateAt(position);
    }

    public boolean isChanged(Vector3i position) {
        return blockChanges.containsKey(position);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ProjectedLevelState that) {
            return blockChanges.equals(that.blockChanges);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return blockChangesHash;
    }
}
