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

import com.soulfiremc.server.data.BlockItems;
import com.soulfiremc.server.data.BlockState;
import com.soulfiremc.server.pathfinding.Costs;
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
import com.soulfiremc.server.util.BlockTypeHelper;
import com.soulfiremc.server.util.ObjectReference;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.util.TriState;

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
          .accept(freeBlock.key(), new UpMovementBlockSubscription(MinecraftGraph.SubscriptionType.MOVEMENT_FREE, blockId++, freeBlock.value()));
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
            .accept(block.position(), new UpMovementBlockSubscription(
              MinecraftGraph.SubscriptionType.MOVEMENT_BREAK_SAFETY_CHECK,
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
  public List<GraphInstructions> getInstructions(SFVec3i node) {
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

    return Collections.singletonList(new GraphInstructions(
      absoluteTargetFeetBlock, cost, actions));
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

  public record UpMovementBlockSubscription(
    MinecraftGraph.SubscriptionType type,
    int blockArrayIndex,
    BlockFace blockBreakSideHint,
    BlockSafetyData.BlockSafetyType safetyType) implements MinecraftGraph.MovementSubscription<UpMovement> {
    public UpMovementBlockSubscription(MinecraftGraph.SubscriptionType type, int blockArrayIndex, BlockFace blockBreakSideHint) {
      this(type, blockArrayIndex, blockBreakSideHint, null);
    }

    public UpMovementBlockSubscription(
      MinecraftGraph.SubscriptionType subscriptionType,
      int i,
      BlockSafetyData.BlockSafetyType type) {
      this(subscriptionType, i, null, type);
    }

    @Override
    public MinecraftGraph.SubscriptionSingleResult processBlock(MinecraftGraph graph, SFVec3i key, UpMovement upMovement, ObjectReference<TriState> isFreeReference,
                                                                BlockState blockState, SFVec3i absolutePositionBlock) {
      // Towering requires placing a block below
      if (!graph.canPlaceBlocks()) {
        return MinecraftGraph.SubscriptionSingleResult.IMPOSSIBLE;
      }

      return switch (type) {
        case MOVEMENT_FREE -> {
          if (isFreeReference.value == TriState.NOT_SET) {
            // We can walk through blocks like air or grass
            isFreeReference.value = MinecraftGraph.isBlockFree(blockState);
          }

          if (isFreeReference.value == TriState.TRUE) {
            upMovement.noNeedToBreak()[blockArrayIndex] = true;
            yield MinecraftGraph.SubscriptionSingleResult.CONTINUE;
          }

          // Search for a way to break this block
          if (!graph.canBreakBlocks()
            || !BlockTypeHelper.isDiggable(blockState.blockType())
            || upMovement.unsafeToBreak()[blockArrayIndex]
            || !BlockItems.hasItemType(blockState.blockType())) {
            // No way to break this block
            yield MinecraftGraph.SubscriptionSingleResult.IMPOSSIBLE;
          }

          var cacheableMiningCost = graph.inventory().getMiningCosts(graph.tagsState(), blockState);
          // We can mine this block, lets add costs and continue
          upMovement.blockBreakCosts()[blockArrayIndex] =
            new MovementMiningCost(
              absolutePositionBlock,
              cacheableMiningCost.miningCost(),
              cacheableMiningCost.willDrop(),
              blockBreakSideHint);
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
        default -> throw new IllegalStateException("Unexpected value: " + type);
      };
    }

    @Override
    public UpMovement castAction(GraphAction action) {
      return (UpMovement) action;
    }
  }
}
