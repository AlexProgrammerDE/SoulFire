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
import com.soulfiremc.server.util.LazyBoolean;
import it.unimi.dsi.fastutil.Pair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
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

  public static void registerUpMovements(
    Consumer<GraphAction> callback,
    BiConsumer<SFVec3i, MinecraftGraph.MovementSubscription<?>> blockSubscribers) {
    callback.accept(registerUpMovement(blockSubscribers, new UpMovement()));
  }

  private static UpMovement registerUpMovement(
    BiConsumer<SFVec3i, MinecraftGraph.MovementSubscription<?>> blockSubscribers,
    UpMovement movement) {
    {
      var blockId = 0;
      for (var freeBlock : movement.requiredFreeBlocks()) {
        blockSubscribers
          .accept(freeBlock.key(), new UpMovementBlockSubscription(UpMovementBlockSubscription.SubscriptionType.MOVEMENT_FREE, blockId++, freeBlock.value()));
      }
    }

    {
      blockSubscribers
        .accept(movement.blockPlacePosition(), new UpMovementBlockSubscription(UpMovementBlockSubscription.SubscriptionType.MOVEMENT_SOLID));
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
            .accept(block.position(), new UpMovementBlockSubscription(
              UpMovementBlockSubscription.SubscriptionType.MOVEMENT_BREAK_SAFETY_CHECK,
              i,
              block.type()));
        }
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

  public SFVec3i blockPlacePosition() {
    return FEET_POSITION_RELATIVE_BLOCK;
  }

  @Override
  public List<GraphInstructions> getInstructions(MinecraftGraph graph, NodeState node) {
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

    var absoluteTargetFeetBlock = node.blockPosition().add(targetFeetBlock);
    var afterBreakUsableBlockItems = node.usableBlockItems() + usableBlockItemsDiff;

    // We need a block to place below us
    if (afterBreakUsableBlockItems < 1) {
      return Collections.emptyList();
    } else if (graph.doUsableBlocksDecreaseWhenPlaced()) {
      // After the place we'll have one less usable block item
      afterBreakUsableBlockItems--;
      cost += Costs.PLACE_BLOCK;
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

  record UpMovementBlockSubscription(
    SubscriptionType type,
    int blockArrayIndex,
    BlockFace blockBreakSideHint,
    BlockSafetyData.BlockSafetyType safetyType) implements MinecraftGraph.MovementSubscription<UpMovement> {
    UpMovementBlockSubscription(SubscriptionType type) {
      this(type, -1, null, null);
    }

    UpMovementBlockSubscription(SubscriptionType type, int blockArrayIndex, BlockFace blockBreakSideHint) {
      this(type, blockArrayIndex, blockBreakSideHint, null);
    }

    UpMovementBlockSubscription(
      SubscriptionType subscriptionType,
      int i,
      BlockSafetyData.BlockSafetyType type) {
      this(subscriptionType, i, null, type);
    }

    @Override
    public MinecraftGraph.SubscriptionSingleResult processBlock(MinecraftGraph graph, SFVec3i key, UpMovement upMovement, LazyBoolean isFree,
                                                                BlockState blockState, SFVec3i absoluteKey) {
      return switch (type) {
        case MOVEMENT_FREE -> {
          if (isFree.get()) {
            upMovement.noNeedToBreak()[blockArrayIndex] = true;
            yield MinecraftGraph.SubscriptionSingleResult.CONTINUE;
          }

          // Search for a way to break this block
          if (graph.disallowedToBreakBlock(absoluteKey)
            || graph.disallowedToBreakType(blockState.blockType())
            || upMovement.unsafeToBreak()[blockArrayIndex]) {
            // No way to break this block
            yield MinecraftGraph.SubscriptionSingleResult.IMPOSSIBLE;
          }

          var cacheableMiningCost = graph.inventory().getMiningCosts(graph.tagsState(), blockState);
          // We can mine this block, lets add costs and continue
          upMovement.blockBreakCosts()[blockArrayIndex] =
            new MovementMiningCost(
              absoluteKey,
              cacheableMiningCost.miningCost(),
              cacheableMiningCost.willDropUsableBlockItem(),
              blockBreakSideHint);
          yield MinecraftGraph.SubscriptionSingleResult.CONTINUE;
        }
        case MOVEMENT_SOLID -> {
          // Towering requires placing a block at old feet position
          if (graph.disallowedToPlaceBlock(absoluteKey)) {
            yield MinecraftGraph.SubscriptionSingleResult.IMPOSSIBLE;
          }

          yield MinecraftGraph.SubscriptionSingleResult.CONTINUE;
        }
        case MOVEMENT_BREAK_SAFETY_CHECK -> {
          // There is no need to break this block, so there is no need for safety checks
          if (upMovement.noNeedToBreak()[blockArrayIndex]) {
            yield MinecraftGraph.SubscriptionSingleResult.CONTINUE;
          }

          // The block was already marked as unsafe
          if (upMovement.unsafeToBreak()[blockArrayIndex]) {
            yield MinecraftGraph.SubscriptionSingleResult.CONTINUE;
          }

          var unsafe = safetyType.isUnsafeBlock(blockState);

          if (!unsafe) {
            // All good, we can continue
            yield MinecraftGraph.SubscriptionSingleResult.CONTINUE;
          }

          var currentValue = upMovement.blockBreakCosts()[blockArrayIndex];

          if (currentValue != null) {
            // We learned that this block needs to be broken, so we need to set it as impossible
            yield MinecraftGraph.SubscriptionSingleResult.IMPOSSIBLE;
          }

          // Store for a later time that this is unsafe,
          // so if we check this block,
          // we know it's unsafe
          upMovement.unsafeToBreak()[blockArrayIndex] = true;

          yield MinecraftGraph.SubscriptionSingleResult.CONTINUE;
        }
      };
    }

    @Override
    public UpMovement castAction(GraphAction action) {
      return (UpMovement) action;
    }

    enum SubscriptionType {
      MOVEMENT_FREE,
      MOVEMENT_SOLID,
      MOVEMENT_BREAK_SAFETY_CHECK
    }
  }
}
