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
import com.soulfiremc.server.pathfinding.execution.JumpAndPlaceBelowAction;
import com.soulfiremc.server.pathfinding.execution.WorldAction;
import com.soulfiremc.server.pathfinding.graph.BlockFace;
import com.soulfiremc.server.pathfinding.graph.GraphInstructions;
import com.soulfiremc.server.pathfinding.graph.MinecraftGraph;
import com.soulfiremc.server.pathfinding.graph.actions.movement.ActionDirection;
import com.soulfiremc.server.pathfinding.graph.actions.movement.BlockSafetyType;
import com.soulfiremc.server.pathfinding.graph.actions.movement.MovementMiningCost;
import com.soulfiremc.server.pathfinding.graph.actions.movement.SkyDirection;
import com.soulfiremc.server.protocol.bot.MultiPlayerGameMode;
import com.soulfiremc.server.util.SFBlockHelpers;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public final class UpMovement extends GraphAction implements Cloneable {
  private static final SFVec3i FEET_POSITION_RELATIVE_BLOCK = SFVec3i.ZERO;
  private final SFVec3i targetFeetBlock;
  // Mutable
  private MovementMiningCost[] blockBreakCosts;
  // Mutable
  private boolean[] unsafeToBreak;
  // Mutable
  private boolean[] noNeedToBreak;

  private UpMovement(SubscriptionConsumer blockSubscribers) {
    super(ActionDirection.UP);
    this.targetFeetBlock = FEET_POSITION_RELATIVE_BLOCK.add(0, 1, 0);

    var arraySize = this.registerRequiredFreeBlocks(blockSubscribers);
    this.blockBreakCosts = new MovementMiningCost[arraySize];
    this.unsafeToBreak = new boolean[arraySize];
    this.noNeedToBreak = new boolean[arraySize];

    this.registerBlockPlacePosition(blockSubscribers);
  }

  public static void registerUpMovements(Consumer<GraphAction> callback, SubscriptionConsumer blockSubscribers) {
    callback.accept(new UpMovement(blockSubscribers));
  }

  private int registerRequiredFreeBlocks(SubscriptionConsumer blockSubscribers) {
    var blockIndexCounter = 0;

    {
      var aboveHeadBlockIndex = blockIndexCounter++;
      var aboveHead = FEET_POSITION_RELATIVE_BLOCK.add(0, 2, 0);

      // The one above the head to jump
      blockSubscribers.subscribe(aboveHead, new MovementFreeSubscription(aboveHeadBlockIndex, BlockFace.BOTTOM));

      blockSubscribers.subscribe(aboveHead.add(0, 1, 0),
        new MovementBreakSafetyCheckSubscription(aboveHeadBlockIndex, BlockSafetyType.FALLING_AND_FLUIDS));

      for (var skyDirection : SkyDirection.VALUES) {
        blockSubscribers.subscribe(skyDirection.offset(aboveHead),
          new MovementBreakSafetyCheckSubscription(aboveHeadBlockIndex, BlockSafetyType.FLUIDS));
      }
    }

    return blockIndexCounter;
  }

  private void registerBlockPlacePosition(SubscriptionConsumer blockSubscribers) {
    blockSubscribers.subscribe(FEET_POSITION_RELATIVE_BLOCK, MovementSolidSubscription.INSTANCE);
  }

  @Override
  public List<GraphInstructions> getInstructions(MinecraftGraph graph, SFVec3i node) {
    var actions = new ArrayList<WorldAction>();
    var cost = Costs.JUMP_UP_BLOCK;

    var usableBlockItemsDiff = 0;
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

    var requiresOneBlock = usableBlockItemsDiff <= 0;
    var absoluteTargetFeetBlock = node.add(targetFeetBlock);

    // We need a block to place below us
    if (graph.doUsableBlocksDecreaseWhenPlaced()) {
      // After the place we'll have one less usable block item
      usableBlockItemsDiff--;
    }

    cost += Costs.PLACE_BLOCK_PENALTY;

    // Where we are standing right now, we'll place the target block below us after jumping
    actions.add(
      new JumpAndPlaceBelowAction(
        node,
        new MultiPlayerGameMode.BlockPlaceAgainstData(
          node.sub(0, 1, 0), BlockFace.TOP)));

    return Collections.singletonList(new GraphInstructions(
      absoluteTargetFeetBlock,
      usableBlockItemsDiff,
      requiresOneBlock,
      actionDirection,
      cost,
      actions
    ));
  }

  @Override
  public UpMovement copy() {
    return this.clone();
  }

  @Override
  public UpMovement clone() {
    try {
      var c = (UpMovement) super.clone();

      c.blockBreakCosts = new MovementMiningCost[this.blockBreakCosts.length];
      c.unsafeToBreak = new boolean[this.unsafeToBreak.length];
      c.noNeedToBreak = new boolean[this.noNeedToBreak.length];

      return c;
    } catch (CloneNotSupportedException cantHappen) {
      throw new InternalError();
    }
  }

  private interface UpMovementSubscription extends MinecraftGraph.MovementSubscription<UpMovement> {
  }

  private record MovementFreeSubscription(int blockArrayIndex, BlockFace blockBreakSideHint) implements UpMovementSubscription {
    @Override
    public MinecraftGraph.SubscriptionSingleResult processBlock(MinecraftGraph graph, SFVec3i key, UpMovement upMovement,
                                                                BlockState blockState, SFVec3i absoluteKey) {
      if (SFBlockHelpers.isBlockFree(blockState)) {
        upMovement.noNeedToBreak[blockArrayIndex] = true;
        return MinecraftGraph.SubscriptionSingleResult.CONTINUE;
      }

      // Search for a way to break this block
      if (graph.disallowedToBreakBlock(absoluteKey)
        || graph.disallowedToBreakBlockType(blockState.blockType())
        || upMovement.unsafeToBreak[blockArrayIndex]) {
        // No way to break this block
        return MinecraftGraph.SubscriptionSingleResult.IMPOSSIBLE;
      }

      var cacheableMiningCost = graph.inventory().getMiningCosts(blockState);
      // We can mine this block, lets add costs and continue
      upMovement.blockBreakCosts[blockArrayIndex] =
        new MovementMiningCost(
          absoluteKey,
          cacheableMiningCost.miningCost(),
          cacheableMiningCost.willDropUsableBlockItem(),
          blockBreakSideHint);
      return MinecraftGraph.SubscriptionSingleResult.CONTINUE;
    }
  }

  private record MovementSolidSubscription() implements UpMovementSubscription {
    private static final MovementSolidSubscription INSTANCE = new MovementSolidSubscription();

    @Override
    public MinecraftGraph.SubscriptionSingleResult processBlock(MinecraftGraph graph, SFVec3i key, UpMovement upMovement,
                                                                BlockState blockState, SFVec3i absoluteKey) {
      // Towering requires placing a block at old feet position
      if (graph.disallowedToPlaceBlock(absoluteKey)) {
        return MinecraftGraph.SubscriptionSingleResult.IMPOSSIBLE;
      }

      return MinecraftGraph.SubscriptionSingleResult.CONTINUE;
    }
  }

  private record MovementBreakSafetyCheckSubscription(int blockArrayIndex, BlockSafetyType safetyType) implements UpMovementSubscription {
    @Override
    public MinecraftGraph.SubscriptionSingleResult processBlock(MinecraftGraph graph, SFVec3i key, UpMovement upMovement,
                                                                BlockState blockState, SFVec3i absoluteKey) {
      // There is no need to break this block, so there is no need for safety checks
      if (upMovement.noNeedToBreak[blockArrayIndex]) {
        return MinecraftGraph.SubscriptionSingleResult.CONTINUE;
      }

      // The block was already marked as unsafe
      if (upMovement.unsafeToBreak[blockArrayIndex]) {
        return MinecraftGraph.SubscriptionSingleResult.CONTINUE;
      }

      var unsafe = safetyType.isUnsafeBlock(blockState);

      if (!unsafe) {
        // All good, we can continue
        return MinecraftGraph.SubscriptionSingleResult.CONTINUE;
      }

      var currentValue = upMovement.blockBreakCosts;

      if (currentValue != null) {
        // We learned that this block needs to be broken, so we need to set it as impossible
        return MinecraftGraph.SubscriptionSingleResult.IMPOSSIBLE;
      }

      // Store for a later time that this is unsafe,
      // so if we check this block,
      // we know it's unsafe
      upMovement.unsafeToBreak[blockArrayIndex] = true;

      return MinecraftGraph.SubscriptionSingleResult.CONTINUE;
    }
  }
}
