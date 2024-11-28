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
import com.soulfiremc.server.pathfinding.execution.GapJumpAction;
import com.soulfiremc.server.pathfinding.graph.BlockFace;
import com.soulfiremc.server.pathfinding.graph.GraphInstructions;
import com.soulfiremc.server.pathfinding.graph.MinecraftGraph;
import com.soulfiremc.server.pathfinding.graph.actions.movement.ParkourDirection;
import com.soulfiremc.server.util.SFBlockHelpers;
import com.soulfiremc.server.util.structs.LazyBoolean;
import it.unimi.dsi.fastutil.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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
      for (var freeBlock : movement.listRequiredFreeBlocks()) {
        blockSubscribers
          .accept(freeBlock.key(), new MovementFreeSubscription());
      }
    }

    {
      blockSubscribers
        .accept(movement.requiredUnsafeBlock(), new ParkourUnsafeToStandOnSubscription());
    }

    {
      blockSubscribers
        .accept(movement.requiredSolidBlock(), new MovementSolidSubscription());
    }

    return movement;
  }

  public List<Pair<SFVec3i, BlockFace>> listRequiredFreeBlocks() {
    var requiredFreeBlocks = new ArrayList<Pair<SFVec3i, BlockFace>>();

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
  public List<GraphInstructions> getInstructions(MinecraftGraph graph, NodeState node) {
    var absoluteTargetFeetBlock = node.blockPosition().add(targetFeetBlock);

    return Collections.singletonList(new GraphInstructions(
      new NodeState(absoluteTargetFeetBlock, node.usableBlockItems()),
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

  interface ParkourMovementSubscription extends MinecraftGraph.MovementSubscription<ParkourMovement> {
    @Override
    default ParkourMovement castAction(GraphAction action) {
      return (ParkourMovement) action;
    }
  }

  record MovementFreeSubscription() implements ParkourMovementSubscription {
    @Override
    public MinecraftGraph.SubscriptionSingleResult processBlock(MinecraftGraph graph, SFVec3i key, ParkourMovement parkourMovement, LazyBoolean isFree,
                                                                BlockState blockState, SFVec3i absoluteKey) {
      if (isFree.get()) {
        return MinecraftGraph.SubscriptionSingleResult.CONTINUE;
      }

      return MinecraftGraph.SubscriptionSingleResult.IMPOSSIBLE;
    }
  }

  record ParkourUnsafeToStandOnSubscription() implements ParkourMovementSubscription {
    @Override
    public MinecraftGraph.SubscriptionSingleResult processBlock(MinecraftGraph graph, SFVec3i key, ParkourMovement parkourMovement, LazyBoolean isFree,
                                                                BlockState blockState, SFVec3i absoluteKey) {
      // We only want to jump over dangerous blocks/gaps
      // So either a non-full-block like water or lava or magma
      // since it hurts to stand on.
      if (SFBlockHelpers.isSafeBlockToStandOn(blockState)) {
        return MinecraftGraph.SubscriptionSingleResult.IMPOSSIBLE;
      }

      return MinecraftGraph.SubscriptionSingleResult.CONTINUE;
    }
  }

  record MovementSolidSubscription() implements ParkourMovementSubscription {
    @Override
    public MinecraftGraph.SubscriptionSingleResult processBlock(MinecraftGraph graph, SFVec3i key, ParkourMovement parkourMovement, LazyBoolean isFree,
                                                                BlockState blockState, SFVec3i absoluteKey) {
      // Block is safe to walk on, no need to check for more
      if (SFBlockHelpers.isSafeBlockToStandOn(blockState)) {
        return MinecraftGraph.SubscriptionSingleResult.CONTINUE;
      }

      return MinecraftGraph.SubscriptionSingleResult.IMPOSSIBLE;
    }
  }
}
