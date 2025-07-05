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
package com.soulfiremc.server.util;

import com.soulfiremc.server.util.structs.IDBooleanMap;
import com.soulfiremc.server.util.structs.IDMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.VoxelShape;

@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public final class SFBlockHelpers {
  // A player can jump up 1.25 blocks
  private static final double SAFE_BLOCK_MIN_HEIGHT = 0.75;
  public static IDMap<BlockState, VoxelShape> RAW_COLLISION_SHAPES = new IDMap<>(
    Block.BLOCK_STATE_REGISTRY, blockState -> blockState.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
  public static IDBooleanMap<BlockState> COLLISION_SHAPE_EMPTY = new IDBooleanMap<>(
    Block.BLOCK_STATE_REGISTRY, blockState -> RAW_COLLISION_SHAPES.get(blockState).isEmpty());
  public static IDBooleanMap<BlockState> COLLISION_SHAPE_NOT_EMPTY = new IDBooleanMap<>(
    Block.BLOCK_STATE_REGISTRY, blockState -> !COLLISION_SHAPE_EMPTY.get(blockState));
  public static IDBooleanMap<BlockState> COLLISION_SHAPE_FULL_BLOCK = new IDBooleanMap<>(
    Block.BLOCK_STATE_REGISTRY, blockState -> Block.isShapeFullBlock(RAW_COLLISION_SHAPES.get(blockState)));
  public static IDBooleanMap<BlockState> COLLISION_SHAPE_TOP_FACE_FULL = new IDBooleanMap<>(
    Block.BLOCK_STATE_REGISTRY, blockState -> Block.isFaceFull(RAW_COLLISION_SHAPES.get(blockState), Direction.UP));

  private SFBlockHelpers() {}

  public static boolean isFullBlock(BlockState state) {
    return COLLISION_SHAPE_FULL_BLOCK.get(state);
  }

  @SuppressWarnings("deprecation")
  public static boolean isBlockFree(BlockState blockState) {
    return COLLISION_SHAPE_EMPTY.get(blockState)
      && blockState.getFluidState().getType() == Fluids.EMPTY
      && !blockState.blocksMotion()
      && !affectsTouchMovementSpeed(blockState.getBlock());
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
    return isTopFullBlock(state) && !isHurtWhenStoodOn(state);
  }

  public static boolean isTopFullBlock(BlockState blockState) {
    return COLLISION_SHAPE_TOP_FACE_FULL.get(blockState);
  }

  public static boolean isDiggable(Block type) {
    return type.defaultDestroyTime() != -1;
  }

  public static boolean isUsableBlockItem(Block blockType) {
    return BlockItems.hasItem(blockType);
  }

  public static boolean isEmptyBlock(Block type) {
    // Void air stands for not loaded blocks, so we do not know what is there
    return type instanceof AirBlock && type != Blocks.VOID_AIR;
  }

  @SuppressWarnings("deprecation")
  public static boolean isSuffocating(BlockState state) {
    // TODO: Handle edge cases like pistons
    return state.blocksMotion() && isFullBlock(state);
  }
}
