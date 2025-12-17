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
package com.soulfiremc.server.pathfinding.graph.constraint;

import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.pathfinding.graph.CollisionData;
import com.soulfiremc.server.pathfinding.graph.DiagonalCollisionCalculator;
import com.soulfiremc.server.pathfinding.graph.GraphInstructions;
import com.soulfiremc.server.pathfinding.minecraft.MinecraftBlockState;
import com.soulfiremc.server.pathfinding.world.BlockState;
import net.minecraft.world.item.ItemStack;

/// Minecraft-specific path constraint interface that extends the abstract PathConstraint.
/// This interface adds methods that work with Minecraft types directly.
@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public interface PathConstraint extends com.soulfiremc.server.pathfinding.graph.PathConstraint {
  boolean isPlaceable(ItemStack item);

  boolean isTool(ItemStack item);

  /// Minecraft-specific version of isOutOfLevel.
  boolean isOutOfLevel(net.minecraft.world.level.block.state.BlockState blockState, SFVec3i pos);

  /// Minecraft-specific version of canBreakBlock.
  boolean canBreakBlock(SFVec3i pos, net.minecraft.world.level.block.state.BlockState blockState);

  /// Minecraft-specific version of collidesWithAtEdge.
  boolean collidesWithAtEdge(DiagonalCollisionCalculator.CollisionData collisionData);

  // Default implementations for abstract interface methods that delegate to Minecraft-specific ones

  @Override
  default boolean isOutOfLevel(BlockState blockState, SFVec3i pos) {
    if (blockState instanceof MinecraftBlockState mcState) {
      return isOutOfLevel(mcState.unwrap(), pos);
    }
    throw new IllegalArgumentException("Expected MinecraftBlockState but got: " + blockState.getClass());
  }

  @Override
  default boolean canBreakBlock(SFVec3i pos, BlockState blockState) {
    if (blockState instanceof MinecraftBlockState mcState) {
      return canBreakBlock(pos, mcState.unwrap());
    }
    throw new IllegalArgumentException("Expected MinecraftBlockState but got: " + blockState.getClass());
  }

  @Override
  default boolean collidesWithAtEdge(CollisionData collisionData) {
    // For the abstract CollisionData, we need to convert it to Minecraft's CollisionData
    // This requires the underlying BlockState to be a MinecraftBlockState
    if (collisionData.blockState() instanceof MinecraftBlockState mcState) {
      return collidesWithAtEdge(new DiagonalCollisionCalculator.CollisionData(
        mcState.unwrap(),
        collisionData.diagonalArrayIndex(),
        collisionData.bodyPart(),
        collisionData.side()
      ));
    }
    throw new IllegalArgumentException("Expected MinecraftBlockState but got: " + collisionData.blockState().getClass());
  }
}
