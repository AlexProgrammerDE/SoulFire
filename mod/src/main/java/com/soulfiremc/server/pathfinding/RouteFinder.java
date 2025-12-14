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
package com.soulfiremc.server.pathfinding;

import com.google.common.base.Stopwatch;
import com.soulfiremc.server.pathfinding.execution.MovementAction;
import com.soulfiremc.server.pathfinding.execution.RecalculatePathAction;
import com.soulfiremc.server.pathfinding.execution.WorldAction;
import com.soulfiremc.server.pathfinding.goals.GoalScorer;
import com.soulfiremc.server.pathfinding.graph.GraphInstructions;
import com.soulfiremc.server.pathfinding.graph.MinecraftGraph;
import com.soulfiremc.server.pathfinding.graph.OutOfLevelException;
import com.soulfiremc.server.util.structs.CallLimiter;
import com.soulfiremc.server.util.structs.IntReference;
import com.soulfiremc.server.util.structs.Long2ObjectLRUCache;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public record RouteFinder(MinecraftGraph graph, GoalScorer scorer) {
  /// Maximum possible usable block items (4 rows * 9 columns * 64 stack size)
  private static final int MAX_USABLE_BLOCK_ITEMS = 4 * 9 * 64;

  private static List<WorldAction> reconstructPath(MinecraftRouteNode current) {
    var actions = new ArrayList<WorldAction>();

    var currentElement = current;
    do {
      var previousActions = new ArrayList<>(currentElement.actions());

      // Insert the actions in reversed order
      for (var i = previousActions.size() - 1; i >= 0; i--) {
        actions.addFirst(previousActions.get(i));
      }

      currentElement = currentElement.parent();
    } while (currentElement != null);

    return actions;
  }

  private static MinecraftRouteNode findBestNode(Long2ObjectOpenHashMap<MinecraftRouteNode>[] routeIndex) {
    MinecraftRouteNode bestNode = null;
    var smallestScore = Double.MAX_VALUE;

    for (var map : routeIndex) {
      if (map == null) {
        continue;
      }

      for (var node : map.values()) {
        // Our implementation calculates the score from this node to the start,
        // so we need to get it by subtracting the source cost
        var targetScore = node.totalRouteScore() - node.sourceCost();

        if (targetScore < smallestScore) {
          smallestScore = targetScore;
          bestNode = node;
        }
      }
    }

    return Objects.requireNonNull(bestNode, "No best node found");
  }

  private static Long2ObjectOpenHashMap<MinecraftRouteNode> getRouteMap(
    Long2ObjectOpenHashMap<MinecraftRouteNode>[] routeIndex, int usableBlockItems) {
    var map = routeIndex[usableBlockItems];
    if (map == null) {
      map = new Long2ObjectOpenHashMap<>();
      routeIndex[usableBlockItems] = map;
    }
    return map;
  }

  private static void clearRouteIndex(Long2ObjectOpenHashMap<MinecraftRouteNode>[] routeIndex) {
    Arrays.fill(routeIndex, null);
  }

  public CompletableFuture<List<WorldAction>> findRouteFuture(NodeState from, boolean requiresRepositioning) {
    return CompletableFuture.supplyAsync(() -> repositionIfNeeded(findRouteSync(from), from, requiresRepositioning));
  }

  @VisibleForTesting
  public List<WorldAction> findRouteSync(NodeState from) {
    var stopwatch = Stopwatch.createStarted();
    var expireTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(Integer.getInteger("sf.pathfinding-expire", 180));

    // Store block positions and the best route to them
    var blockItemsIndex = new Long2IntOpenHashMap();
    var instructionCache = new Long2ObjectLRUCache<GraphInstructions[]>(50_000);
    // Array of maps indexed by usable block items count for O(1) lookup without hash collisions
    // Uses Long2ObjectOpenHashMap with SFVec3i.asMinecraftLong() as key for optimal performance
    @SuppressWarnings("unchecked")
    var routeIndex = (Long2ObjectOpenHashMap<MinecraftRouteNode>[])
      new Long2ObjectOpenHashMap[MAX_USABLE_BLOCK_ITEMS + 1];

    // Store block positions that we need to look at
    var openSet = new ObjectHeapPriorityQueue<MinecraftRouteNode>();

    {
      var startScore = scorer.computeScore(graph, from.blockPosition(), List.of());
      log.debug("Start score (Usually distance): {}", startScore);

      var start =
        new MinecraftRouteNode(
          from,
          List.of(),
          0,
          startScore,
          startScore);
      getRouteMap(routeIndex, from.usableBlockItems()).put(from.blockPosition().asMinecraftLong(), start);
      openSet.enqueue(start);
    }

    var progressInfo = new CallLimiter(() -> {
      if (!log.isInfoEnabled()) {
        return;
      }

      var bestNode = findBestNode(routeIndex);
      log.info("Still looking for route... {}ms time left, {} nodes left, closest position is {} with distance {}",
        expireTime - System.currentTimeMillis(),
        openSet.size(),
        bestNode.node().blockPosition().formatXYZ(),
        bestNode.totalRouteScore() - bestNode.sourceCost()
      );
    }, 1, TimeUnit.SECONDS, true);
    var cleaner = new CallLimiter(() -> {
      if (Boolean.getBoolean("sf.pathfinding-no-prune")) {
        return;
      }

      log.info("Pruning route index and open set to avoid branching");

      var bestNode = findBestNode(routeIndex);
      openSet.clear();
      openSet.enqueue(bestNode);
      clearRouteIndex(routeIndex);
    }, 5, TimeUnit.SECONDS, true);
    while (!openSet.isEmpty()) {
      if (Thread.currentThread().isInterrupted()) {
        stopwatch.stop();
        log.info("Cancelled pathfinding after {}ms", stopwatch.elapsed().toMillis());
        return List.of();
      } else if (System.currentTimeMillis() > expireTime) {
        stopwatch.stop();
        log.info("Expired pathfinding after {}ms", stopwatch.elapsed().toMillis());
        throw new IllegalStateException("Pathfinding took too long");
      }

      progressInfo.run();
      cleaner.run();

      var current = openSet.dequeue();

      log.debug("Looking at node: {}", current.node());

      // If we found our destination, we can stop looking
      if (scorer.isFinished(current)) {
        stopwatch.stop();
        log.info("Success! Took {}ms to find route", stopwatch.elapsed().toMillis());

        return reconstructPath(current);
      }

      try {
        instructionCache.compute(current.node().blockPosition().asMinecraftLong(), (_, v) -> {
          if (v == null) {
            var counter = new IntReference();
            var list = new GraphInstructions[MinecraftGraph.ACTIONS_SIZE];
            graph.insertActions(
              current.node().blockPosition(),
              current.parentToNodeDirection(),
              instructions -> {
                list[counter.value++] = instructions;
                handleInstructions(openSet, routeIndex, blockItemsIndex, current, instructions);
              }
            );

            return list;
          }

          for (var instructions : v) {
            if (instructions == null) {
              break;
            }

            handleInstructions(openSet, routeIndex, blockItemsIndex, current, instructions);
          }

          return v;
        });
      } catch (OutOfLevelException _) {
        log.debug("Found a node out of the level: {}", current.node());
        stopwatch.stop();
        log.info(
          "Took {}ms to find route to reach the edge of view distance",
          stopwatch.elapsed().toMillis());

        // The current node is not always the best node. We need to find the best node.
        var bestNode = findBestNode(routeIndex);

        // This is the best node we found so far
        // We will add a recalculating action and return the best route
        var recalculateTrace = reconstructPath(bestNode);
        if (recalculateTrace.isEmpty()) {
          throw new AlreadyClosestException();
        }

        return addRecalculate(recalculateTrace);
      }
    }

    stopwatch.stop();
    log.info("Failed to find route after {}ms", stopwatch.elapsed().toMillis());
    throw new NoRouteFoundException();
  }

  private void handleInstructions(ObjectHeapPriorityQueue<MinecraftRouteNode> openSet,
                                  Long2ObjectOpenHashMap<MinecraftRouteNode>[] routeIndex,
                                  Long2IntMap blockItemsIndex,
                                  MinecraftRouteNode current,
                                  GraphInstructions instructions) {
    // Creative mode placing requires us to have at least one block
    if (instructions.requiresOneBlock() && current.node().usableBlockItems() < 1) {
      return;
    }

    var newBlocks = current.node().usableBlockItems() + instructions.deltaUsableBlockItems();

    // If we don't have enough items to reach this node, we can skip it
    if (newBlocks < 0) {
      return;
    }

    var blockPosition = instructions.blockPosition();
    var blockPositionLong = blockPosition.asMinecraftLong();

    // Pre-check if we can reach this node with the current amount of items
    // We don't want to consider nodes again where we have even less usable items
    var bestUsableItems = blockItemsIndex.compute(blockPositionLong, (_, v) -> {
      if (v == null || newBlocks > v) {
        return newBlocks;
      }

      return v;
    });
    if (bestUsableItems > newBlocks) {
      return;
    }

    var actionCost = instructions.actionCost();
    var worldActions = instructions.actions();

    // Calculate new distance from start to this connection,
    // Get distance from the current element
    // and add the distance from the current element to the next element
    var newSourceCost = current.sourceCost() + actionCost;
    var newTargetCost = scorer.computeScore(graph, blockPosition, worldActions);
    var newTotalRouteScore = newSourceCost + newTargetCost;

    var routeMap = getRouteMap(routeIndex, newBlocks);
    routeMap.compute(
      blockPositionLong,
      (_, v) -> {
        // The first time we see this node
        if (v == null) {
          var instructionNode = new NodeState(blockPosition, newBlocks);
          var node =
            new MinecraftRouteNode(
              instructionNode,
              current,
              instructions.moveDirection(),
              worldActions,
              newSourceCost,
              newTargetCost,
              newTotalRouteScore);

          log.debug("Found a new node: {}", instructionNode);

          openSet.enqueue(node);

          return node;
        }

        // If we found a better route to this node, update it
        if (newSourceCost < v.sourceCost()) {
          v.setBetterParent(
            current,
            instructions.moveDirection(),
            worldActions,
            newSourceCost,
            newTargetCost,
            newTotalRouteScore);

          log.debug("Found a better route to node: {}", v.node());

          openSet.enqueue(v);
        }

        return v;
      });
  }

  private List<WorldAction> repositionIfNeeded(List<WorldAction> actions, NodeState from, boolean requiresRepositioning) {
    if (!requiresRepositioning) {
      return actions;
    }

    var repositionActions = new ArrayList<WorldAction>();
    repositionActions.add(new MovementAction(from.blockPosition(), false));
    repositionActions.addAll(actions);

    return repositionActions;
  }

  private List<WorldAction> addRecalculate(List<WorldAction> actions) {
    var repositionActions = new ArrayList<>(actions);
    repositionActions.add(new RecalculatePathAction());

    return repositionActions;
  }
}
