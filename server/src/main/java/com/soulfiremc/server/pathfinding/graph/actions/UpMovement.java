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
import com.soulfiremc.server.pathfinding.execution.JumpAndPlaceBelowAction;
import com.soulfiremc.server.pathfinding.execution.WorldAction;
import com.soulfiremc.server.pathfinding.graph.BlockFace;
import com.soulfiremc.server.pathfinding.graph.GraphInstructions;
import com.soulfiremc.server.pathfinding.graph.MinecraftGraph;
import com.soulfiremc.server.pathfinding.graph.actions.movement.BlockSafetyData;
import com.soulfiremc.server.pathfinding.graph.actions.movement.MovementMiningCost;
import com.soulfiremc.server.pathfinding.graph.actions.movement.SkyDirection;
import com.soulfiremc.server.protocol.bot.BotActionManager;
import com.soulfiremc.server.util.structs.LazyBoolean;
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
  private MovementMiningCost blockBreakCost;
  // Mutable
  private boolean unsafeToBreak;
  // Mutable
  private boolean noNeedToBreak;

  public UpMovement() {
    this.targetFeetBlock = FEET_POSITION_RELATIVE_BLOCK.add(0, 1, 0);
  }

  public static void registerUpMovements(
    Consumer<GraphAction> callback,
    SubscriptionConsumer blockSubscribers) {
    callback.accept(registerUpMovement(blockSubscribers, new UpMovement()));
  }

  private static UpMovement registerUpMovement(
    SubscriptionConsumer blockSubscribers,
    UpMovement movement) {
    movement.registerRequiredFreeBlocks(blockSubscribers);
    movement.registerBlockPlacePosition(blockSubscribers);
    movement.registerCheckSafeMineBlocks(blockSubscribers);

    return movement;
  }

  private void registerRequiredFreeBlocks(SubscriptionConsumer blockSubscribers) {
    // The one above the head to jump
    blockSubscribers.subscribe(FEET_POSITION_RELATIVE_BLOCK.add(0, 2, 0), new MovementFreeSubscription(BlockFace.BOTTOM));
  }

  private void registerBlockPlacePosition(SubscriptionConsumer blockSubscribers) {
    blockSubscribers.subscribe(FEET_POSITION_RELATIVE_BLOCK, MovementSolidSubscription.INSTANCE);
  }

  private void registerCheckSafeMineBlocks(SubscriptionConsumer blockSubscribers) {
    var aboveHead = FEET_POSITION_RELATIVE_BLOCK.add(0, 2, 0);

    blockSubscribers.subscribe(aboveHead.add(0, 1, 0), new MovementBreakSafetyCheckSubscription(BlockSafetyData.BlockSafetyType.FALLING_AND_FLUIDS));

    for (var skyDirection : SkyDirection.VALUES) {
      blockSubscribers.subscribe(skyDirection.offset(aboveHead), new MovementBreakSafetyCheckSubscription(BlockSafetyData.BlockSafetyType.FLUIDS));
    }
  }

  @Override
  public List<GraphInstructions> getInstructions(MinecraftGraph graph, NodeState node) {
    var actions = new ArrayList<WorldAction>();
    var cost = Costs.JUMP_UP_BLOCK;

    var usableBlockItemsDiff = 0;
    if (blockBreakCost != null) {
      cost += blockBreakCost.miningCost();
      actions.add(new BlockBreakAction(blockBreakCost));

      if (blockBreakCost.willDropUsableBlockItem()) {
        usableBlockItemsDiff++;
      }
    }

    var absoluteTargetFeetBlock = node.blockPosition().add(targetFeetBlock);
    var afterBreakUsableBlockItems = node.usableBlockItems() + usableBlockItemsDiff;

    // We need a block to place below us
    if (afterBreakUsableBlockItems < 1) {
      // Not enough blocks to place below us
      return Collections.emptyList();
    } else {
      if (graph.doUsableBlocksDecreaseWhenPlaced()) {
        // After the place we'll have one less usable block item
        afterBreakUsableBlockItems--;
      }

      cost += Costs.PLACE_BLOCK_PENALTY;
    }

    // Where we are standing right now, we'll place the target block below us after jumping
    actions.add(
      new JumpAndPlaceBelowAction(
        node.blockPosition(),
        new BotActionManager.BlockPlaceAgainstData(
          node.blockPosition().sub(0, 1, 0), BlockFace.TOP)));

    return Collections.singletonList(new GraphInstructions(
      new NodeState(absoluteTargetFeetBlock, afterBreakUsableBlockItems), cost, actions));
  }

  @Override
  public UpMovement copy() {
    return this.clone();
  }

  @Override
  public UpMovement clone() {
    try {
      return (UpMovement) super.clone();
    } catch (CloneNotSupportedException cantHappen) {
      throw new InternalError();
    }
  }

  interface UpMovementSubscription extends MinecraftGraph.MovementSubscription<UpMovement> {
    @Override
    default UpMovement castAction(GraphAction action) {
      return (UpMovement) action;
    }
  }

  record MovementFreeSubscription(BlockFace blockBreakSideHint) implements UpMovementSubscription {
    @Override
    public MinecraftGraph.SubscriptionSingleResult processBlock(MinecraftGraph graph, SFVec3i key, UpMovement upMovement, LazyBoolean isFree,
                                                                BlockState blockState, SFVec3i absoluteKey) {
      if (isFree.get()) {
        upMovement.noNeedToBreak = true;
        return MinecraftGraph.SubscriptionSingleResult.CONTINUE;
      }

      // Search for a way to break this block
      if (graph.disallowedToBreakBlock(absoluteKey)
        || graph.disallowedToBreakBlockType(blockState.blockType())
        || upMovement.unsafeToBreak) {
        // No way to break this block
        return MinecraftGraph.SubscriptionSingleResult.IMPOSSIBLE;
      }

      var cacheableMiningCost = graph.inventory().getMiningCosts(graph.tagsState(), blockState);
      // We can mine this block, lets add costs and continue
      upMovement.blockBreakCost =
        new MovementMiningCost(
          absoluteKey,
          cacheableMiningCost.miningCost(),
          cacheableMiningCost.willDropUsableBlockItem(),
          blockBreakSideHint);
      return MinecraftGraph.SubscriptionSingleResult.CONTINUE;
    }
  }

  record MovementSolidSubscription() implements UpMovementSubscription {
    private static final MovementSolidSubscription INSTANCE = new MovementSolidSubscription();

    @Override
    public MinecraftGraph.SubscriptionSingleResult processBlock(MinecraftGraph graph, SFVec3i key, UpMovement upMovement, LazyBoolean isFree,
                                                                BlockState blockState, SFVec3i absoluteKey) {
      // Towering requires placing a block at old feet position
      if (graph.disallowedToPlaceBlock(absoluteKey)) {
        return MinecraftGraph.SubscriptionSingleResult.IMPOSSIBLE;
      }

      return MinecraftGraph.SubscriptionSingleResult.CONTINUE;
    }
  }

  record MovementBreakSafetyCheckSubscription(BlockSafetyData.BlockSafetyType safetyType) implements UpMovementSubscription {
    @Override
    public MinecraftGraph.SubscriptionSingleResult processBlock(MinecraftGraph graph, SFVec3i key, UpMovement upMovement, LazyBoolean isFree,
                                                                BlockState blockState, SFVec3i absoluteKey) {
      // There is no need to break this block, so there is no need for safety checks
      if (upMovement.noNeedToBreak) {
        return MinecraftGraph.SubscriptionSingleResult.CONTINUE;
      }

      // The block was already marked as unsafe
      if (upMovement.unsafeToBreak) {
        return MinecraftGraph.SubscriptionSingleResult.CONTINUE;
      }

      var unsafe = safetyType.isUnsafeBlock(blockState);

      if (!unsafe) {
        // All good, we can continue
        return MinecraftGraph.SubscriptionSingleResult.CONTINUE;
      }

      var currentValue = upMovement.blockBreakCost;

      if (currentValue != null) {
        // We learned that this block needs to be broken, so we need to set it as impossible
        return MinecraftGraph.SubscriptionSingleResult.IMPOSSIBLE;
      }

      // Store for a later time that this is unsafe,
      // so if we check this block,
      // we know it's unsafe
      upMovement.unsafeToBreak = true;

      return MinecraftGraph.SubscriptionSingleResult.CONTINUE;
    }
  }
}
