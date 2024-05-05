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
import com.soulfiremc.server.pathfinding.execution.BlockPlaceAction;
import com.soulfiremc.server.pathfinding.execution.MovementAction;
import com.soulfiremc.server.pathfinding.execution.WorldAction;
import com.soulfiremc.server.pathfinding.graph.BlockFace;
import com.soulfiremc.server.pathfinding.graph.GraphInstructions;
import com.soulfiremc.server.pathfinding.graph.actions.movement.BlockSafetyData;
import com.soulfiremc.server.pathfinding.graph.actions.movement.BodyPart;
import com.soulfiremc.server.pathfinding.graph.actions.movement.MovementDirection;
import com.soulfiremc.server.pathfinding.graph.actions.movement.MovementMiningCost;
import com.soulfiremc.server.pathfinding.graph.actions.movement.MovementSide;
import com.soulfiremc.server.pathfinding.graph.actions.movement.SkyDirection;
import com.soulfiremc.server.protocol.bot.BotActionManager;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class FallMovement extends GraphAction implements Cloneable {
  private static final SFVec3i FEET_POSITION_RELATIVE_BLOCK = SFVec3i.ZERO;
  private final MovementDirection direction;
  private final MovementSide side;
  @Getter
  private final boolean diagonal;
  @Getter
  private final boolean allowBlockActions;
  @Getter
  private final List<Pair<SFVec3i, BlockFace>> requiredFreeBlocks;
  @Getter
  private MovementMiningCost[] blockBreakCosts;
  @Getter
  private boolean[] unsafeToBreak;
  @Getter
  private boolean[] noNeedToBreak;
  @Setter
  @Getter
  private BotActionManager.BlockPlaceAgainstData blockPlaceAgainstData;
  private double baseCost;
  @Getter
  private boolean appliedCornerCost = false;
  @Getter
  @Setter
  private int closestBlockToFallOn = Integer.MIN_VALUE;

  public FallMovement(MovementDirection direction, MovementSide side) {
    this.direction = direction;
    this.side = side;
    this.diagonal = direction.isDiagonal();

    this.baseCost = diagonal ? Costs.DIAGONAL : Costs.STRAIGHT;

    this.allowBlockActions = !diagonal;

    this.requiredFreeBlocks = listRequiredFreeBlocks();
    if (allowBlockActions) {
      blockBreakCosts = new MovementMiningCost[requiredFreeBlocks.size()];
      unsafeToBreak = new boolean[requiredFreeBlocks.size()];
      noNeedToBreak = new boolean[requiredFreeBlocks.size()];
    } else {
      blockBreakCosts = null;
      unsafeToBreak = null;
      noNeedToBreak = null;
    }
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

    // Add the blocks that are required to be free for diagonal movement
    if (diagonal) {
      var corner = getCorner(side);

      for (var bodyOffset : BodyPart.VALUES) {
        // Apply jump shift to target edge and offset for body part
        requiredFreeBlocks.add(
          Pair.of(bodyOffset.offset(corner), getCornerDirection(side).opposite().toBlockFace()));
      }
    }

    var targetEdge = direction.offset(FEET_POSITION_RELATIVE_BLOCK);
    for (var bodyOffset : BodyPart.VALUES) {
      BlockFace blockBreakSideHint;
      if (diagonal) {
        blockBreakSideHint = getCornerDirection(side.opposite()).opposite().toBlockFace();
      } else {
        blockBreakSideHint = direction.toBlockFace();
      }

      // Apply jump shift to target diagonal and offset for body part
      requiredFreeBlocks.add(Pair.of(bodyOffset.offset(targetEdge), blockBreakSideHint));
    }

    // Require free blocks to fall into the target position
    requiredFreeBlocks.add(Pair.of(targetEdge.sub(0, 1, 0), BlockFace.TOP));

    return requiredFreeBlocks;
  }

  private SFVec3i getCorner(MovementSide side) {
    return getCornerDirection(side).offset(FEET_POSITION_RELATIVE_BLOCK);
  }

  private SkyDirection getCornerDirection(MovementSide side) {
    return (switch (direction) {
      case NORTH_EAST -> switch (side) {
        case LEFT -> SkyDirection.NORTH;
        case RIGHT -> SkyDirection.EAST;
      };
      case NORTH_WEST -> switch (side) {
        case LEFT -> SkyDirection.NORTH;
        case RIGHT -> SkyDirection.WEST;
      };
      case SOUTH_EAST -> switch (side) {
        case LEFT -> SkyDirection.SOUTH;
        case RIGHT -> SkyDirection.EAST;
      };
      case SOUTH_WEST -> switch (side) {
        case LEFT -> SkyDirection.SOUTH;
        case RIGHT -> SkyDirection.WEST;
      };
      default -> throw new IllegalStateException("Unexpected value: " + direction);
    });
  }

  public List<SFVec3i> listAddCostIfSolidBlocks() {
    if (!diagonal) {
      return List.of();
    }

    var list = new ObjectArrayList<SFVec3i>(2);

    // If these blocks are solid, the bot moves slower because the bot is running around a corner
    var corner = getCorner(side.opposite());
    for (var bodyOffset : BodyPart.VALUES) {
      // Apply jump shift to target edge and offset for body part
      list.add(bodyOffset.offset(corner));
    }

    return list;
  }

  // These blocks are possibly safe blocks we can fall on top of
  public List<SFVec3i> listSafetyCheckBlocks() {
    var targetEdge = direction.offset(FEET_POSITION_RELATIVE_BLOCK);
    return List.of(
      targetEdge.sub(0, 2, 0),
      targetEdge.sub(0, 3, 0),
      targetEdge.sub(0, 4, 0));
  }

  public BlockSafetyData[][] listCheckSafeMineBlocks() {
    if (!allowBlockActions) {
      return new BlockSafetyData[0][];
    }

    var results = new BlockSafetyData[requiredFreeBlocks.size()][];

    var blockDirection = direction.toSkyDirection();
    var leftDirectionSide = blockDirection.leftSide();
    var rightDirectionSide = blockDirection.rightSide();

    var targetEdge = direction.offset(FEET_POSITION_RELATIVE_BLOCK);
    for (var bodyOffset : BodyPart.VALUES) {
      // Apply jump shift to target diagonal and offset for body part
      var block = bodyOffset.offset(targetEdge);
      var index = freeBlockIndex(block);

      if (bodyOffset == BodyPart.HEAD) {
        results[index] =
          new BlockSafetyData[] {
            new BlockSafetyData(
              block.add(0, 1, 0), BlockSafetyData.BlockSafetyType.FALLING_AND_FLUIDS),
            new BlockSafetyData(direction.offset(block), BlockSafetyData.BlockSafetyType.FLUIDS),
            new BlockSafetyData(
              leftDirectionSide.offset(block), BlockSafetyData.BlockSafetyType.FLUIDS),
            new BlockSafetyData(
              rightDirectionSide.offset(block), BlockSafetyData.BlockSafetyType.FLUIDS)
          };
      } else {
        results[index] =
          new BlockSafetyData[] {
            new BlockSafetyData(direction.offset(block), BlockSafetyData.BlockSafetyType.FLUIDS),
            new BlockSafetyData(
              leftDirectionSide.offset(block), BlockSafetyData.BlockSafetyType.FLUIDS),
            new BlockSafetyData(
              rightDirectionSide.offset(block), BlockSafetyData.BlockSafetyType.FLUIDS)
          };
      }
    }

    // Require free blocks to fall into the target position
    var fallFree = targetEdge.sub(0, 1, 0);
    results[freeBlockIndex(fallFree)] =
      new BlockSafetyData[] {
        new BlockSafetyData(direction.offset(fallFree), BlockSafetyData.BlockSafetyType.FLUIDS),
        new BlockSafetyData(
          leftDirectionSide.offset(fallFree), BlockSafetyData.BlockSafetyType.FLUIDS),
        new BlockSafetyData(
          rightDirectionSide.offset(fallFree), BlockSafetyData.BlockSafetyType.FLUIDS)
      };

    return results;
  }

  public List<BotActionManager.BlockPlaceAgainstData> possibleBlocksToPlaceAgainst() {
    if (!allowBlockActions) {
      return List.of();
    }

    var blockDirection = direction.toSkyDirection();

    var oppositeDirection = blockDirection.opposite();
    var leftDirectionSide = blockDirection.leftSide();
    var rightDirectionSide = blockDirection.rightSide();

    var targetEdge = direction.offset(FEET_POSITION_RELATIVE_BLOCK);
    var floorBlock = targetEdge.sub(0, 1, 0);
    return // 4 - no scaffolding
      List.of(
        // Below
        new BotActionManager.BlockPlaceAgainstData(floorBlock.sub(0, 1, 0), BlockFace.TOP),
        // In front
        new BotActionManager.BlockPlaceAgainstData(
          blockDirection.offset(floorBlock), oppositeDirection.toBlockFace()),
        // Left side
        new BotActionManager.BlockPlaceAgainstData(
          leftDirectionSide.offset(floorBlock), rightDirectionSide.toBlockFace()),
        // Right side
        new BotActionManager.BlockPlaceAgainstData(
          rightDirectionSide.offset(floorBlock), leftDirectionSide.toBlockFace()));
  }

  public void addCornerCost() {
    baseCost += Costs.CORNER_SLIDE;
    appliedCornerCost = true;
  }

  @Override
  public boolean impossibleToComplete() {
    return closestBlockToFallOn == Integer.MIN_VALUE && blockPlaceAgainstData == null;
  }

  @Override
  public GraphInstructions getInstructions(SFVec3i node) {
    var cost = this.baseCost;

    var needsToPlaceBlock = closestBlockToFallOn == Integer.MIN_VALUE;
    if (needsToPlaceBlock) {
      // We place a block and fall one down to land on that block
      cost += Costs.FALL_1;
    } else {
      // We fall on a block that is already there
      switch (closestBlockToFallOn) {
        case -2 -> cost += Costs.FALL_1;
        case -3 -> cost += Costs.FALL_2;
        case -4 -> cost += Costs.FALL_3;
        default -> throw new IllegalStateException("Unexpected value: " + closestBlockToFallOn);
      }
    }

    var blocksToBreak = blockBreakCosts == null ? 0 : blockBreakCosts.length;
    var blockToPlace = needsToPlaceBlock ? 1 : 0;

    var actions = new ObjectArrayList<WorldAction>(1 + blocksToBreak + blockToPlace);
    if (blockBreakCosts != null) {
      for (var breakCost : blockBreakCosts) {
        if (breakCost == null) {
          continue;
        }

        cost += breakCost.miningCost();
        actions.add(new BlockBreakAction(breakCost));
      }
    }

    var absoluteTargetFeetBlock = node.add(direction
      .offset(FEET_POSITION_RELATIVE_BLOCK)
      // -1 means we place a block to be able to fall one down
      .add(0, closestBlockToFallOn == Integer.MIN_VALUE ? -1 : closestBlockToFallOn + 1, 0));

    if (needsToPlaceBlock) {
      var floorBlock = absoluteTargetFeetBlock.sub(0, 1, 0);
      cost += Costs.PLACE_BLOCK;
      actions.add(new BlockPlaceAction(floorBlock, blockPlaceAgainstData));
    }

    actions.add(new MovementAction(absoluteTargetFeetBlock, diagonal));

    return new GraphInstructions(
      absoluteTargetFeetBlock, cost, actions);
  }

  @Override
  public FallMovement copy() {
    return this.clone();
  }

  @Override
  public FallMovement clone() {
    try {
      var c = (FallMovement) super.clone();

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
