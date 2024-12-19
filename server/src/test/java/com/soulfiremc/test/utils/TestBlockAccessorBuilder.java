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
import com.soulfiremc.server.util.VectorHelper;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public class TestBlockAccessorBuilder {
  private final Long2ObjectMap<BlockState> blocks = new Long2ObjectOpenHashMap<>();
  private final BlockState defaultBlock;

  public TestBlockAccessorBuilder() {
    this(BlockState.forDefaultBlockType(BlockType.AIR));
  }

  public TestBlockAccessorBuilder(BlockState defaultBlock) {
    this.defaultBlock = defaultBlock;
  }

  public void setBlockAt(int x, int y, int z, BlockType block) {
    blocks.put(VectorHelper.asLong(x, y, z), BlockState.forDefaultBlockType(block));
  }

  public BlockAccessor build() {
    return new TestBlockAccessor(blocks, defaultBlock);
  }

  private record TestBlockAccessor(Long2ObjectMap<BlockState> blocks, BlockState defaultBlock) implements BlockAccessor {
    @Override
    public BlockState getBlockState(int x, int y, int z) {
      return blocks.getOrDefault(VectorHelper.asLong(x, y, z), defaultBlock);
    }
  }
}
