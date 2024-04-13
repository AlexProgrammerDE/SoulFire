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
import com.soulfiremc.server.pathfinding.execution.JumpAndPlaceBelowAction;
import com.soulfiremc.server.pathfinding.execution.WorldAction;
import com.soulfiremc.server.pathfinding.graph.BlockFace;
import com.soulfiremc.server.pathfinding.graph.GraphInstructions;
import com.soulfiremc.server.pathfinding.graph.actions.movement.BlockSafetyData;
import com.soulfiremc.server.pathfinding.graph.actions.movement.MovementMiningCost;
import com.soulfiremc.server.pathfinding.graph.actions.movement.SkyDirection;
import com.soulfiremc.server.protocol.bot.BotActionManager;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class UpMovement extends GraphAction implements Cloneable {
  private static final SFVec3i FEET_POSITION_RELATIVE_BLOCK = SFVec3i.ZERO;
  private final SFVec3i targetFeetBlock;
  @Getter
  private final List<Pair<SFVec3i, BlockFace>> requiredFreeBlocks;
  @Getter
  private MovementMiningCost[] blockBreakCosts;
  @Getter
  private boolean[] unsafeToBreak;
  @Getter
  private boolean[] noNeedToBreak;

  public UpMovement() {
    this.targetFeetBlock = FEET_POSITION_RELATIVE_BLOCK.add(0, 1, 0);

    this.requiredFreeBlocks = listRequiredFreeBlocks();
    this.blockBreakCosts = new MovementMiningCost[requiredFreeBlocks.size()];
    this.unsafeToBreak = new boolean[requiredFreeBlocks.size()];
    this.noNeedToBreak = new boolean[requiredFreeBlocks.size()];
  }

  private int freeBlockIndex(SFVec3i block) {
    for (var i = 0; i < requiredFreeBlocks.size(); i++) {
      if (requiredFreeBlocks.get(i).left().equals(block)) {
        return i;
      }
    }

    throw new IllegalArgumentException("Block not found in required free blocks");
  }

  private List<Pair<SFVec3i, BlockFace>> listRequiredFreeBlocks() {
    var requiredFreeBlocks = new ObjectArrayList<Pair<SFVec3i, BlockFace>>();

    // The one above the head to jump
    requiredFreeBlocks.add(Pair.of(FEET_POSITION_RELATIVE_BLOCK.add(0, 2, 0), BlockFace.BOTTOM));

    return requiredFreeBlocks;
  }

  public BlockSafetyData[][] listCheckSafeMineBlocks() {
    var results = new BlockSafetyData[requiredFreeBlocks.size()][];

    var firstDirection = SkyDirection.NORTH;
    var oppositeDirection = firstDirection.opposite();
    var leftDirectionSide = firstDirection.leftSide();
    var rightDirectionSide = firstDirection.rightSide();

    var aboveHead = FEET_POSITION_RELATIVE_BLOCK.add(0, 2, 0);
    results[freeBlockIndex(aboveHead)] =
      new BlockSafetyData[] {
        new BlockSafetyData(
          aboveHead.add(0, 1, 0), BlockSafetyData.BlockSafetyType.FALLING_AND_FLUIDS),
        new BlockSafetyData(
          oppositeDirection.offset(aboveHead), BlockSafetyData.BlockSafetyType.FLUIDS),
        new BlockSafetyData(
          leftDirectionSide.offset(aboveHead), BlockSafetyData.BlockSafetyType.FLUIDS),
        new BlockSafetyData(
          rightDirectionSide.offset(aboveHead), BlockSafetyData.BlockSafetyType.FLUIDS)
      };

    return results;
  }

  @Override
  public GraphInstructions getInstructions(SFVec3i node) {
    var actions = new ObjectArrayList<WorldAction>();
    var cost = Costs.TOWER_COST;

    for (var breakCost : blockBreakCosts) {
      if (breakCost == null) {
        continue;
      }

      cost += breakCost.miningCost();
      actions.add(new BlockBreakAction(breakCost));
    }

    var absoluteTargetFeetBlock = node.add(targetFeetBlock);

    // Where we are standing right now, we'll place the target block below us after jumping
    actions.add(
      new JumpAndPlaceBelowAction(
        node,
        new BotActionManager.BlockPlaceAgainstData(
          node.sub(0, 1, 0), BlockFace.TOP)));

    return new GraphInstructions(
      absoluteTargetFeetBlock, cost, actions);
  }

  @Override
  public UpMovement copy() {
    return this.clone();
  }

  @Override
  public UpMovement clone() {
    try {
      var c = (UpMovement) super.clone();

      c.blockBreakCosts =
        this.blockBreakCosts == null ? null : new MovementMiningCost[this.blockBreakCosts.length];
      c.unsafeToBreak = this.unsafeToBreak == null ? null : new boolean[this.unsafeToBreak.length];
      c.noNeedToBreak = this.noNeedToBreak == null ? null : new boolean[this.noNeedToBreak.length];

      return c;
    } catch (CloneNotSupportedException cantHappen) {
      throw new InternalError();
    }
  }
}
