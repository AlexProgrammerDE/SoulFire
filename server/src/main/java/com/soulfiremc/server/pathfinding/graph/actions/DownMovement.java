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
import com.soulfiremc.server.pathfinding.graph.BlockFace;
import com.soulfiremc.server.pathfinding.graph.GraphInstructions;
import com.soulfiremc.server.pathfinding.graph.MinecraftGraph;
import com.soulfiremc.server.pathfinding.graph.actions.movement.BlockSafetyData;
import com.soulfiremc.server.pathfinding.graph.actions.movement.MovementMiningCost;
import com.soulfiremc.server.pathfinding.graph.actions.movement.SkyDirection;
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

public final class DownMovement extends GraphAction implements Cloneable {
  private static final SFVec3i FEET_POSITION_RELATIVE_BLOCK = SFVec3i.ZERO;
  private final SFVec3i targetToMineBlock;
  @Getter
  @Setter
  private MovementMiningCost breakCost;
  @Getter
  @Setter
  private int closestBlockToFallOn = Integer.MIN_VALUE;
  @Getter
  @Setter
  private int closestObstructingBlock = Integer.MIN_VALUE;

  public DownMovement() {
    this.targetToMineBlock = FEET_POSITION_RELATIVE_BLOCK.sub(0, 1, 0);
  }

  public static void registerDownMovements(
    Consumer<GraphAction> callback,
    BiConsumer<SFVec3i, MinecraftGraph.MovementSubscription<?>> blockSubscribers) {
    callback.accept(registerDownMovement(blockSubscribers, new DownMovement()));
  }

  public static DownMovement registerDownMovement(
    BiConsumer<SFVec3i, MinecraftGraph.MovementSubscription<?>> blockSubscribers,
    DownMovement movement) {
    {
      for (var safetyBlock : movement.listSafetyCheckBlocks()) {
        blockSubscribers
          .accept(safetyBlock, new DownMovementBlockSubscription(DownMovementBlockSubscription.SubscriptionType.DOWN_SAFETY_CHECK));
      }
    }

    {
      for (var obstructingBlock : movement.listObstructFallCheckBlocks()) {
        blockSubscribers
          .accept(obstructingBlock, new DownMovementBlockSubscription(DownMovementBlockSubscription.SubscriptionType.MOVEMENT_OBSTRUCTING_FALL_CHECK));
      }
    }

    {
      var freeBlock = movement.blockToBreak();
      blockSubscribers
        .accept(freeBlock.key(), new DownMovementBlockSubscription(DownMovementBlockSubscription.SubscriptionType.MOVEMENT_FREE, 0, freeBlock.value()));
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
            .accept(block.position(), new DownMovementBlockSubscription(
              DownMovementBlockSubscription.SubscriptionType.MOVEMENT_BREAK_SAFETY_CHECK,
              i,
              block.type()));
        }
      }
    }

    return movement;
  }

  public Pair<SFVec3i, BlockFace> blockToBreak() {
    return Pair.of(targetToMineBlock, BlockFace.TOP);
  }

  // These blocks are possibly safe blocks we can fall on top of
  public List<SFVec3i> listSafetyCheckBlocks() {
    var requiredFreeBlocks = new ArrayList<SFVec3i>();

    // Falls one block
    requiredFreeBlocks.add(FEET_POSITION_RELATIVE_BLOCK.sub(0, 2, 0));

    // Falls two blocks
    requiredFreeBlocks.add(FEET_POSITION_RELATIVE_BLOCK.sub(0, 3, 0));

    // Falls three blocks
    requiredFreeBlocks.add(FEET_POSITION_RELATIVE_BLOCK.sub(0, 4, 0));

    return requiredFreeBlocks;
  }

  public List<SFVec3i> listObstructFallCheckBlocks() {
    var requiredFreeBlocks = new ArrayList<SFVec3i>();

    // Block below the block we mine can obstruct a 2 block fall
    requiredFreeBlocks.add(FEET_POSITION_RELATIVE_BLOCK.sub(0, 2, 0));

    // Block below the block we mine can obstruct a 3 block fall
    requiredFreeBlocks.add(FEET_POSITION_RELATIVE_BLOCK.sub(0, 3, 0));

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
  public List<GraphInstructions> getInstructions(MinecraftGraph graph, NodeState node) {
    if (closestBlockToFallOn == Integer.MIN_VALUE || closestObstructingBlock > closestBlockToFallOn) {
      return Collections.emptyList();
    }

    var cost = 0D;

    cost +=
      switch (closestBlockToFallOn) {
        case -2 -> Costs.FALL_1;
        case -3 -> Costs.FALL_2;
        case -4 -> Costs.FALL_3;
        default -> throw new IllegalStateException("Unexpected value: " + closestBlockToFallOn);
      };

    cost += breakCost.miningCost();

    var absoluteTargetFeetBlock = node.blockPosition().add(0, closestBlockToFallOn + 1, 0);

    return Collections.singletonList(new GraphInstructions(
      new NodeState(absoluteTargetFeetBlock, node.usableBlockItems() + (breakCost.willDropUsableBlockItem() ? 1 : 0)),
      cost,
      List.of(new BlockBreakAction(breakCost))));
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

  record DownMovementBlockSubscription(
    SubscriptionType type,
    int blockArrayIndex,
    BlockFace blockBreakSideHint,
    BlockSafetyData.BlockSafetyType safetyType) implements MinecraftGraph.MovementSubscription<DownMovement> {
    DownMovementBlockSubscription(SubscriptionType type) {
      this(type, -1, null, null);
    }

    DownMovementBlockSubscription(SubscriptionType type, int blockArrayIndex, BlockFace blockBreakSideHint) {
      this(type, blockArrayIndex, blockBreakSideHint, null);
    }

    DownMovementBlockSubscription(
      SubscriptionType subscriptionType,
      int i,
      BlockSafetyData.BlockSafetyType type) {
      this(subscriptionType, i, null, type);
    }

    @Override
    public MinecraftGraph.SubscriptionSingleResult processBlock(MinecraftGraph graph, SFVec3i key, DownMovement downMovement, LazyBoolean isFree,
                                                                BlockState blockState, SFVec3i absoluteKey) {
      return switch (type) {
        case MOVEMENT_FREE -> {
          if (graph.disallowedToBreakBlock(absoluteKey)
            || graph.disallowedToBreakType(blockState.blockType())) {
            // No way to break this block
            yield MinecraftGraph.SubscriptionSingleResult.IMPOSSIBLE;
          }

          var cacheableMiningCost = graph.inventory().getMiningCosts(graph.tagsState(), blockState);
          // We can mine this block, lets add costs and continue
          downMovement.breakCost(
            new MovementMiningCost(
              absoluteKey,
              cacheableMiningCost.miningCost(),
              cacheableMiningCost.willDropUsableBlockItem(),
              blockBreakSideHint));
          yield MinecraftGraph.SubscriptionSingleResult.CONTINUE;
        }
        case DOWN_SAFETY_CHECK -> {
          var yLevel = key.y;

          if (yLevel < downMovement.closestBlockToFallOn()) {
            // We already found a block to fall on, above this one
            yield MinecraftGraph.SubscriptionSingleResult.CONTINUE;
          }

          if (BlockTypeHelper.isSafeBlockToStandOn(blockState)) {
            // We found a block to fall on
            downMovement.closestBlockToFallOn(yLevel);
          }

          yield MinecraftGraph.SubscriptionSingleResult.CONTINUE;
        }
        case MOVEMENT_BREAK_SAFETY_CHECK -> {
          var unsafe = safetyType.isUnsafeBlock(blockState);

          if (unsafe) {
            // We know already WE MUST dig the block below for this action
            // So if one block around the block below is unsafe, we can't do this action
            yield MinecraftGraph.SubscriptionSingleResult.IMPOSSIBLE;
          }

          // All good, we can continue
          yield MinecraftGraph.SubscriptionSingleResult.CONTINUE;
        }
        case MOVEMENT_OBSTRUCTING_FALL_CHECK -> {
          var yLevel = key.y;

          if (yLevel < downMovement.closestObstructingBlock()) {
            // We already found a higher obstructing block
            yield MinecraftGraph.SubscriptionSingleResult.CONTINUE;
          }

          if (yLevel < downMovement.closestBlockToFallOn()) {
            // We only search blocks above the closest block to fall on
            yield MinecraftGraph.SubscriptionSingleResult.CONTINUE;
          }

          if (isFree.get()) {
            yield MinecraftGraph.SubscriptionSingleResult.CONTINUE;
          }

          // We found a block that obstructs our fall
          downMovement.closestObstructingBlock(yLevel);
          yield MinecraftGraph.SubscriptionSingleResult.CONTINUE;
        }
      };
    }

    @Override
    public DownMovement castAction(GraphAction action) {
      return (DownMovement) action;
    }

    enum SubscriptionType {
      MOVEMENT_FREE,
      DOWN_SAFETY_CHECK,
      MOVEMENT_BREAK_SAFETY_CHECK,
      MOVEMENT_OBSTRUCTING_FALL_CHECK
    }
  }
}
