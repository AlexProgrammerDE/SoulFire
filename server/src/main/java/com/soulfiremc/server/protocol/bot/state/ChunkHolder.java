/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.protocol.bot.state;

import com.soulfiremc.server.data.BlockState;
import com.soulfiremc.server.data.BlockType;
import com.soulfiremc.server.protocol.bot.block.BlockAccessor;
import com.soulfiremc.server.protocol.bot.block.GlobalBlockPalette;
import com.soulfiremc.server.protocol.bot.model.ChunkKey;
import com.soulfiremc.server.protocol.bot.utils.SectionUtils;
import com.soulfiremc.server.util.NoopLock;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.cloudburstmc.math.vector.Vector3i;

public class ChunkHolder implements BlockAccessor {
  private static final BlockState VOID_AIR_BLOCK_STATE =
    BlockState.forDefaultBlockType(BlockType.VOID_AIR);
  private final Long2ObjectOpenHashMap<ChunkData> chunks = new Long2ObjectOpenHashMap<>();
  private final Lock readLock;
  private final Lock writeLock;
  private final int minBuildHeight;
  private final int maxBuildHeight;
  private final int minSection;
  private final int maxSection;
  private final int sectionsCount;

  public ChunkHolder(int minBuildHeight, int maxBuildHeight) {
    ReadWriteLock lock = new ReentrantReadWriteLock();
    this.readLock = lock.readLock();
    this.writeLock = lock.writeLock();
    this.minBuildHeight = minBuildHeight;
    this.maxBuildHeight = maxBuildHeight;

    // Precalculate section values
    this.minSection = SectionUtils.blockToSection(minBuildHeight);
    this.maxSection = SectionUtils.blockToSection(maxBuildHeight - 1) + 1;
    this.sectionsCount = this.maxSection - this.minSection;
  }

  private ChunkHolder(ChunkHolder chunkHolder) {
    this.chunks.putAll(chunkHolder.chunks);
    this.readLock = new NoopLock();
    this.writeLock = new NoopLock();
    this.minBuildHeight = chunkHolder.minBuildHeight;
    this.maxBuildHeight = chunkHolder.maxBuildHeight;
    this.minSection = chunkHolder.minSection;
    this.maxSection = chunkHolder.maxSection;
    this.sectionsCount = chunkHolder.sectionsCount;
  }

  public ChunkData getChunk(int chunkX, int chunkZ) {
    return getChunkFromSection(ChunkKey.calculateKey(chunkX, chunkZ));
  }

  public ChunkData getChunk(Vector3i block) {
    return getChunk(
      SectionUtils.blockToSection(block.getX()), SectionUtils.blockToSection(block.getZ()));
  }

  private ChunkData getChunkFromSection(long sectionIndex) {
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
      return chunks.containsKey(ChunkKey.calculateKey(x, z));
    } finally {
      readLock.unlock();
    }
  }

  public boolean isChunkLoaded(Vector3i block) {
    return isChunkLoaded(
      SectionUtils.blockToSection(block.getX()), SectionUtils.blockToSection(block.getZ()));
  }

  public void removeChunk(int x, int z) {
    writeLock.lock();
    try {
      chunks.remove(ChunkKey.calculateKey(x, z));
    } finally {
      writeLock.unlock();
    }
  }

  public ChunkData getOrCreateChunk(int x, int z) {
    writeLock.lock();
    try {
      return chunks.computeIfAbsent(
        ChunkKey.calculateKey(x, z), (key) -> new ChunkData(minSection, sectionsCount, false));
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public BlockState getBlockState(int x, int y, int z) {
    if (isOutsideBuildHeight(y)) {
      return VOID_AIR_BLOCK_STATE;
    }

    var chunkData = getChunk(SectionUtils.blockToSection(x), SectionUtils.blockToSection(z));

    // Out of world
    if (chunkData == null) {
      return VOID_AIR_BLOCK_STATE;
    }

    return GlobalBlockPalette.INSTANCE.getBlockStateForStateId(chunkData.getBlock(x, y, z));
  }

  public Long2ObjectMap<ChunkData> getChunks() {
    return chunks.clone();
  }

  public boolean isOutsideBuildHeight(int y) {
    return y < minBuildHeight || y >= maxBuildHeight;
  }

  public ChunkHolder immutableCopy() {
    return new ChunkHolder(this);
  }
}
