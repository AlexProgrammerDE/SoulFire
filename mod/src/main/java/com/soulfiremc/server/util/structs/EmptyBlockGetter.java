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
package com.soulfiremc.server.util.structs;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

public final class EmptyBlockGetter implements BlockGetter {
  public static final EmptyBlockGetter INSTANCE = new EmptyBlockGetter();

  private EmptyBlockGetter() {}

  @Override
  public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
    return null;
  }

  @Override
  public BlockState getBlockState(BlockPos pos) {
    return Blocks.VOID_AIR.defaultBlockState();
  }

  @Override
  public FluidState getFluidState(BlockPos pos) {
    return Fluids.EMPTY.defaultFluidState();
  }

  @Override
  public int getHeight() {
    return Integer.MAX_VALUE;
  }

  @Override
  public int getMinY() {
    return Integer.MIN_VALUE;
  }
}
