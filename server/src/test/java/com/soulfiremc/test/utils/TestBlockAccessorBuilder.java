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
package com.soulfiremc.test.utils;

import com.soulfiremc.server.data.BlockState;
import com.soulfiremc.server.data.BlockType;
import com.soulfiremc.server.protocol.bot.block.BlockAccessor;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.Map;

public class TestBlockAccessorBuilder {
  private final Map<Vector3i, BlockState> blocks = new Object2ObjectOpenHashMap<>();
  private final BlockState defaultBlock;
  private int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
  private int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

  public TestBlockAccessorBuilder() {
    this(BlockState.forDefaultBlockType(BlockType.AIR));
  }

  public TestBlockAccessorBuilder(BlockState defaultBlock) {
    this.defaultBlock = defaultBlock;
  }

  public void setBlockAt(int x, int y, int z, BlockType block) {
    blocks.put(Vector3i.from(x, y, z), BlockState.forDefaultBlockType(block));
    minX = Math.min(minX, x);
    minY = Math.min(minY, y);
    minZ = Math.min(minZ, z);
    maxX = Math.max(maxX, x);
    maxY = Math.max(maxY, y);
    maxZ = Math.max(maxZ, z);
  }

  public BlockAccessor build() {
    int sizeX = maxX - minX + 1;
    int sizeY = maxY - minY + 1;
    int sizeZ = maxZ - minZ + 1;
    BlockState[][][] arrayBlocks = new BlockState[sizeX][sizeY][sizeZ];

    for (Map.Entry<Vector3i, BlockState> entry : blocks.entrySet()) {
      Vector3i pos = entry.getKey();
      arrayBlocks[pos.getX() - minX][pos.getY() - minY][pos.getZ() - minZ] = entry.getValue();
    }

    return new TestBlockAccessor(arrayBlocks, defaultBlock, -minX, -minY, -minZ);
  }

  private record TestBlockAccessor(BlockState[][][] blocks, BlockState defaultBlock, int offsetX, int offsetY, int offsetZ) implements BlockAccessor {
    @Override
    public BlockState getBlockState(int x, int y, int z) {
      int adjX = x + offsetX;
      int adjY = y + offsetY;
      int adjZ = z + offsetZ;
      if (adjX < 0 || adjX >= blocks.length || adjY < 0 || adjY >= blocks[0].length || adjZ < 0 || adjZ >= blocks[0][0].length) {
        return defaultBlock;
      }

      BlockState value = blocks[adjX][adjY][adjZ];
      return value != null ? value : defaultBlock;
    }
  }
}
