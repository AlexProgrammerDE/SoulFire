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

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.protocol.bot.block.BlockStateMeta;
import net.pistonmaster.serverwrecker.protocol.bot.state.LevelState;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An immutable representation of the world state.
 * This takes a world state and projects changes onto it.
 * This way we calculate the way we can do actions after a block was broken/placed.
 */
@RequiredArgsConstructor
public class ProjectedLevelState {
    private final LevelState levelState;
    private final Map<Vector3i, BlockStateMeta> blockChanges;
    private final int blockChangesHash;
    private final Map<Vector3i, BlockStateMetaValue> blockStateCache;

    public ProjectedLevelState(LevelState levelState) {
        this.levelState = levelState;
        this.blockChanges = Collections.emptyMap();
        this.blockChangesHash = blockChanges.hashCode();
        this.blockStateCache = new Object2ObjectLinkedOpenHashMap<>();
    }

    public ProjectedLevelState withChange(Vector3i position, BlockStateMeta blockStateMeta) {
        Map<Vector3i, BlockStateMeta> blockChanges = new Object2ObjectArrayMap<>(this.blockChanges.size() + 1);
        blockChanges.putAll(this.blockChanges);
        blockChanges.put(position, blockStateMeta);

        Map<Vector3i, BlockStateMetaValue> blockStateCache = new Object2ObjectLinkedOpenHashMap<>(this.blockStateCache);
        blockStateCache.put(position, new BlockStateMetaValue(blockStateMeta, true));

        return new ProjectedLevelState(levelState, blockChanges, blockChanges.hashCode(), blockStateCache);
    }

    public Optional<BlockStateMeta> getBlockStateAt(Vector3i position) {
        var checkShrink = new AtomicBoolean(false);
        var meta = Optional.ofNullable(blockStateCache.computeIfAbsent(position, k -> {
            var toInsert = levelState.getBlockStateAt(k)
                    .map(v -> new BlockStateMetaValue(v, false)).orElse(null);

            if (toInsert != null) {
                checkShrink.set(true);
            }

            return toInsert;
        }));

        // Reduce the cache size if it gets too big
        if (checkShrink.get()) {
            var drainSize = 200 + blockChanges.size();
            if (blockStateCache.size() > drainSize) {
                // Remove the 100 eldest elements that are not changes (To prevent re-fetching)
                var limitCounter = new AtomicInteger(0);
                blockStateCache.values().removeIf(value -> {
                    if (value.isChange()) {
                        return false;
                    }

                    return limitCounter.getAndIncrement() < 100;
                });
            }
        }

        return meta.map(BlockStateMetaValue::meta);
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

    private record BlockStateMetaValue(BlockStateMeta meta, boolean isChange) {
    }
}
