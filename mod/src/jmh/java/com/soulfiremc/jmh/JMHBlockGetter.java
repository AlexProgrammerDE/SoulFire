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
package com.soulfiremc.jmh;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

public class JMHBlockGetter implements BlockGetter {
  private final Long2ObjectMap<BlockState> blocks = new Long2ObjectOpenHashMap<>();
  private final int minY;
  private final int height;

  public JMHBlockGetter(int minY, int maxY) {
    this.minY = minY;
    this.height = maxY - minY;
  }

  public void setBlockState(int x, int y, int z, BlockState state) {
    blocks.put(BlockPos.asLong(x, y, z), state);
  }

  @Override
  public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
    return null;
  }

  @Override
  public BlockState getBlockState(BlockPos pos) {
    var state = blocks.get(pos.asLong());
    return state != null ? state : Blocks.AIR.defaultBlockState();
  }

  @Override
  public FluidState getFluidState(BlockPos pos) {
    return Fluids.EMPTY.defaultFluidState();
  }

  @Override
  public int getHeight() {
    return height;
  }

  @Override
  public int getMinY() {
    return minY;
  }
}
