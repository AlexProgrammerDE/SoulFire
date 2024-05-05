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
package com.soulfiremc.server.pathfinding.graph;

import com.soulfiremc.server.data.BlockState;
import com.soulfiremc.server.data.BlockType;
import com.soulfiremc.server.data.FluidType;
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.pathfinding.graph.actions.DownMovement;
import com.soulfiremc.server.pathfinding.graph.actions.GraphAction;
import com.soulfiremc.server.pathfinding.graph.actions.ParkourMovement;
import com.soulfiremc.server.pathfinding.graph.actions.SimpleMovement;
import com.soulfiremc.server.pathfinding.graph.actions.UpMovement;
import com.soulfiremc.server.protocol.bot.state.TagsState;
import com.soulfiremc.server.util.ObjectReference;
import com.soulfiremc.server.util.Vec2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectFunction;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.util.TriState;

@Slf4j
public record MinecraftGraph(TagsState tagsState,
                             ProjectedLevel level, ProjectedInventory inventory,
                             boolean canBreakBlocks, boolean canPlaceBlocks) {
  private static final Object2ObjectFunction<
    ? super SFVec3i, ? extends ObjectList<WrappedActionSubscription>>
    CREATE_MISSING_FUNCTION = k -> new ObjectArrayList<>();
  private static final GraphAction[] ACTIONS_TEMPLATE;
  private static final SFVec3i[] SUBSCRIPTION_KEYS;
  private static final WrappedActionSubscription[][] SUBSCRIPTION_VALUES;

  static {
    var blockSubscribers = new Vec2ObjectOpenHashMap<SFVec3i, ObjectList<WrappedActionSubscription>>();
    var actions = new ObjectArrayList<GraphAction>();
    BiConsumer<SFVec3i, MovementSubscription<?>> blockSubscribersConsumer = (key, value) ->
      blockSubscribers.computeIfAbsent(key, CREATE_MISSING_FUNCTION).add(new WrappedActionSubscription(actions.size(), value));

    SimpleMovement.registerMovements(actions::add, blockSubscribersConsumer);
    ParkourMovement.registerParkourMovements(actions::add, blockSubscribersConsumer);
    DownMovement.registerDownMovements(actions::add, blockSubscribersConsumer);
    UpMovement.registerUpMovements(actions::add, blockSubscribersConsumer);

    ACTIONS_TEMPLATE = actions.toArray(new GraphAction[0]);
    SUBSCRIPTION_KEYS = new SFVec3i[blockSubscribers.size()];
    SUBSCRIPTION_VALUES = new WrappedActionSubscription[blockSubscribers.size()][];

    var entrySetDescending =
      blockSubscribers.object2ObjectEntrySet().stream()
        .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
        .toList();
    for (var i = 0; i < entrySetDescending.size(); i++) {
      var entry = entrySetDescending.get(i);
      SUBSCRIPTION_KEYS[i] = entry.getKey();
      SUBSCRIPTION_VALUES[i] = entry.getValue().toArray(new WrappedActionSubscription[0]);
    }
  }

  public static TriState isBlockFree(BlockState blockState) {
    return TriState.byBoolean(
      blockState.blockShapeGroup().hasNoCollisions() && blockState.blockType().fluidType() == FluidType.EMPTY);
  }

  public void insertActions(
    SFVec3i node, Consumer<GraphInstructions> callback) {
    log.debug("Inserting actions for node: {}", node);
    calculateActions(node, generateTemplateActions(), callback);
  }

  private GraphAction[] generateTemplateActions() {
    var actions = new GraphAction[ACTIONS_TEMPLATE.length];
    for (var i = 0; i < ACTIONS_TEMPLATE.length; i++) {
      actions[i] = ACTIONS_TEMPLATE[i].copy();
    }

    return actions;
  }

  private void calculateActions(
    SFVec3i node,
    GraphAction[] actions,
    Consumer<GraphInstructions> callback) {
    for (var i = 0; i < SUBSCRIPTION_KEYS.length; i++) {
      processSubscription(node, actions, callback, i);
    }
  }

  private void processSubscription(
    SFVec3i node, GraphAction[] actions, Consumer<GraphInstructions> callback, int i) {
    var key = SUBSCRIPTION_KEYS[i];
    var value = SUBSCRIPTION_VALUES[i];

    BlockState blockState = null;
    SFVec3i absolutePositionBlock = null;

    // We cache only this, but not solid because solid will only occur a single time
    var isFreeReference = new ObjectReference<>(TriState.NOT_SET);
    for (var subscriber : value) {
      var action = actions[subscriber.actionIndex];
      if (action == null) {
        continue;
      }

      if (blockState == null) {
        // Lazy calculation to avoid unnecessary calls
        absolutePositionBlock = node.add(key);
        blockState = level.getBlockState(absolutePositionBlock);

        if (blockState.blockType() == BlockType.VOID_AIR) {
          throw new OutOfLevelException();
        }
      }

      switch (subscriber.subscription.processBlockUnsafe(this, key, action, isFreeReference, blockState, absolutePositionBlock)) {
        case CONTINUE -> {
          if (!action.decrementAndIsDone()) {
            continue;
          }

          for (var instruction : action.getInstructions(node)) {
            callback.accept(instruction);
          }
        }
        case IMPOSSIBLE -> actions[subscriber.actionIndex] = null;
      }
    }
  }

  public enum SubscriptionSingleResult {
    CONTINUE,
    IMPOSSIBLE
  }

  public enum SubscriptionType {
    MOVEMENT_FREE,
    MOVEMENT_BREAK_SAFETY_CHECK,
    MOVEMENT_SOLID,
    MOVEMENT_ADD_CORNER_COST_IF_SOLID,
    MOVEMENT_AGAINST_PLACE_SOLID,
    DOWN_SAFETY_CHECK,
    PARKOUR_UNSAFE_TO_STAND_ON
  }

  public interface MovementSubscription<M extends GraphAction> {
    SubscriptionSingleResult processBlock(
      MinecraftGraph graph,
      SFVec3i key,
      M action,
      ObjectReference<TriState> isFreeReference,
      BlockState blockState,
      SFVec3i absolutePositionBlock);

    default SubscriptionSingleResult processBlockUnsafe(
      MinecraftGraph graph,
      SFVec3i key,
      GraphAction action,
      ObjectReference<TriState> isFreeReference,
      BlockState blockState,
      SFVec3i absolutePositionBlock) {
      return processBlock(graph, key, castAction(action), isFreeReference, blockState, absolutePositionBlock);
    }

    M castAction(GraphAction action);
  }

  private record WrappedActionSubscription(int actionIndex, MovementSubscription<?> subscription) {}
}
