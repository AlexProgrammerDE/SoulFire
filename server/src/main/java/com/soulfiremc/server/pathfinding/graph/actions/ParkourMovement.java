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
import com.soulfiremc.server.pathfinding.execution.GapJumpAction;
import com.soulfiremc.server.pathfinding.graph.BlockFace;
import com.soulfiremc.server.pathfinding.graph.GraphInstructions;
import com.soulfiremc.server.pathfinding.graph.MinecraftGraph;
import com.soulfiremc.server.pathfinding.graph.actions.movement.BlockSafetyData;
import com.soulfiremc.server.pathfinding.graph.actions.movement.ParkourDirection;
import com.soulfiremc.server.protocol.bot.BotActionManager;
import com.soulfiremc.server.util.BlockTypeHelper;
import com.soulfiremc.server.util.ObjectReference;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.kyori.adventure.util.TriState;

public final class ParkourMovement extends GraphAction implements Cloneable {
  private static final SFVec3i FEET_POSITION_RELATIVE_BLOCK = SFVec3i.ZERO;
  private final ParkourDirection direction;
  private final SFVec3i targetFeetBlock;

  public ParkourMovement(ParkourDirection direction) {
    this.direction = direction;
    this.targetFeetBlock = direction.offset(direction.offset(FEET_POSITION_RELATIVE_BLOCK));
  }

  public static void registerParkourMovements(
    Consumer<GraphAction> callback,
    BiConsumer<SFVec3i, MinecraftGraph.MovementSubscription<?>> blockSubscribers) {
    for (var direction : ParkourDirection.VALUES) {
      callback.accept(
        ParkourMovement.registerParkourMovement(
          blockSubscribers, new ParkourMovement(direction)));
    }
  }

  private static ParkourMovement registerParkourMovement(
    BiConsumer<SFVec3i, MinecraftGraph.MovementSubscription<?>> blockSubscribers,
    ParkourMovement movement) {
    {
      var blockId = 0;
      for (var freeBlock : movement.listRequiredFreeBlocks()) {
        movement.subscribe();
        blockSubscribers
          .accept(freeBlock.key(), new ParkourMovementBlockSubscription(MinecraftGraph.SubscriptionType.MOVEMENT_FREE, blockId++, freeBlock.value()));
      }
    }

    {
      movement.subscribe();
      blockSubscribers
        .accept(movement.requiredUnsafeBlock(), new ParkourMovementBlockSubscription(MinecraftGraph.SubscriptionType.PARKOUR_UNSAFE_TO_STAND_ON));
    }

    {
      movement.subscribe();
      blockSubscribers
        .accept(movement.requiredSolidBlock(), new ParkourMovementBlockSubscription(MinecraftGraph.SubscriptionType.MOVEMENT_SOLID));
    }

    return movement;
  }

  public List<Pair<SFVec3i, BlockFace>> listRequiredFreeBlocks() {
    var requiredFreeBlocks = new ObjectArrayList<Pair<SFVec3i, BlockFace>>();

    // Make block above the head block free for jump
    requiredFreeBlocks.add(Pair.of(FEET_POSITION_RELATIVE_BLOCK.add(0, 2, 0), BlockFace.BOTTOM));

    var oneFurther = direction.offset(FEET_POSITION_RELATIVE_BLOCK);
    var blockDigDirection = direction.toSkyDirection().opposite().toBlockFace();

    // Room for jumping
    requiredFreeBlocks.add(Pair.of(oneFurther, blockDigDirection));
    requiredFreeBlocks.add(Pair.of(oneFurther.add(0, 1, 0), blockDigDirection));
    requiredFreeBlocks.add(Pair.of(oneFurther.add(0, 2, 0), blockDigDirection));

    var twoFurther = direction.offset(oneFurther);

    // Room for jumping
    requiredFreeBlocks.add(Pair.of(twoFurther, blockDigDirection));
    requiredFreeBlocks.add(Pair.of(twoFurther.add(0, 1, 0), blockDigDirection));
    requiredFreeBlocks.add(Pair.of(twoFurther.add(0, 2, 0), blockDigDirection));

    return requiredFreeBlocks;
  }

  public SFVec3i requiredUnsafeBlock() {
    // The gap to jump over, needs to be unsafe for this movement to be possible
    return direction.offset(FEET_POSITION_RELATIVE_BLOCK).sub(0, 1, 0);
  }

  public SFVec3i requiredSolidBlock() {
    // Floor block
    return targetFeetBlock.sub(0, 1, 0);
  }

  @Override
  public List<GraphInstructions> getInstructions(SFVec3i node) {
    var absoluteTargetFeetBlock = node.add(targetFeetBlock);

    return Collections.singletonList(new GraphInstructions(
      absoluteTargetFeetBlock,
      Costs.ONE_GAP_JUMP,
      List.of(new GapJumpAction(absoluteTargetFeetBlock))));
  }

  @Override
  public ParkourMovement copy() {
    return this.clone();
  }

  @Override
  public ParkourMovement clone() {
    try {
      return (ParkourMovement) super.clone();
    } catch (CloneNotSupportedException cantHappen) {
      throw new InternalError();
    }
  }

  public record ParkourMovementBlockSubscription(
    MinecraftGraph.SubscriptionType type,
    int blockArrayIndex,
    BlockFace blockBreakSideHint,
    BotActionManager.BlockPlaceAgainstData blockToPlaceAgainst,
    BlockSafetyData.BlockSafetyType safetyType) implements MinecraftGraph.MovementSubscription<ParkourMovement> {
    public ParkourMovementBlockSubscription(MinecraftGraph.SubscriptionType type) {
      this(type, -1, null, null, null);
    }

    public ParkourMovementBlockSubscription(MinecraftGraph.SubscriptionType type, int blockArrayIndex, BlockFace blockBreakSideHint) {
      this(type, blockArrayIndex, blockBreakSideHint, null, null);
    }

    @Override
    public MinecraftGraph.SubscriptionSingleResult processBlock(MinecraftGraph graph, SFVec3i key, ParkourMovement parkourMovement, ObjectReference<TriState> isFreeReference,
                                                                BlockState blockState, SFVec3i absolutePositionBlock) {

      return switch (type) {
        case MOVEMENT_FREE -> {
          if (isFreeReference.value == TriState.NOT_SET) {
            // We can walk through blocks like air or grass
            isFreeReference.value = MinecraftGraph.isBlockFree(blockState);
          }

          if (isFreeReference.value == TriState.TRUE) {
            yield MinecraftGraph.SubscriptionSingleResult.CONTINUE;
          }

          yield MinecraftGraph.SubscriptionSingleResult.IMPOSSIBLE;
        }
        case PARKOUR_UNSAFE_TO_STAND_ON -> {
          // We only want to jump over dangerous blocks/gaps
          // So either a non-full-block like water or lava or magma
          // since it hurts to stand on.
          if (BlockTypeHelper.isSafeBlockToStandOn(blockState)) {
            yield MinecraftGraph.SubscriptionSingleResult.IMPOSSIBLE;
          }

          yield MinecraftGraph.SubscriptionSingleResult.CONTINUE;
        }
        case MOVEMENT_SOLID -> {
          // Block is safe to walk on, no need to check for more
          if (BlockTypeHelper.isSafeBlockToStandOn(blockState)) {
            yield MinecraftGraph.SubscriptionSingleResult.CONTINUE;
          }

          yield MinecraftGraph.SubscriptionSingleResult.IMPOSSIBLE;
        }
        default -> throw new IllegalStateException("Unexpected value: " + type);
      };
    }

    @Override
    public ParkourMovement castAction(GraphAction action) {
      return (ParkourMovement) action;
    }
  }
}
