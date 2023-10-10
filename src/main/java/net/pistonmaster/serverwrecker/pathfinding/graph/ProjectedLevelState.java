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

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.pathfinding.Costs;
import net.pistonmaster.serverwrecker.protocol.bot.block.BlockStateMeta;
import net.pistonmaster.serverwrecker.protocol.bot.state.ChunkHolder;
import net.pistonmaster.serverwrecker.protocol.bot.state.LevelState;
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
    private final Int2ObjectMap<BlockStateMeta> blockChanges;
    private final int blockChangesHash;

    public ProjectedLevelState(LevelState levelState) {
        this.chunkHolder = levelState.getChunks().immutableCopy();
        this.blockChanges = Int2ObjectMaps.emptyMap();
        this.blockChangesHash = blockChanges.hashCode();
    }

    public ProjectedLevelState withChangeToSolidBlock(Vector3i position) {
        var blockChanges = new Int2ObjectArrayMap<BlockStateMeta>(this.blockChanges.size() + 1);
        blockChanges.putAll(this.blockChanges);
        blockChanges.put(position.hashCode(), Costs.SOLID_PLACED_BLOCK_STATE);

        return new ProjectedLevelState(chunkHolder, blockChanges, blockChanges.hashCode());
    }

    public ProjectedLevelState withChangeToAir(Vector3i position) {
        var blockChanges = new Int2ObjectArrayMap<BlockStateMeta>(this.blockChanges.size() + 1);
        blockChanges.putAll(this.blockChanges);
        blockChanges.put(position.hashCode(), BlockStateMeta.AIR_BLOCK_STATE);

        return new ProjectedLevelState(chunkHolder, blockChanges, blockChanges.hashCode());
    }

    public Optional<BlockStateMeta> getBlockStateAt(Vector3i position) {
        var blockChange = blockChanges.get(position.hashCode());
        if (blockChange != null) {
            return Optional.of(blockChange);
        }

        return chunkHolder.getBlockStateAt(position);
    }

    public boolean isChanged(Vector3i position) {
        return blockChanges.containsKey(position.hashCode());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (ProjectedLevelState) o;
        return blockChangesHash == that.blockChangesHash;
    }

    @Override
    public int hashCode() {
        return blockChangesHash;
    }
}
