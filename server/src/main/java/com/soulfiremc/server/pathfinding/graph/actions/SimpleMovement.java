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
import com.soulfiremc.server.pathfinding.graph.actions.movement.MovementModifier;
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
public final class SimpleMovement extends GraphAction implements Cloneable {
  private static final SFVec3i FEET_POSITION_RELATIVE_BLOCK = SFVec3i.ZERO;
  private final MovementDirection direction;
  private final MovementSide side;
  private final MovementModifier modifier;
  private final SFVec3i targetFeetBlock;
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
  private double cost;
  @Getter
  private boolean appliedCornerCost = false;
  @Setter
  private boolean requiresAgainstBlock = false;

  public SimpleMovement(MovementDirection direction, MovementSide side, MovementModifier modifier) {
    this.direction = direction;
    this.side = side;
    this.modifier = modifier;
    this.diagonal = direction.isDiagonal();

    this.cost =
      (diagonal ? Costs.DIAGONAL : Costs.STRAIGHT)
        // Add modifier costs
        // Jump up block gets a tiny bit extra (you can move midair)
        // But that's fine since we also want to slightly discourage jumping up
        + switch (modifier) {
        case NORMAL -> 0;
        case FALL_1 -> Costs.FALL_1;
        case FALL_2 -> Costs.FALL_2;
        case FALL_3 -> Costs.FALL_3;
        case JUMP_UP_BLOCK -> Costs.JUMP_UP_BLOCK;
      };

    this.targetFeetBlock = modifier.offset(direction.offset(FEET_POSITION_RELATIVE_BLOCK));
    this.allowBlockActions =
      !diagonal && switch (modifier) {
        case JUMP_UP_BLOCK, NORMAL, FALL_1 -> true;
        default -> false;
      };

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

    if (modifier == MovementModifier.JUMP_UP_BLOCK) {
      // Make block above the head block free for jump
      requiredFreeBlocks.add(Pair.of(FEET_POSITION_RELATIVE_BLOCK.add(0, 2, 0), BlockFace.BOTTOM));
    }

    // Add the blocks that are required to be free for diagonal movement
    if (diagonal) {
      var corner = getCorner(side);

      for (var bodyOffset : BodyPart.VALUES) {
        // Apply jump shift to target edge and offset for body part
        requiredFreeBlocks.add(
          Pair.of(bodyOffset.offset(modifier.offsetIfJump(corner)), getCornerDirection(side).opposite().toBlockFace()));
      }
    }

    var targetEdge = direction.offset(FEET_POSITION_RELATIVE_BLOCK);
    for (var bodyOffset : BodyPart.VALUES) {
      BlockFace blockBreakSideHint;
      if (diagonal) {
        blockBreakSideHint = getCornerDirection(side.opposite()).opposite().toBlockFace();
      } else {
        blockBreakSideHint = switch (direction) {
          case NORTH -> BlockFace.NORTH;
          case SOUTH -> BlockFace.SOUTH;
          case EAST -> BlockFace.EAST;
          case WEST -> BlockFace.WEST;
          default -> throw new IllegalStateException("Unexpected value: " + direction);
        };
      }

      // Apply jump shift to target diagonal and offset for body part
      requiredFreeBlocks.add(Pair.of(bodyOffset.offset(modifier.offsetIfJump(targetEdge)), blockBreakSideHint));
    }

    // Require free blocks to fall into the target position
    switch (modifier) {
      case FALL_1 -> requiredFreeBlocks.add(Pair.of(MovementModifier.FALL_1.offset(targetEdge), BlockFace.TOP));
      case FALL_2 -> {
        requiredFreeBlocks.add(Pair.of(MovementModifier.FALL_1.offset(targetEdge), BlockFace.TOP));
        requiredFreeBlocks.add(Pair.of(MovementModifier.FALL_2.offset(targetEdge), BlockFace.TOP));
      }
      case FALL_3 -> {
        requiredFreeBlocks.add(Pair.of(MovementModifier.FALL_1.offset(targetEdge), BlockFace.TOP));
        requiredFreeBlocks.add(Pair.of(MovementModifier.FALL_2.offset(targetEdge), BlockFace.TOP));
        requiredFreeBlocks.add(Pair.of(MovementModifier.FALL_3.offset(targetEdge), BlockFace.TOP));
      }
    }

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
      list.add(bodyOffset.offset(modifier.offsetIfJump(corner)));
    }

    return list;
  }

  public SFVec3i requiredSolidBlock() {
    // Floor block
    return targetFeetBlock.sub(0, 1, 0);
  }

  public BlockSafetyData[][] listCheckSafeMineBlocks() {
    if (!allowBlockActions) {
      return new BlockSafetyData[0][];
    }

    var results = new BlockSafetyData[requiredFreeBlocks.size()][];

    var blockDirection =
      switch (direction) {
        case NORTH -> SkyDirection.NORTH;
        case SOUTH -> SkyDirection.SOUTH;
        case EAST -> SkyDirection.EAST;
        case WEST -> SkyDirection.WEST;
        default -> throw new IllegalStateException("Unexpected value: " + direction);
      };

    var oppositeDirection = blockDirection.opposite();
    var leftDirectionSide = blockDirection.leftSide();
    var rightDirectionSide = blockDirection.rightSide();

    if (modifier == MovementModifier.JUMP_UP_BLOCK) {
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
    }

    var targetEdge = direction.offset(FEET_POSITION_RELATIVE_BLOCK);
    for (var bodyOffset : BodyPart.VALUES) {
      // Apply jump shift to target diagonal and offset for body part
      var block = bodyOffset.offset(modifier.offsetIfJump(targetEdge));
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
    if (modifier == MovementModifier.FALL_1) {
      var fallFree = MovementModifier.FALL_1.offset(targetEdge);
      results[freeBlockIndex(fallFree)] =
        new BlockSafetyData[] {
          new BlockSafetyData(direction.offset(fallFree), BlockSafetyData.BlockSafetyType.FLUIDS),
          new BlockSafetyData(
            leftDirectionSide.offset(fallFree), BlockSafetyData.BlockSafetyType.FLUIDS),
          new BlockSafetyData(
            rightDirectionSide.offset(fallFree), BlockSafetyData.BlockSafetyType.FLUIDS)
        };
    }

    return results;
  }

  public List<BotActionManager.BlockPlaceAgainstData> possibleBlocksToPlaceAgainst() {
    if (!allowBlockActions) {
      return List.of();
    }

    var blockDirection =
      switch (direction) {
        case NORTH -> SkyDirection.NORTH;
        case SOUTH -> SkyDirection.SOUTH;
        case EAST -> SkyDirection.EAST;
        case WEST -> SkyDirection.WEST;
        default -> throw new IllegalStateException("Unexpected value: " + direction);
      };

    var oppositeDirection = blockDirection.opposite();
    var leftDirectionSide = blockDirection.leftSide();
    var rightDirectionSide = blockDirection.rightSide();

    var floorBlock = targetFeetBlock.sub(0, 1, 0);
    return switch (modifier) {
      case NORMAL -> // 5
        List.of(
          // Below
          new BotActionManager.BlockPlaceAgainstData(floorBlock.sub(0, 1, 0), BlockFace.TOP),
          // In front
          new BotActionManager.BlockPlaceAgainstData(
            blockDirection.offset(floorBlock), oppositeDirection.toBlockFace()),
          // Scaffolding
          new BotActionManager.BlockPlaceAgainstData(
            oppositeDirection.offset(floorBlock), blockDirection.toBlockFace()),
          // Left side
          new BotActionManager.BlockPlaceAgainstData(
            leftDirectionSide.offset(floorBlock), rightDirectionSide.toBlockFace()),
          // Right side
          new BotActionManager.BlockPlaceAgainstData(
            rightDirectionSide.offset(floorBlock), leftDirectionSide.toBlockFace()));
      case JUMP_UP_BLOCK, FALL_1 -> // 4 - no scaffolding
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
      default -> throw new IllegalStateException("Unexpected value: " + modifier);
    };
  }

  public void addCornerCost() {
    cost += Costs.CORNER_SLIDE;
    appliedCornerCost = true;
  }

  @Override
  public boolean impossibleToComplete() {
    return requiresAgainstBlock && blockPlaceAgainstData == null;
  }

  @Override
  public GraphInstructions getInstructions(SFVec3i node) {
    var cost = this.cost;

    var blocksToBreak = blockBreakCosts == null ? 0 : blockBreakCosts.length;
    var blockToPlace = requiresAgainstBlock ? 1 : 0;

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

    var absoluteTargetFeetBlock = node.add(targetFeetBlock);

    if (requiresAgainstBlock) {
      var floorBlock = absoluteTargetFeetBlock.sub(0, 1, 0);
      cost += Costs.PLACE_BLOCK;
      actions.add(new BlockPlaceAction(floorBlock, blockPlaceAgainstData));
    }

    actions.add(new MovementAction(absoluteTargetFeetBlock, diagonal));

    return new GraphInstructions(
      absoluteTargetFeetBlock, cost, actions);
  }

  @Override
  public SimpleMovement copy() {
    return this.clone();
  }

  @Override
  public SimpleMovement clone() {
    try {
      var c = (SimpleMovement) super.clone();

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
