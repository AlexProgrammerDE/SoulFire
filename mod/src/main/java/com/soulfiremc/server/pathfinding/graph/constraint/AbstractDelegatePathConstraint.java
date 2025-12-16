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
import com.soulfiremc.server.pathfinding.graph.DiagonalCollisionCalculator;
import com.soulfiremc.server.pathfinding.graph.GraphInstructions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractDelegatePathConstraint implements PathConstraint {
  @Override
  public boolean doUsableBlocksDecreaseWhenPlaced() {
    return delegate().doUsableBlocksDecreaseWhenPlaced();
  }

  @Override
  public boolean canBlocksDropWhenBroken() {
    return delegate().canBlocksDropWhenBroken();
  }

  @Override
  public boolean canBreakBlocks() {
    return delegate().canBreakBlocks();
  }

  @Override
  public boolean canPlaceBlocks() {
    return delegate().canPlaceBlocks();
  }

  @Override
  public boolean isPlaceable(ItemStack item) {
    return delegate().isPlaceable(item);
  }

  @Override
  public boolean isTool(ItemStack item) {
    return delegate().isTool(item);
  }

  @Override
  public boolean isOutOfLevel(BlockState blockState, SFVec3i pos) {
    return delegate().isOutOfLevel(blockState, pos);
  }

  @Override
  public boolean canBreakBlock(SFVec3i pos, BlockState blockState) {
    return delegate().canBreakBlock(pos, blockState);
  }

  @Override
  public boolean canPlaceBlock(SFVec3i pos) {
    return delegate().canPlaceBlock(pos);
  }

  @Override
  public boolean collidesWithAtEdge(DiagonalCollisionCalculator.CollisionData collisionData) {
    return delegate().collidesWithAtEdge(collisionData);
  }

  @Override
  public GraphInstructions modifyAsNeeded(GraphInstructions instruction) {
    return delegate().modifyAsNeeded(instruction);
  }

  @Override
  public double breakBlockPenalty() {
    return delegate().breakBlockPenalty();
  }

  @Override
  public double placeBlockPenalty() {
    return delegate().placeBlockPenalty();
  }

  @Override
  public int expireTimeout() {
    return delegate().expireTimeout();
  }

  @Override
  public boolean disablePruning() {
    return delegate().disablePruning();
  }

  protected abstract PathConstraint delegate();
}
