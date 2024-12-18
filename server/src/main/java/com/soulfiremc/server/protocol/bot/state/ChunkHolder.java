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
import com.soulfiremc.server.util.SectionUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ChunkHolder implements BlockAccessor {
  private static final BlockState VOID_AIR_BLOCK_STATE = BlockState.forDefaultBlockType(BlockType.VOID_AIR);
  private final Long2ObjectOpenHashMap<ChunkData> chunks = new Long2ObjectOpenHashMap<>();
  private final Lock readLock;
  private final Lock writeLock;
  private final LevelHeightAccessor levelHeightAccessor;

  public ChunkHolder(LevelHeightAccessor levelHeightAccessor) {
    ReadWriteLock lock = new ReentrantReadWriteLock();
    this.readLock = lock.readLock();
    this.writeLock = lock.writeLock();
    this.levelHeightAccessor = levelHeightAccessor;
  }

  private ChunkHolder(ChunkHolder chunkHolder) {
    this.chunks.putAll(chunkHolder.chunks);
    this.readLock = null;
    this.writeLock = null;
    this.levelHeightAccessor = chunkHolder.levelHeightAccessor;
  }

  public ChunkData getChunk(int chunkX, int chunkZ) {
    return getChunkFromSection(ChunkKey.calculateKey(chunkX, chunkZ));
  }

  public ChunkData getChunk(Vector3i block) {
    return getChunk(
      SectionUtils.blockToSection(block.getX()), SectionUtils.blockToSection(block.getZ()));
  }

  private ChunkData getChunkFromSection(long sectionIndex) {
    if (readLock != null) {
      readLock.lock();
    }

    try {
      return chunks.get(sectionIndex);
    } finally {
      if (readLock != null) {
        readLock.unlock();
      }
    }
  }

  public boolean isChunkSectionLoaded(int sectionX, int sectionZ) {
    if (readLock != null) {
      readLock.lock();
    }

    try {
      return chunks.containsKey(ChunkKey.calculateKey(sectionX, sectionZ));
    } finally {
      if (readLock != null) {
        readLock.unlock();
      }
    }
  }

  public boolean isChunkPositionLoaded(int blockX, int blockZ) {
    return isChunkSectionLoaded(SectionUtils.blockToSection(blockX), SectionUtils.blockToSection(blockZ));
  }

  public void removeChunkSection(int sectionX, int sectionZ) {
    if (writeLock != null) {
      writeLock.lock();
    }

    try {
      chunks.remove(ChunkKey.calculateKey(sectionX, sectionZ));
    } finally {
      if (writeLock != null) {
        writeLock.unlock();
      }
    }
  }

  public boolean hasChunksAt(int fromBlockX, int fromBlockZ, int toBlockX, int toBlockZ) {
    var fromSectionX = SectionUtils.blockToSection(fromBlockX);
    var toSectionX = SectionUtils.blockToSection(toBlockX);
    var fromSectionZ = SectionUtils.blockToSection(fromBlockZ);
    var toSectionZ = SectionUtils.blockToSection(toBlockZ);

    for (var sectionX = fromSectionX; sectionX <= toSectionX; sectionX++) {
      for (var sectionZ = fromSectionZ; sectionZ <= toSectionZ; sectionZ++) {
        if (!this.isChunkSectionLoaded(sectionX, sectionZ)) {
          return false;
        }
      }
    }

    return true;
  }

  public ChunkData getOrCreateChunkSection(int sectionX, int sectionZ) {
    if (writeLock != null) {
      writeLock.lock();
    }

    try {
      return chunks.computeIfAbsent(
        ChunkKey.calculateKey(sectionX, sectionZ), (key) -> new ChunkData(levelHeightAccessor));
    } finally {
      if (writeLock != null) {
        writeLock.unlock();
      }
    }
  }

  @Override
  public BlockState getBlockState(int x, int y, int z) {
    if (levelHeightAccessor.isOutsideBuildHeight(y)) {
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

  public ChunkHolder immutableCopy() {
    return new ChunkHolder(this);
  }
}
