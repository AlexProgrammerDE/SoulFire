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
package com.soulfiremc.server.pathfinding.minecraft;

import com.soulfiremc.server.pathfinding.world.BlockState;
import com.soulfiremc.server.pathfinding.world.BlockType;
import com.soulfiremc.server.util.SFBlockHelpers;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.material.Fluids;

/// Minecraft implementation of BlockState interface.
@RequiredArgsConstructor
public final class MinecraftBlockState implements BlockState {
  private final net.minecraft.world.level.block.state.BlockState mcState;

  public net.minecraft.world.level.block.state.BlockState unwrap() {
    return mcState;
  }

  @Override
  public int globalId() {
    return Block.BLOCK_STATE_REGISTRY.getId(mcState);
  }

  @Override
  public BlockType blockType() {
    return new MinecraftBlockType(mcState.getBlock());
  }

  @Override
  public boolean isCollisionShapeEmpty() {
    return SFBlockHelpers.COLLISION_SHAPE_EMPTY.get(mcState);
  }

  @Override
  public boolean isCollisionShapeFullBlock() {
    return SFBlockHelpers.COLLISION_SHAPE_FULL_BLOCK.get(mcState);
  }

  @Override
  public boolean isTopFaceFull() {
    return SFBlockHelpers.COLLISION_SHAPE_TOP_FACE_FULL.get(mcState);
  }

  @Override
  public boolean isEmpty() {
    return SFBlockHelpers.isEmptyBlock(mcState.getBlock());
  }

  @Override
  public boolean isVoidAir() {
    return mcState.getBlock() == Blocks.VOID_AIR;
  }

  @Override
  public boolean isFluid() {
    return mcState.getFluidState().getType() != Fluids.EMPTY;
  }

  @Override
  public boolean isFallingBlock() {
    return mcState.getBlock() instanceof FallingBlock;
  }

  @Override
  public boolean isHurtOnTouch() {
    return SFBlockHelpers.isHurtOnTouchSide(mcState);
  }

  @Override
  public boolean isHurtOnStand() {
    return SFBlockHelpers.isHurtWhenStoodOn(mcState);
  }

  @Override
  public boolean affectsMovementSpeed() {
    return SFBlockHelpers.affectsTouchMovementSpeed(mcState.getBlock());
  }

  @Override
  @SuppressWarnings("deprecation")
  public boolean blocksMotion() {
    return mcState.blocksMotion();
  }

  @Override
  public boolean canBeReplaced() {
    return mcState.canBeReplaced();
  }

  @Override
  public boolean isStairs() {
    return mcState.is(BlockTags.STAIRS);
  }

  /// Record for wrapping Minecraft Block as BlockType.
  private record MinecraftBlockType(Block block) implements BlockType {
    @Override
    public int id() {
      return BuiltInRegistries.BLOCK.getId(block);
    }

    @Override
    public float defaultDestroyTime() {
      return block.defaultDestroyTime();
    }

    @Override
    public boolean hasBlockItem() {
      return com.soulfiremc.server.util.BlockItems.hasItem(block);
    }
  }
}
