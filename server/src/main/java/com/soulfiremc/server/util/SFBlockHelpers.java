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

import com.soulfiremc.server.data.*;
import com.soulfiremc.server.protocol.bot.state.TagsState;

@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public class SFBlockHelpers {
  // A player can jump up 1.25 blocks
  private static final double SAFE_BLOCK_MIN_HEIGHT = 0.75;

  private SFBlockHelpers() {}

  public static boolean isFullBlock(BlockState state) {
    return state.collisionShape().isFullBlock();
  }

  public static boolean isHurtOnTouchFluid(FluidType fluidType) {
    return fluidType == FluidType.LAVA
      || fluidType == FluidType.FLOWING_LAVA;
  }

  public static boolean isHurtOnTouchSide(BlockState blockState) {
    var blockType = blockState.blockType();
    return blockType == BlockType.CACTUS
      || blockType == BlockType.SWEET_BERRY_BUSH
      || blockType == BlockType.WITHER_ROSE
      || blockType == BlockType.FIRE
      || blockType == BlockType.SOUL_FIRE
      || isHurtOnTouchFluid(blockState.fluidState().type());
  }

  public static boolean isHurtWhenStoodOn(BlockState blockState) {
    var blockType = blockState.blockType();
    return blockType == BlockType.MAGMA_BLOCK
      || isHurtOnTouchFluid(blockState.fluidState().type());
  }

  public static boolean isSafeBlockToStandOn(BlockState state) {
    return isTopFullBlock(state.collisionShape()) && !isHurtWhenStoodOn(state);
  }

  public static boolean isStairsBlockToStandOn(TagsState tagsState, BlockState state) {
    return tagsState.is(state.blockType(), BlockTags.STAIRS) && !isHurtWhenStoodOn(state);
  }

  public static boolean isTopFullBlock(BlockShapeGroup shapeGroup) {
    for (var shape : shapeGroup.blockShapes()) {
      if (shape.isFullBlockXZ() && shape.maxY >= SAFE_BLOCK_MIN_HEIGHT) {
        return true;
      }
    }

    return false;
  }

  public static boolean isDiggable(BlockType type) {
    return type.destroyTime() != -1;
  }

  public static boolean isUsableBlockItem(BlockType blockType) {
    return BlockItems.hasItemType(blockType);
  }

  public static boolean isEmptyBlock(BlockType type) {
    // Void air stands for not loaded blocks, so we do not know what is there
    return type.air() && type != BlockType.VOID_AIR;
  }

  public static boolean isSuffocating(BlockState state) {
    // TODO: Handle edge cases like pistons
    return state.blockType().blocksMotion() && isFullBlock(state);
  }
}
