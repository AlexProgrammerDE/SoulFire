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
import lombok.RequiredArgsConstructor;
import net.pistonmaster.serverwrecker.protocol.bot.model.ChunkKey;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@RequiredArgsConstructor
public class ChunkHolder {
    private final Int2ObjectMap<ChunkData> chunks = new Int2ObjectOpenHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final LevelState levelState;

    public ChunkData getChunk(int x, int z) {
        lock.readLock().lock();
        try {
            return chunks.get(ChunkKey.calculateHash(x, z));
        } finally {
            lock.readLock().unlock();
        }
    }

    public ChunkData getChunk(Vector3i block) {
        lock.readLock().lock();
        try {
            return chunks.get(ChunkKey.calculateHash(block));
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean isChunkLoaded(int x, int z) {
        lock.readLock().lock();
        try {
            return chunks.containsKey(ChunkKey.calculateHash(x, z));
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean isChunkLoaded(Vector3i block) {
        lock.readLock().lock();
        try {
            return chunks.containsKey(ChunkKey.calculateHash(block));
        } finally {
            lock.readLock().unlock();
        }
    }

    public void removeChunk(int x, int z) {
        lock.writeLock().lock();
        try {
            chunks.remove(ChunkKey.calculateHash(x, z));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public ChunkData getOrCreateChunk(int x, int z) {
        lock.writeLock().lock();
        try {
            return chunks.computeIfAbsent(ChunkKey.calculateHash(x, z), (key) -> new ChunkData(levelState));
        } finally {
            lock.writeLock().unlock();
        }
    }
}
