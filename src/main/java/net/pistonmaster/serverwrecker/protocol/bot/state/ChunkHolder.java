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
package net.pistonmaster.serverwrecker.protocol.bot.state;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.pistonmaster.serverwrecker.data.ResourceData;
import net.pistonmaster.serverwrecker.protocol.bot.block.BlockStateMeta;
import net.pistonmaster.serverwrecker.protocol.bot.model.ChunkKey;
import net.pistonmaster.serverwrecker.protocol.bot.utils.SectionUtils;
import net.pistonmaster.serverwrecker.util.NoopLock;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ChunkHolder {
    private final Int2ObjectMap<ChunkData> chunks = new Int2ObjectOpenHashMap<>();
    private final Lock readLock;
    private final Lock writeLock;

    public ChunkHolder() {
        ReadWriteLock lock = new ReentrantReadWriteLock();
        this.readLock = lock.readLock();
        this.writeLock = lock.writeLock();
    }

    private ChunkHolder(ChunkHolder chunkHolder) {
        this.chunks.putAll(chunkHolder.chunks);
        this.readLock = new NoopLock();
        this.writeLock = new NoopLock();
    }

    public ChunkData getChunk(int chunkX, int chunkZ) {
        return getChunkFromSection(ChunkKey.calculateHash(chunkX, chunkZ));
    }

    public ChunkData getChunk(Vector3i block) {
        return getChunk(SectionUtils.blockToSection(block.getX()), SectionUtils.blockToSection(block.getZ()));
    }

    private ChunkData getChunkFromSection(int sectionIndex) {
        readLock.lock();
        try {
            return chunks.get(sectionIndex);
        } finally {
            readLock.unlock();
        }
    }

    public boolean isChunkLoaded(int x, int z) {
        readLock.lock();
        try {
            return chunks.containsKey(ChunkKey.calculateHash(x, z));
        } finally {
            readLock.unlock();
        }
    }

    public boolean isChunkLoaded(Vector3i block) {
        return isChunkLoaded(SectionUtils.blockToSection(block.getX()), SectionUtils.blockToSection(block.getZ()));
    }

    public void removeChunk(int x, int z) {
        writeLock.lock();
        try {
            chunks.remove(ChunkKey.calculateHash(x, z));
        } finally {
            writeLock.unlock();
        }
    }

    public ChunkData getOrCreateChunk(int x, int z, LevelState levelState) {
        writeLock.lock();
        try {
            return chunks.computeIfAbsent(ChunkKey.calculateHash(x, z), (key) ->
                    new ChunkData(levelState));
        } finally {
            writeLock.unlock();
        }
    }

    public Optional<BlockStateMeta> getBlockStateAt(int x, int y, int z) {
        var chunkData = getChunk(SectionUtils.blockToSection(x), SectionUtils.blockToSection(z));

        // Out of world
        if (chunkData == null) {
            return Optional.empty();
        }

        return Optional.of(ResourceData.GLOBAL_BLOCK_PALETTE
                .getBlockStateForStateId(chunkData.getBlock(x, y, z)));
    }

    public ChunkHolder immutableCopy() {
        return new ChunkHolder(this);
    }
}
