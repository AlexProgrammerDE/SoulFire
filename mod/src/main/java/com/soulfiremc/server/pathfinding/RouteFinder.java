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
import com.soulfiremc.server.pathfinding.execution.WorldAction;
import com.soulfiremc.server.pathfinding.goals.GoalScorer;
import com.soulfiremc.server.pathfinding.graph.GraphInstructions;
import com.soulfiremc.server.pathfinding.graph.MinecraftGraph;
import com.soulfiremc.server.pathfinding.graph.OutOfLevelException;
import com.soulfiremc.server.util.structs.*;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public record RouteFinder(MinecraftGraph baseGraph, GoalScorer scorer) {
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

  private static Long2ObjectOpenHashMap<MinecraftRouteNode> getRouteMap(
    Long2ObjectOpenHashMap<MinecraftRouteNode>[] routeIndex, int usableBlockItems) {
    var map = routeIndex[usableBlockItems];
    if (map == null) {
      map = new Long2ObjectOpenHashMap<>();
      routeIndex[usableBlockItems] = map;
    }
    return map;
  }

  public CompletableFuture<RouteSearchResult> findRouteFuture(NodeState from) {
    var stopwatch = Stopwatch.createStarted();
    var futures = new ArrayList<CompletableFuture<RouteSearchResult>>();
    var cancellationToken = new CancellationToken();

    futures.add(findRouteFutureSingle(baseGraph, from, cancellationToken));
//    var pathConstraint = baseGraph.pathConstraint();
//    if (pathConstraint.canBreakBlocks() && pathConstraint.canPlaceBlocks()) {
//      futures.add(findRouteFutureSingle(baseGraph.withPathConstraint(
//        new NoBlockActionsConstraint(pathConstraint)
//      ), from, cancellationToken));
//    }
//
//    if (pathConstraint.canBreakBlocks()) {
//      futures.add(findRouteFutureSingle(baseGraph.withPathConstraint(
//        new NoBlockBreakingConstraint(pathConstraint)
//      ), from, cancellationToken));
//    }
//
//    if (pathConstraint.canPlaceBlocks()) {
//      futures.add(findRouteFutureSingle(baseGraph.withPathConstraint(
//        new NoBlockPlacingConstraint(pathConstraint)
//      ), from, cancellationToken));
//    }

    var resultFuture = new CompletableFuture<RouteSearchResult>();
    var countdown = new CountDownLatch(futures.size());
    for (var future : futures) {
      future.thenAccept(result -> {
          if (result instanceof FoundRouteResult || result instanceof PartialRouteResult) {
            if (!resultFuture.isDone()) {
              resultFuture.complete(result);
            }
            cancellationToken.cancel();
            for (var f : futures) {
              f.cancel(true);
            }
            stopwatch.stop();
            log.info("Best route found in {}ms", stopwatch.elapsed().toMillis());
          } else {
            countdown.countDown();
            if (countdown.getCount() == 0 && !resultFuture.isDone()) {
              resultFuture.complete(futures.getFirst().resultNow());
              stopwatch.stop();
              log.info("No route found after {}ms", stopwatch.elapsed().toMillis());
            }
          }
        })
        .exceptionally(ex -> {
          countdown.countDown();
          if (countdown.getCount() == 0 && !resultFuture.isDone()) {
            resultFuture.completeExceptionally(ex);
            stopwatch.stop();
            log.info("Route finding failed after {}ms", stopwatch.elapsed().toMillis());
          }
          return null;
        });
    }

    return resultFuture;
  }

  public CompletableFuture<RouteSearchResult> findRouteFutureSingle(MinecraftGraph graph, NodeState from, CancellationToken cancellationToken) {
    return CompletableFuture.supplyAsync(() -> findRouteSyncSingle(graph, from, cancellationToken));
  }

  private RouteSearchResult findRouteSyncSingle(MinecraftGraph graph, NodeState from, CancellationToken cancellationToken) {
    var stopwatch = Stopwatch.createStarted();
    var pathConstraint = graph.pathConstraint();
    var expireTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(pathConstraint.expireTimeout());

    // Store block positions and the best route to them
    var blockItemsIndex = new Long2IntOpenHashMap();
    blockItemsIndex.defaultReturnValue(-1); // -1 means not visited yet
    var instructionCache = new Long2ObjectLRUCache<GraphInstructions[]>(50_000);
    // Array of maps indexed by usable block items count for O(1) lookup without hash collisions
    // Uses Long2ObjectOpenHashMap with SFVec3i.asMinecraftLong() as key for optimal performance
    @SuppressWarnings("unchecked")
    var routeIndex = (Long2ObjectOpenHashMap<MinecraftRouteNode>[])
      new Long2ObjectOpenHashMap[MAX_USABLE_BLOCK_ITEMS + 1];

    // Store block positions that we need to look at
    var openSet = new ObjectHeapPriorityQueue<MinecraftRouteNode>();
    var bestGlobalNode = new ObjectReference<MinecraftRouteNode>();

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
      bestGlobalNode.value = start;
    }

    var progressInfo = new CallLimiter(() -> {
      if (!log.isInfoEnabled()) {
        return;
      }

      log.info("Still looking for route... {}ms time left, {} nodes left, closest position is {} with distance to target {}",
        expireTime - System.currentTimeMillis(),
        openSet.size(),
        bestGlobalNode.value.node().blockPosition().formatXYZ(),
        bestGlobalNode.value.targetCost()
      );
    }, 1, TimeUnit.SECONDS, true);
    var cleaner = new CallLimiter(() -> {
      if (pathConstraint.disablePruning()) {
        return;
      }

      log.info("Pruning route index and open set to avoid branching");

      openSet.clear();
      openSet.enqueue(bestGlobalNode.value);
      Arrays.fill(routeIndex, null);
    }, 5, TimeUnit.SECONDS, true);
    while (!openSet.isEmpty()) {
      if (Thread.currentThread().isInterrupted() || cancellationToken.isCancelled()) {
        stopwatch.stop();
        log.info("Cancelled pathfinding after {}ms", stopwatch.elapsed().toMillis());
        return SearchInterruptedResult.INSTANCE;
      } else if (System.currentTimeMillis() > expireTime) {
        stopwatch.stop();
        log.info("Expired pathfinding after {}ms", stopwatch.elapsed().toMillis());
        return SearchExpiredResult.INSTANCE;
      }

      progressInfo.run();
      cleaner.run();

      var current = openSet.dequeue();

      log.debug("Looking at node: {}", current.node());

      // If we found our destination, we can stop looking
      if (scorer.isFinished(current)) {
        stopwatch.stop();
        log.info("Success! Took {}ms to find route", stopwatch.elapsed().toMillis());

        return new FoundRouteResult(reconstructPath(current));
      }

      try {
        var positionLong = current.node().blockPosition().asMinecraftLong();
        var cachedInstructions = instructionCache.get(positionLong);

        if (cachedInstructions == null) {
          // Cache miss - compute and store instructions
          var counter = new IntReference();
          var list = new GraphInstructions[MinecraftGraph.ACTIONS_SIZE];
          graph.insertActions(
            current.node().blockPosition(),
            current.parentToNodeDirection(),
            instructions -> {
              list[counter.value++] = instructions;
              handleInstructions(graph, openSet, routeIndex, blockItemsIndex, current, instructions, bestGlobalNode);
            }
          );
          instructionCache.put(positionLong, list);
        } else {
          // Cache hit - reuse cached instructions
          for (var instructions : cachedInstructions) {
            if (instructions == null) {
              break;
            }
            handleInstructions(graph, openSet, routeIndex, blockItemsIndex, current, instructions, bestGlobalNode);
          }
        }
      } catch (OutOfLevelException _) {
        log.debug("Found a node out of the level: {}", current.node());
        stopwatch.stop();
        log.info(
          "Took {}ms to find route to reach the edge of view distance",
          stopwatch.elapsed().toMillis());

        // The current node is not always the best node. We need to find the best node.
        var bestNode = bestGlobalNode.value;

        return new PartialRouteResult(reconstructPath(bestNode));
      }
    }

    stopwatch.stop();
    log.info("Failed to find route after {}ms", stopwatch.elapsed().toMillis());
    return NoRouteFoundResult.INSTANCE;
  }

  private void handleInstructions(MinecraftGraph graph,
                                  ObjectHeapPriorityQueue<MinecraftRouteNode> openSet,
                                  Long2ObjectOpenHashMap<MinecraftRouteNode>[] routeIndex,
                                  Long2IntOpenHashMap blockItemsIndex,
                                  MinecraftRouteNode current,
                                  GraphInstructions instructions,
                                  ObjectReference<MinecraftRouteNode> bestGlobalNode) {
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
    // Using get/put instead of compute() to avoid lambda allocation overhead
    var currentBest = blockItemsIndex.get(blockPositionLong);
    if (currentBest >= newBlocks) {
      return; // Already found a path with same or more blocks
    }
    blockItemsIndex.put(blockPositionLong, newBlocks);

    var actionCost = instructions.actionCost();
    var worldActions = instructions.actions();

    // Calculate new distance from start to this connection,
    // Get distance from the current element
    // and add the distance from the current element to the next element
    var newSourceCost = current.sourceCost() + actionCost;
    var newTargetCost = scorer.computeScore(graph, blockPosition, worldActions);
    var newTotalRouteScore = newSourceCost + newTargetCost;

    // Using get/put instead of compute() to avoid lambda allocation overhead
    var routeMap = getRouteMap(routeIndex, newBlocks);
    var existingNode = routeMap.get(blockPositionLong);

    if (existingNode == null) {
      // The first time we see this node
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

      if (newTargetCost < bestGlobalNode.value.targetCost()) {
        bestGlobalNode.value = node;
      }

      log.debug("Found a new node: {}", instructionNode);

      routeMap.put(blockPositionLong, node);
      openSet.enqueue(node);
    } else if (newSourceCost < existingNode.sourceCost()) {
      // If we found a better route to this node, update it
      existingNode.setBetterParent(
        current,
        instructions.moveDirection(),
        worldActions,
        newSourceCost,
        newTargetCost,
        newTotalRouteScore);

      if (newTargetCost < bestGlobalNode.value.targetCost()) {
        bestGlobalNode.value = existingNode;
      }

      log.debug("Found a better route to node: {}", existingNode.node());

      openSet.enqueue(existingNode);
    }
  }

  /// The result of a route search
  public sealed interface RouteSearchResult {
  }

  /// No route found to the target
  public record NoRouteFoundResult() implements RouteSearchResult {
    public static final NoRouteFoundResult INSTANCE = new NoRouteFoundResult();
  }

  /// The search was interrupted before finding a route
  public record SearchInterruptedResult() implements RouteSearchResult {
    public static final SearchInterruptedResult INSTANCE = new SearchInterruptedResult();
  }

  /// The search expired before finding a route
  public record SearchExpiredResult() implements RouteSearchResult {
    public static final SearchExpiredResult INSTANCE = new SearchExpiredResult();
  }

  /// A full route found to the target
  public record FoundRouteResult(List<WorldAction> actions) implements RouteSearchResult {
  }

  /// This is the best route we found before reaching the edge of view distance
  public record PartialRouteResult(List<WorldAction> actions) implements RouteSearchResult {
  }
}
