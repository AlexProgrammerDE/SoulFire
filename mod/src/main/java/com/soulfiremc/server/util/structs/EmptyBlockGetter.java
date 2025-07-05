package com.soulfiremc.server.util.structs;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

public class EmptyBlockGetter implements BlockGetter {
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
