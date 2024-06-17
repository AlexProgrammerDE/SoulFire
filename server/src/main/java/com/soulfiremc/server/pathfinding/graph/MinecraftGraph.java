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
import com.soulfiremc.server.pathfinding.NodeState;
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.pathfinding.graph.actions.DownMovement;
import com.soulfiremc.server.pathfinding.graph.actions.GraphAction;
import com.soulfiremc.server.pathfinding.graph.actions.ParkourMovement;
import com.soulfiremc.server.pathfinding.graph.actions.SimpleMovement;
import com.soulfiremc.server.pathfinding.graph.actions.UpMovement;
import com.soulfiremc.server.protocol.bot.state.TagsState;
import com.soulfiremc.server.util.BlockTypeHelper;
import com.soulfiremc.server.util.LazyBoolean;
import com.soulfiremc.server.util.Vec2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectFunction;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record MinecraftGraph(TagsState tagsState,
                             ProjectedLevel level, ProjectedInventory inventory,
                             Predicate<SFVec3i> canBreakBlockPredicate,
                             Predicate<SFVec3i> canPlaceBlockPredicate) {
  private static final Object2ObjectFunction<
    ? super SFVec3i, ? extends ObjectList<WrappedActionSubscription>>
    CREATE_MISSING_FUNCTION = k -> new ObjectArrayList<>();
  private static final GraphAction[] ACTIONS_TEMPLATE;
  private static final SFVec3i[] SUBSCRIPTION_KEYS;
  private static final WrappedActionSubscription[][] SUBSCRIPTION_VALUES;
  private static final boolean ALLOW_BREAKING_UNDIGGABLE = Boolean.getBoolean("sf.pathfinding-allow-breaking-undiggable");

  static {
    var blockSubscribers = new Vec2ObjectOpenHashMap<SFVec3i, ObjectList<WrappedActionSubscription>>();
    var actions = new ObjectArrayList<GraphAction>();
    var currentSubscriptions = new AtomicInteger(0);
    BiConsumer<SFVec3i, MovementSubscription<?>> blockSubscribersConsumer = (key, value) -> {
      currentSubscriptions.incrementAndGet();
      blockSubscribers.computeIfAbsent(key, CREATE_MISSING_FUNCTION).add(new WrappedActionSubscription(actions.size(), value));
    };
    Consumer<GraphAction> actionAdder = action -> {
      actions.add(action);
      action.subscriptionCounter(currentSubscriptions.getAndSet(0));
    };

    SimpleMovement.registerMovements(actionAdder, blockSubscribersConsumer);
    ParkourMovement.registerParkourMovements(actionAdder, blockSubscribersConsumer);
    DownMovement.registerDownMovements(actionAdder, blockSubscribersConsumer);
    UpMovement.registerUpMovements(actionAdder, blockSubscribersConsumer);

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

  public MinecraftGraph(TagsState tagsState,
                        ProjectedLevel level, ProjectedInventory inventory,
                        boolean canBreakBlocks, boolean canPlaceBlocks) {
    this(tagsState, level, inventory, v -> canBreakBlocks, v -> {
      if (!canPlaceBlocks) {
        return false;
      }

      return level.isPlaceable(v);
    });
  }

  public boolean doUsableBlocksDecreaseWhenPlaced() {
    return !inventory.creativeModeBreak();
  }

  public static boolean isBlockFree(BlockState blockState) {
    return blockState.blockShapeGroup().hasNoCollisions() && blockState.blockType().fluidType() == FluidType.EMPTY;
  }

  public boolean disallowedToPlaceBlock(SFVec3i position) {
    return !canPlaceBlockPredicate.test(position);
  }

  public boolean disallowedToBreakBlock(SFVec3i position) {
    return !canBreakBlockPredicate.test(position);
  }

  public boolean disallowedToBreakType(BlockType blockType) {
    if (!ALLOW_BREAKING_UNDIGGABLE) {
      return !BlockTypeHelper.isDiggable(blockType);
    }

    return false;
  }

  public void insertActions(NodeState node, Consumer<GraphInstructions> callback) {
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
    NodeState node,
    GraphAction[] actions,
    Consumer<GraphInstructions> callback) {
    for (var i = 0; i < SUBSCRIPTION_KEYS.length; i++) {
      processSubscription(node, actions, callback, i);
    }
  }

  private void processSubscription(
    NodeState node, GraphAction[] actions, Consumer<GraphInstructions> callback, int i) {
    var key = SUBSCRIPTION_KEYS[i];
    var value = SUBSCRIPTION_VALUES[i];

    BlockState blockState = null;
    SFVec3i absolutePositionBlock = null;

    // We cache only this, but not solid because solid will only occur a single time
    LazyBoolean isFree = null;
    for (var subscriber : value) {
      var action = actions[subscriber.actionIndex];
      if (action == null) {
        continue;
      }

      if (blockState == null) {
        // Lazy calculation to avoid unnecessary calls
        absolutePositionBlock = node.blockPosition().add(key);
        blockState = level.getBlockState(absolutePositionBlock);

        if (blockState.blockType() == BlockType.VOID_AIR) {
          throw new OutOfLevelException();
        }
      }

      if (isFree == null) {
        var finalBlockState = blockState;
        isFree = new LazyBoolean(() -> isBlockFree(finalBlockState));
      }

      switch (subscriber.subscription.processBlockUnsafe(this, key, action, isFree, blockState, absolutePositionBlock)) {
        case CONTINUE -> {
          if (!action.decrementAndIsDone()) {
            continue;
          }

          for (var instruction : action.getInstructions(this, node)) {
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

  public interface MovementSubscription<M extends GraphAction> {
    SubscriptionSingleResult processBlock(
      MinecraftGraph graph,
      SFVec3i key,
      M action,
      LazyBoolean isFree,
      BlockState blockState,
      SFVec3i absoluteKey);

    default SubscriptionSingleResult processBlockUnsafe(
      MinecraftGraph graph,
      SFVec3i key,
      GraphAction action,
      LazyBoolean isFree,
      BlockState blockState,
      SFVec3i absolutePositionBlock) {
      return processBlock(graph, key, castAction(action), isFree, blockState, absolutePositionBlock);
    }

    M castAction(GraphAction action);
  }

  private record WrappedActionSubscription(int actionIndex, MovementSubscription<?> subscription) {}
}
