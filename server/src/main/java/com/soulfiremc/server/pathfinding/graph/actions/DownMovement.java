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
package com.soulfiremc.server.pathfinding.graph.actions;

import com.soulfiremc.server.pathfinding.Costs;
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.pathfinding.execution.BlockBreakAction;
import com.soulfiremc.server.pathfinding.graph.BlockFace;
import com.soulfiremc.server.pathfinding.graph.GraphInstructions;
import com.soulfiremc.server.pathfinding.graph.actions.movement.BlockSafetyData;
import com.soulfiremc.server.pathfinding.graph.actions.movement.MovementMiningCost;
import com.soulfiremc.server.pathfinding.graph.actions.movement.SkyDirection;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

public final class DownMovement extends GraphAction implements Cloneable {
  private static final SFVec3i FEET_POSITION_RELATIVE_BLOCK = SFVec3i.ZERO;
  private final SFVec3i targetToMineBlock;
  @Getter
  @Setter
  private MovementMiningCost breakCost;
  @Getter
  @Setter
  private int closestBlockToFallOn = Integer.MIN_VALUE;

  public DownMovement() {
    this.targetToMineBlock = FEET_POSITION_RELATIVE_BLOCK.sub(0, 1, 0);
  }

  public Pair<SFVec3i, BlockFace> blockToBreak() {
    return Pair.of(targetToMineBlock, BlockFace.TOP);
  }

  public List<SFVec3i> listSafetyCheckBlocks() {
    var requiredFreeBlocks = new ObjectArrayList<SFVec3i>();

    // Falls one block
    requiredFreeBlocks.add(FEET_POSITION_RELATIVE_BLOCK.sub(0, 2, 0));

    // Falls two blocks
    requiredFreeBlocks.add(FEET_POSITION_RELATIVE_BLOCK.sub(0, 3, 0));

    // Falls three blocks
    requiredFreeBlocks.add(FEET_POSITION_RELATIVE_BLOCK.sub(0, 4, 0));

    return requiredFreeBlocks;
  }

  public BlockSafetyData[][] listCheckSafeMineBlocks() {
    var results = new BlockSafetyData[1][];

    var firstDirection = SkyDirection.NORTH;
    var oppositeDirection = firstDirection.opposite();
    var leftDirectionSide = firstDirection.leftSide();
    var rightDirectionSide = firstDirection.rightSide();

    results[0] =
      new BlockSafetyData[] {
        new BlockSafetyData(
          firstDirection.offset(targetToMineBlock), BlockSafetyData.BlockSafetyType.FLUIDS),
        new BlockSafetyData(
          oppositeDirection.offset(targetToMineBlock), BlockSafetyData.BlockSafetyType.FLUIDS),
        new BlockSafetyData(
          leftDirectionSide.offset(targetToMineBlock), BlockSafetyData.BlockSafetyType.FLUIDS),
        new BlockSafetyData(
          rightDirectionSide.offset(targetToMineBlock), BlockSafetyData.BlockSafetyType.FLUIDS)
      };

    return results;
  }

  @Override
  public boolean impossibleToComplete() {
    return closestBlockToFallOn == Integer.MIN_VALUE;
  }

  @Override
  public GraphInstructions getInstructions(SFVec3i node) {
    var cost = 0D;

    cost +=
      switch (closestBlockToFallOn) {
        case -2 -> Costs.FALL_1;
        case -3 -> Costs.FALL_2;
        case -4 -> Costs.FALL_3;
        default -> throw new IllegalStateException("Unexpected value: " + closestBlockToFallOn);
      };

    cost += breakCost.miningCost();

    var absoluteTargetFeetBlock = node.add(0, closestBlockToFallOn + 1, 0);

    return new GraphInstructions(
      absoluteTargetFeetBlock,
      cost,
      List.of(new BlockBreakAction(breakCost)));
  }

  @Override
  public DownMovement copy() {
    return this.clone();
  }

  @Override
  public DownMovement clone() {
    try {
      return (DownMovement) super.clone();
    } catch (CloneNotSupportedException cantHappen) {
      throw new InternalError();
    }
  }
}
