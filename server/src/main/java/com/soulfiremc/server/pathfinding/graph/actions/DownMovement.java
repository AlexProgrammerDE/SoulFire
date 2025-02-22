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
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.pathfinding.execution.BlockBreakAction;
import com.soulfiremc.server.pathfinding.graph.BlockFace;
import com.soulfiremc.server.pathfinding.graph.GraphInstructions;
import com.soulfiremc.server.pathfinding.graph.MinecraftGraph;
import com.soulfiremc.server.pathfinding.graph.actions.movement.ActionDirection;
import com.soulfiremc.server.pathfinding.graph.actions.movement.BlockSafetyType;
import com.soulfiremc.server.pathfinding.graph.actions.movement.MovementMiningCost;
import com.soulfiremc.server.pathfinding.graph.actions.movement.SkyDirection;
import com.soulfiremc.server.util.SFBlockHelpers;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public final class DownMovement extends GraphAction implements Cloneable {
  private static final SFVec3i FEET_POSITION_RELATIVE_BLOCK = SFVec3i.ZERO;
  private final SFVec3i targetToMineBlock;
  // Mutable
  private MovementMiningCost breakCost;
  // Mutable
  private int closestBlockToFallOn = Integer.MIN_VALUE;
  // Mutable
  private int closestObstructingBlock = Integer.MIN_VALUE;

  private DownMovement(SubscriptionConsumer blockSubscribers) {
    super(ActionDirection.DOWN);
    this.targetToMineBlock = FEET_POSITION_RELATIVE_BLOCK.sub(0, 1, 0);

    this.registerSafetyCheckBlocks(blockSubscribers);
    this.registerObstructFallCheckBlocks(blockSubscribers);
    this.registerBlockToBreak(blockSubscribers);
    this.registerCheckSafeMineBlocks(blockSubscribers);
  }

  public static void registerDownMovements(Consumer<GraphAction> callback, SubscriptionConsumer blockSubscribers) {
    callback.accept(new DownMovement(blockSubscribers));
  }

  // These blocks are possibly safe blocks we can fall on top of
  private void registerSafetyCheckBlocks(SubscriptionConsumer blockSubscribers) {
    // Falls one block
    blockSubscribers.subscribe(FEET_POSITION_RELATIVE_BLOCK.sub(0, 2, 0), DownSafetyCheckSubscription.INSTANCE);

    // Falls two blocks
    blockSubscribers.subscribe(FEET_POSITION_RELATIVE_BLOCK.sub(0, 3, 0), DownSafetyCheckSubscription.INSTANCE);

    // Falls three blocks
    blockSubscribers.subscribe(FEET_POSITION_RELATIVE_BLOCK.sub(0, 4, 0), DownSafetyCheckSubscription.INSTANCE);
  }

  private void registerObstructFallCheckBlocks(SubscriptionConsumer blockSubscribers) {
    // Block below the block we mine can obstruct a 2 block fall
    blockSubscribers.subscribe(FEET_POSITION_RELATIVE_BLOCK.sub(0, 2, 0), ObstructingFallCheckSubscription.INSTANCE);

    // Block below the block we mine can obstruct a 3 block fall
    blockSubscribers.subscribe(FEET_POSITION_RELATIVE_BLOCK.sub(0, 3, 0), ObstructingFallCheckSubscription.INSTANCE);
  }

  private void registerBlockToBreak(SubscriptionConsumer blockSubscribers) {
    blockSubscribers.subscribe(targetToMineBlock, new MovementFreeSubscription(BlockFace.TOP));
  }

  private void registerCheckSafeMineBlocks(SubscriptionConsumer blockSubscribers) {
    for (var skyDirection : SkyDirection.VALUES) {
      blockSubscribers.subscribe(
        skyDirection.offset(targetToMineBlock),
        new MovementBreakSafetyCheckSubscription(BlockSafetyType.FLUIDS));
    }
  }

  @Override
  public List<GraphInstructions> getInstructions(MinecraftGraph graph, SFVec3i node) {
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

    var absoluteTargetFeetBlock = node.add(0, closestBlockToFallOn + 1, 0);

    return Collections.singletonList(new GraphInstructions(
      absoluteTargetFeetBlock,
      breakCost.willDropUsableBlockItem() ? 1 : 0,
      false,
      actionDirection,
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

  private interface DownMovementSubscription extends MinecraftGraph.MovementSubscription<DownMovement> {
  }

  private record MovementFreeSubscription(BlockFace blockBreakSideHint) implements DownMovementSubscription {
    @Override
    public MinecraftGraph.SubscriptionSingleResult processBlock(MinecraftGraph graph, SFVec3i key, DownMovement downMovement,
                                                                BlockState blockState, SFVec3i absoluteKey) {
      if (graph.disallowedToBreakBlock(absoluteKey)
        || graph.disallowedToBreakBlockType(blockState.blockType())) {
        // No way to break this block
        return MinecraftGraph.SubscriptionSingleResult.IMPOSSIBLE;
      }

      var cacheableMiningCost = graph.inventory().getMiningCosts(blockState);
      // We can mine this block, lets add costs and continue
      downMovement.breakCost = new MovementMiningCost(
        absoluteKey,
        cacheableMiningCost.miningCost(),
        cacheableMiningCost.willDropUsableBlockItem(),
        blockBreakSideHint);
      return MinecraftGraph.SubscriptionSingleResult.CONTINUE;
    }
  }

  private record DownSafetyCheckSubscription() implements DownMovementSubscription {
    private static final DownSafetyCheckSubscription INSTANCE = new DownSafetyCheckSubscription();

    @Override
    public MinecraftGraph.SubscriptionSingleResult processBlock(MinecraftGraph graph, SFVec3i key, DownMovement downMovement,
                                                                BlockState blockState, SFVec3i absoluteKey) {
      var yLevel = key.y;

      if (yLevel < downMovement.closestBlockToFallOn) {
        // We already found a block to fall on, above this one
        return MinecraftGraph.SubscriptionSingleResult.CONTINUE;
      }

      if (SFBlockHelpers.isSafeBlockToStandOn(blockState)) {
        // We found a block to fall on
        downMovement.closestBlockToFallOn = yLevel;
      }

      return MinecraftGraph.SubscriptionSingleResult.CONTINUE;
    }
  }

  private record MovementBreakSafetyCheckSubscription(BlockSafetyType safetyType) implements DownMovementSubscription {
    @Override
    public MinecraftGraph.SubscriptionSingleResult processBlock(MinecraftGraph graph, SFVec3i key, DownMovement downMovement,
                                                                BlockState blockState, SFVec3i absoluteKey) {
      var unsafe = safetyType.isUnsafeBlock(blockState);

      if (unsafe) {
        // We know already WE MUST dig the block below for this action
        // So if one block around the block below is unsafe, we can't do this action
        return MinecraftGraph.SubscriptionSingleResult.IMPOSSIBLE;
      }

      // All good, we can continue
      return MinecraftGraph.SubscriptionSingleResult.CONTINUE;
    }
  }

  private record ObstructingFallCheckSubscription() implements DownMovementSubscription {
    private static final ObstructingFallCheckSubscription INSTANCE = new ObstructingFallCheckSubscription();

    @Override
    public MinecraftGraph.SubscriptionSingleResult processBlock(MinecraftGraph graph, SFVec3i key, DownMovement downMovement,
                                                                BlockState blockState, SFVec3i absoluteKey) {
      var yLevel = key.y;

      if (yLevel < downMovement.closestObstructingBlock) {
        // We already found a higher obstructing block
        return MinecraftGraph.SubscriptionSingleResult.CONTINUE;
      }

      if (yLevel < downMovement.closestBlockToFallOn) {
        // We only search blocks above the closest block to fall on
        return MinecraftGraph.SubscriptionSingleResult.CONTINUE;
      }

      if (SFBlockHelpers.isBlockFree(blockState)) {
        return MinecraftGraph.SubscriptionSingleResult.CONTINUE;
      }

      // We found a block that obstructs our fall
      downMovement.closestObstructingBlock = yLevel;
      return MinecraftGraph.SubscriptionSingleResult.CONTINUE;
    }
  }
}
