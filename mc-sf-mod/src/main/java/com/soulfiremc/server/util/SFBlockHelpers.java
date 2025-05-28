package com.soulfiremc.server.util;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public final class SFBlockHelpers {
  // A player can jump up 1.25 blocks
  private static final double SAFE_BLOCK_MIN_HEIGHT = 0.75;

  private SFBlockHelpers() {}

  public static boolean isFullBlock(BlockState state) {
    return state.collisionShape().isFullBlock();
  }

  @SuppressWarnings("deprecation")
  public static boolean isBlockFree(BlockState blockState) {
    return blockState.collisionShape().hasNoCollisions()
      && blockState.getFluidState().getType() == Fluids.EMPTY
      && !blockState.blocksMotion()
      && !affectsTouchMovementSpeed(blockState.blockType());
  }

  public static boolean affectsTouchMovementSpeed(Block blockType) {
    return blockType == Blocks.COBWEB
      || blockType == Blocks.POWDER_SNOW
      || blockType == Blocks.SOUL_SAND
      || blockType == Blocks.SOUL_SOIL
      || blockType == Blocks.HONEY_BLOCK
      || blockType == Blocks.SLIME_BLOCK;
  }

  public static boolean isHurtOnTouchFluid(Fluid fluidType) {
    return fluidType == Fluids.LAVA
      || fluidType == Fluids.FLOWING_LAVA;
  }

  public static boolean isHurtOnTouchSide(BlockState blockState) {
    var blockType = blockState.getBlock();
    return blockType == Blocks.CACTUS
      || blockType == Blocks.SWEET_BERRY_BUSH
      || blockType == Blocks.WITHER_ROSE
      || blockType == Blocks.FIRE
      || blockType == Blocks.SOUL_FIRE
      // Not hurt, but definitely slows you down
      || blockType == Blocks.COBWEB
      || blockType == Blocks.POWDER_SNOW
      || isHurtOnTouchFluid(blockState.getFluidState().getType());
  }

  public static boolean isHurtWhenStoodOn(BlockState blockState) {
    var blockType = blockState.getBlock();
    return blockType == Blocks.MAGMA_BLOCK
      || isHurtOnTouchFluid(blockState.getFluidState().getType());
  }

  public static boolean isSafeBlockToStandOn(BlockState state) {
    return isTopFullBlock(state.collisionShape()) && !isHurtWhenStoodOn(state);
  }

  public static boolean isTopFullBlock(BlockShapeGroup shapeGroup) {
    for (var shape : shapeGroup.blockShapes()) {
      if (shape.isFullBlockXZ() && shape.maxY >= SAFE_BLOCK_MIN_HEIGHT) {
        return true;
      }
    }

    return false;
  }

  public static boolean isDiggable(Block type) {
    return type.destroyTime() != -1;
  }

  public static boolean isEmptyBlock(BlockState state) {
    // Void air stands for not loaded blocks, so we do not know what is there
    return state.isAir() && state.getBlock() != Blocks.VOID_AIR;
  }
}
