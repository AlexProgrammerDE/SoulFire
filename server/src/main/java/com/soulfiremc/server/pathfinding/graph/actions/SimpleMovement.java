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

import com.soulfiremc.server.data.BlockState;
import com.soulfiremc.server.pathfinding.Costs;
import com.soulfiremc.server.pathfinding.NodeState;
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.pathfinding.execution.BlockBreakAction;
import com.soulfiremc.server.pathfinding.execution.BlockPlaceAction;
import com.soulfiremc.server.pathfinding.execution.MovementAction;
import com.soulfiremc.server.pathfinding.execution.WorldAction;
import com.soulfiremc.server.pathfinding.graph.BlockFace;
import com.soulfiremc.server.pathfinding.graph.GraphInstructions;
import com.soulfiremc.server.pathfinding.graph.MinecraftGraph;
import com.soulfiremc.server.pathfinding.graph.actions.movement.BlockSafetyData;
import com.soulfiremc.server.pathfinding.graph.actions.movement.BodyPart;
import com.soulfiremc.server.pathfinding.graph.actions.movement.MovementDirection;
import com.soulfiremc.server.pathfinding.graph.actions.movement.MovementMiningCost;
import com.soulfiremc.server.pathfinding.graph.actions.movement.MovementModifier;
import com.soulfiremc.server.pathfinding.graph.actions.movement.MovementSide;
import com.soulfiremc.server.protocol.bot.BotActionManager;
import com.soulfiremc.server.util.BlockTypeHelper;
import com.soulfiremc.server.util.LazyBoolean;
import it.unimi.dsi.fastutil.Pair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class SimpleMovement extends GraphAction implements Cloneable {
  private static final SFVec3i FEET_POSITION_RELATIVE_BLOCK = SFVec3i.ZERO;
  private final MovementDirection direction;
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
  private MovementSide blockedSide;
  @Setter
  @Getter
  private BotActionManager.BlockPlaceAgainstData blockPlaceAgainstData;
  @Getter
  @Setter
  private double cost;
  @Setter
  private boolean requiresAgainstBlock = false;

  public SimpleMovement(MovementDirection direction, MovementModifier modifier) {
    this.direction = direction;
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

  public static void registerMovements(
    Consumer<GraphAction> callback,
    BiConsumer<SFVec3i, MinecraftGraph.MovementSubscription<?>> blockSubscribers) {
    for (var direction : MovementDirection.VALUES) {
      for (var modifier : MovementModifier.VALUES) {
        callback.accept(
          SimpleMovement.registerMovement(
            blockSubscribers,
            new SimpleMovement(direction, modifier)));
      }
    }
  }

  private static SimpleMovement registerMovement(
    BiConsumer<SFVec3i, MinecraftGraph.MovementSubscription<?>> blockSubscribers,
    SimpleMovement movement) {
    {
      var blockId = 0;
      for (var freeBlock : movement.requiredFreeBlocks()) {
        blockSubscribers
          .accept(freeBlock.key(), new SimpleMovementBlockSubscription(SimpleMovementBlockSubscription.SubscriptionType.MOVEMENT_FREE, blockId++, freeBlock.value()));
      }
    }

    {
      var safeBlocks = movement.listCheckSafeMineBlocks();
      for (var i = 0; i < safeBlocks.length; i++) {
        var savedBlock = safeBlocks[i];
        if (savedBlock == null) {
          continue;
        }

        for (var block : savedBlock) {
          blockSubscribers
            .accept(block.position(), new SimpleMovementBlockSubscription(
              SimpleMovementBlockSubscription.SubscriptionType.MOVEMENT_BREAK_SAFETY_CHECK,
              i,
              block.type()));
        }
      }
    }

    {
      blockSubscribers
        .accept(movement.requiredSolidBlock(), new SimpleMovementBlockSubscription(SimpleMovementBlockSubscription.SubscriptionType.MOVEMENT_SOLID));
    }

    {
      for (var diagonalCollisionBlock : movement.listDiagonalCollisionBlocks()) {
        blockSubscribers
          .accept(diagonalCollisionBlock.key(), new SimpleMovementBlockSubscription(
            SimpleMovementBlockSubscription.SubscriptionType.MOVEMENT_DIAGONAL_COLLISION, diagonalCollisionBlock.value()));
      }
    }

    {
      for (var againstBlock : movement.possibleBlocksToPlaceAgainst()) {
        blockSubscribers
          .accept(againstBlock.againstPos(), new SimpleMovementBlockSubscription(
            SimpleMovementBlockSubscription.SubscriptionType.MOVEMENT_AGAINST_PLACE_SOLID, againstBlock));
      }
    }

    return movement;
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
    var requiredFreeBlocks = new ArrayList<Pair<SFVec3i, BlockFace>>();

    if (modifier == MovementModifier.JUMP_UP_BLOCK) {
      // Make block above the head block free for jump
      requiredFreeBlocks.add(Pair.of(FEET_POSITION_RELATIVE_BLOCK.add(0, 2, 0), BlockFace.BOTTOM));
    }

    var targetEdge = direction.offset(FEET_POSITION_RELATIVE_BLOCK);
    for (var bodyOffset : BodyPart.VALUES) {
      BlockFace blockBreakSideHint;
      if (diagonal) {
        blockBreakSideHint = null; // We don't mine blocks in diagonals
      } else {
        blockBreakSideHint = direction.toBlockFace();
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

  public List<Pair<SFVec3i, MovementSide>> listDiagonalCollisionBlocks() {
    if (!diagonal) {
      return List.of();
    }

    var list = new ArrayList<Pair<SFVec3i, MovementSide>>(4);

    for (var side : MovementSide.VALUES) {
      // If these blocks are solid, the bot moves slower because the bot is running around a corner
      var corner = direction.side(side).offset(FEET_POSITION_RELATIVE_BLOCK);
      for (var bodyOffset : BodyPart.VALUES) {
        // Apply jump shift to target edge and offset for body part
        list.add(Pair.of(bodyOffset.offset(modifier.offsetIfJump(corner)), side));
      }
    }

    return list;
  }

  public SFVec3i requiredSolidBlock() {
    // Floor block
    return targetFeetBlock.sub(0, 1, 0);
  }

  public BlockSafetyData[][] listCheckSafeMineBlocks() {
    // This also excludes diagonal movement, so we only worry about digging straight
    if (!allowBlockActions) {
      return new BlockSafetyData[0][];
    }

    var results = new BlockSafetyData[requiredFreeBlocks.size()][];

    var blockDirection = direction.toSkyDirection();

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

    var blockDirection = direction.toSkyDirection();

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

  @Override
  public List<GraphInstructions> getInstructions(MinecraftGraph graph, NodeState node) {
    if (requiresAgainstBlock && blockPlaceAgainstData == null) {
      return Collections.emptyList();
    }

    var cost = this.cost;

    var blocksToBreak = blockBreakCosts == null ? 0 : blockBreakCosts.length;
    var blockToPlace = requiresAgainstBlock ? 1 : 0;

    var usableBlockItemsDiff = 0;
    var actions = new ArrayList<WorldAction>(1 + blocksToBreak + blockToPlace);
    if (blockBreakCosts != null) {
      for (var breakCost : blockBreakCosts) {
        if (breakCost == null) {
          continue;
        }

        cost += breakCost.miningCost();
        actions.add(new BlockBreakAction(breakCost));

        if (breakCost.willDropUsableBlockItem()) {
          usableBlockItemsDiff++;
        }
      }
    }

    var absoluteTargetFeetBlock = node.blockPosition().add(targetFeetBlock);
    var afterBreakUsableBlockItems = node.usableBlockItems() + usableBlockItemsDiff;

    if (requiresAgainstBlock) {
      if (afterBreakUsableBlockItems < 1) {
        return Collections.emptyList();
      } else if (graph.doUsableBlocksDecreaseWhenPlaced()) {
        // After the place we'll have one less usable block item
        afterBreakUsableBlockItems--;
        cost += Costs.PLACE_BLOCK;
      }

      var floorBlock = absoluteTargetFeetBlock.sub(0, 1, 0);
      actions.add(new BlockPlaceAction(floorBlock, blockPlaceAgainstData));
    }

    actions.add(new MovementAction(absoluteTargetFeetBlock, diagonal));

    return Collections.singletonList(new GraphInstructions(
      new NodeState(absoluteTargetFeetBlock, afterBreakUsableBlockItems), cost, actions));
  }

  @Override
  public SimpleMovement copy() {
    return this.clone();
  }

  @Override
  public SimpleMovement clone() {
    try {
      var c = (SimpleMovement) super.clone();

      c.blockBreakCosts = this.blockBreakCosts == null ? null : new MovementMiningCost[this.blockBreakCosts.length];
      c.unsafeToBreak = this.unsafeToBreak == null ? null : new boolean[this.unsafeToBreak.length];
      c.noNeedToBreak = this.noNeedToBreak == null ? null : new boolean[this.noNeedToBreak.length];

      return c;
    } catch (CloneNotSupportedException cantHappen) {
      throw new InternalError();
    }
  }

  record SimpleMovementBlockSubscription(
    SubscriptionType type,
    int blockArrayIndex,
    BlockFace blockBreakSideHint,
    BotActionManager.BlockPlaceAgainstData blockToPlaceAgainst,
    BlockSafetyData.BlockSafetyType safetyType,
    MovementSide side) implements MinecraftGraph.MovementSubscription<SimpleMovement> {
    SimpleMovementBlockSubscription(SubscriptionType type) {
      this(type, -1, null, null, null, null);
    }

    SimpleMovementBlockSubscription(SubscriptionType type, MovementSide side) {
      this(type, -1, null, null, null, side);
    }

    SimpleMovementBlockSubscription(SubscriptionType type, int blockArrayIndex, BlockFace blockBreakSideHint) {
      this(type, blockArrayIndex, blockBreakSideHint, null, null, null);
    }

    SimpleMovementBlockSubscription(
      SubscriptionType type,
      BotActionManager.BlockPlaceAgainstData blockToPlaceAgainst) {
      this(type, -1, null, blockToPlaceAgainst, null, null);
    }

    SimpleMovementBlockSubscription(
      SubscriptionType subscriptionType,
      int i,
      BlockSafetyData.BlockSafetyType type) {
      this(subscriptionType, i, null, null, type, null);
    }

    @Override
    public MinecraftGraph.SubscriptionSingleResult processBlock(MinecraftGraph graph, SFVec3i key, SimpleMovement simpleMovement, LazyBoolean isFree,
                                                                BlockState blockState, SFVec3i absoluteKey) {
      return switch (type) {
        case MOVEMENT_FREE -> {
          if (isFree.get()) {
            if (simpleMovement.allowBlockActions()) {
              simpleMovement.noNeedToBreak()[blockArrayIndex] = true;
            }

            yield MinecraftGraph.SubscriptionSingleResult.CONTINUE;
          }

          // Search for a way to break this block
          if (graph.disallowedToBreakBlock(absoluteKey)
            || !simpleMovement.allowBlockActions()
            || graph.disallowedToBreakType(blockState.blockType())
            // Check if we previously found out this block is unsafe to break
            || simpleMovement.unsafeToBreak()[blockArrayIndex]) {
            // No way to break this block
            yield MinecraftGraph.SubscriptionSingleResult.IMPOSSIBLE;
          }

          var cacheableMiningCost = graph.inventory().getMiningCosts(graph.tagsState(), blockState);
          // We can mine this block, lets add costs and continue
          simpleMovement.blockBreakCosts()[blockArrayIndex] =
            new MovementMiningCost(
              absoluteKey,
              cacheableMiningCost.miningCost(),
              cacheableMiningCost.willDropUsableBlockItem(),
              blockBreakSideHint);
          yield MinecraftGraph.SubscriptionSingleResult.CONTINUE;
        }
        case MOVEMENT_BREAK_SAFETY_CHECK -> {
          // There is no need to break this block, so there is no need for safety checks
          if (simpleMovement.noNeedToBreak()[blockArrayIndex]) {
            yield MinecraftGraph.SubscriptionSingleResult.CONTINUE;
          }

          // The block was already marked as unsafe
          if (simpleMovement.unsafeToBreak()[blockArrayIndex]) {
            yield MinecraftGraph.SubscriptionSingleResult.CONTINUE;
          }

          var unsafe = safetyType.isUnsafeBlock(blockState);

          if (!unsafe) {
            // All good, we can continue
            yield MinecraftGraph.SubscriptionSingleResult.CONTINUE;
          }

          var currentValue = simpleMovement.blockBreakCosts()[blockArrayIndex];

          if (currentValue != null) {
            // We learned that this block needs to be broken, so we need to set it as impossible
            yield MinecraftGraph.SubscriptionSingleResult.IMPOSSIBLE;
          }

          // Store for a later time that this is unsafe,
          // so if we check this block,
          // we know it's unsafe
          simpleMovement.unsafeToBreak()[blockArrayIndex] = true;

          yield MinecraftGraph.SubscriptionSingleResult.CONTINUE;
        }
        case MOVEMENT_SOLID -> {
          // Block is safe to walk on, no need to check for more
          if (BlockTypeHelper.isSafeBlockToStandOn(blockState)) {
            yield MinecraftGraph.SubscriptionSingleResult.CONTINUE;
          }

          if (graph.disallowedToPlaceBlock(absoluteKey)
            || !simpleMovement.allowBlockActions()
            || !blockState.blockType().replaceable()) {
            yield MinecraftGraph.SubscriptionSingleResult.IMPOSSIBLE;
          }

          // We can place a block here, but we need to find a block to place against
          simpleMovement.requiresAgainstBlock(true);
          yield MinecraftGraph.SubscriptionSingleResult.CONTINUE;
        }
        case MOVEMENT_AGAINST_PLACE_SOLID -> {
          // We already found one, no need to check for more
          if (simpleMovement.blockPlaceAgainstData() != null) {
            yield MinecraftGraph.SubscriptionSingleResult.CONTINUE;
          }

          // This block should not be placed against
          if (!blockState.blockShapeGroup().isFullBlock()) {
            yield MinecraftGraph.SubscriptionSingleResult.CONTINUE;
          }

          // Fixup the position to be the block we are placing against instead of relative
          simpleMovement.blockPlaceAgainstData(
            new BotActionManager.BlockPlaceAgainstData(
              absoluteKey, blockToPlaceAgainst.blockFace()));
          yield MinecraftGraph.SubscriptionSingleResult.CONTINUE;
        }
        case MOVEMENT_DIAGONAL_COLLISION -> {
          if (BlockTypeHelper.isHurtOnTouchSide(blockState.blockType())) {
            // Since this is a corner, we can also avoid touching blocks that hurt us, e.g., cacti
            yield MinecraftGraph.SubscriptionSingleResult.IMPOSSIBLE;
          } else if (blockState.blockShapeGroup().isFullBlock()) {
            var blockedSide = simpleMovement.blockedSide();
            if (blockedSide == null) {
              simpleMovement.blockedSide(side);
              simpleMovement.cost(simpleMovement.cost() + Costs.CORNER_SLIDE);
              yield MinecraftGraph.SubscriptionSingleResult.CONTINUE;
            } else if (blockedSide == side) {
              yield MinecraftGraph.SubscriptionSingleResult.CONTINUE;
            } else {
              // Diagonal path is blocked on both sides
              yield MinecraftGraph.SubscriptionSingleResult.IMPOSSIBLE;
            }
          }

          yield MinecraftGraph.SubscriptionSingleResult.CONTINUE;
        }
      };
    }

    @Override
    public SimpleMovement castAction(GraphAction action) {
      return (SimpleMovement) action;
    }

    enum SubscriptionType {
      MOVEMENT_FREE,
      MOVEMENT_BREAK_SAFETY_CHECK,
      MOVEMENT_SOLID,
      MOVEMENT_DIAGONAL_COLLISION,
      MOVEMENT_AGAINST_PLACE_SOLID
    }
  }
}
