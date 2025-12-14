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

import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.pathfinding.graph.GraphInstructions;
import com.soulfiremc.server.pathfinding.graph.PathConstraint;
import com.soulfiremc.server.util.SFBlockHelpers;
import com.soulfiremc.server.util.SFItemHelpers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class JMHPathConstraint extends PathConstraint {
  public static final JMHPathConstraint INSTANCE = new JMHPathConstraint();

  private final int minY;
  private final int maxY;

  public JMHPathConstraint() {
    this(-64, 320);
  }

  public JMHPathConstraint(int minY, int maxY) {
    super(null, null);
    this.minY = minY;
    this.maxY = maxY;
  }

  @Override
  public boolean doUsableBlocksDecreaseWhenPlaced() {
    return true;
  }

  @Override
  public boolean isPlaceable(ItemStack item) {
    return SFItemHelpers.isSafeFullBlockItem(item);
  }

  @Override
  public boolean isTool(ItemStack item) {
    return SFItemHelpers.isTool(item);
  }

  @Override
  public boolean isOutOfLevel(BlockState blockState, SFVec3i pos) {
    return blockState.getBlock() == Blocks.VOID_AIR && pos.y >= minY && pos.y < maxY;
  }

  @Override
  public boolean canBreakBlockPos(SFVec3i pos) {
    return pos.y >= minY && pos.y < maxY;
  }

  @Override
  public boolean canPlaceBlockPos(SFVec3i pos) {
    return pos.y >= minY && pos.y < maxY;
  }

  @Override
  public boolean canBreakBlock(Block blockType) {
    return SFBlockHelpers.isDiggable(blockType);
  }

  @Override
  public GraphInstructions modifyAsNeeded(GraphInstructions instruction) {
    return instruction;
  }
}
