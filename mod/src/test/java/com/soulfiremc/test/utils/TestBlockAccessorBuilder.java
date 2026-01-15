/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.test.utils;

import com.soulfiremc.server.pathfinding.SFVec3i;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public final class TestBlockAccessorBuilder {
  private final Map<SFVec3i, BlockState> blocks = new Object2ObjectOpenHashMap<>();
  private final BlockState defaultBlock;
  private int minX = Integer.MAX_VALUE;
  private int minY = Integer.MAX_VALUE;
  private int minZ = Integer.MAX_VALUE;
  private int maxX = Integer.MIN_VALUE;
  private int maxY = Integer.MIN_VALUE;
  private int maxZ = Integer.MIN_VALUE;

  public TestBlockAccessorBuilder() {
    this(Blocks.AIR.defaultBlockState());
  }

  public TestBlockAccessorBuilder(BlockState defaultBlock) {
    this.defaultBlock = defaultBlock;
  }

  public void setBlockAt(int x, int y, int z, Block block) {
    var targetState = block.defaultBlockState();
    if (targetState == defaultBlock) {
      blocks.remove(SFVec3i.from(x, y, z));
    } else {
      blocks.put(SFVec3i.from(x, y, z), targetState);
    }

    minX = Math.min(minX, x);
    minY = Math.min(minY, y);
    minZ = Math.min(minZ, z);
    maxX = Math.max(maxX, x);
    maxY = Math.max(maxY, y);
    maxZ = Math.max(maxZ, z);
  }

  public BlockGetter build() {
    var sizeX = maxX - minX + 1;
    var sizeY = maxY - minY + 1;
    var sizeZ = maxZ - minZ + 1;
    var arrayBlocks = new BlockState[sizeX][sizeY][sizeZ];

    for (var entry : blocks.entrySet()) {
      var pos = entry.getKey();
      arrayBlocks[pos.x - minX][pos.y - minY][pos.z - minZ] = entry.getValue();
    }

    return new TestBlockAccessor(arrayBlocks, defaultBlock, -minX, -minY, -minZ);
  }

  private record TestBlockAccessor(BlockState[][][] blocks, BlockState defaultBlock, int offsetX, int offsetY, int offsetZ) implements BlockGetter {
    @Override
    public @Nullable BlockEntity getBlockEntity(@NonNull BlockPos pos) {
      return null;
    }

    @Override
    public @NonNull BlockState getBlockState(@NonNull BlockPos pos) {
      var adjX = pos.getX() + offsetX;
      var adjY = pos.getY() + offsetY;
      var adjZ = pos.getZ() + offsetZ;
      if (adjX < 0 || adjX >= blocks.length || adjY < 0 || adjY >= blocks[0].length || adjZ < 0 || adjZ >= blocks[0][0].length) {
        return defaultBlock;
      }

      var value = blocks[adjX][adjY][adjZ];
      return value != null ? value : defaultBlock;
    }

    @Override
    public @NonNull FluidState getFluidState(@NonNull BlockPos pos) {
      return Fluids.EMPTY.defaultFluidState();
    }

    @Override
    public int getHeight() {
      return blocks[0].length;
    }

    @Override
    public int getMinY() {
      return -offsetY;
    }
  }
}
