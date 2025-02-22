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
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public record RouteFinder(MinecraftGraph graph, GoalScorer scorer) {
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

  private static MinecraftRouteNode findBestNode(Collection<MinecraftRouteNode> values) {
    MinecraftRouteNode bestNode = null;
    var smallestScore = Double.MAX_VALUE;

    for (var node : values) {
      // Our implementation calculates the score from this node to the start,
      // so we need to get it by subtracting the source cost
      var targetScore = node.totalRouteScore() - node.sourceCost();

      if (targetScore < smallestScore) {
        smallestScore = targetScore;
        bestNode = node;
      }
    }

    return bestNode;
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
    var routeIndex = new Object2ObjectOpenHashMap<NodeState, MinecraftRouteNode>();

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
      routeIndex.put(from, start);
      openSet.enqueue(start);
    }

    var progressInfo = new CallLimiter(() -> {
      if (!log.isInfoEnabled()) {
        return;
      }

      var bestNode = findBestNode(routeIndex.values());
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

      var bestNode = findBestNode(routeIndex.values());
      openSet.clear();
      openSet.enqueue(bestNode);
      routeIndex.clear();
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
        instructionCache.compute(current.node().blockPosition().asMinecraftLong(), (k, v) -> {
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
      } catch (OutOfLevelException e) {
        log.debug("Found a node out of the level: {}", current.node());
        stopwatch.stop();
        log.info(
          "Took {}ms to find route to reach the edge of view distance",
          stopwatch.elapsed().toMillis());

        // The current node is not always the best node. We need to find the best node.
        var bestNode = findBestNode(routeIndex.values());

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
                                  Map<NodeState, MinecraftRouteNode> routeIndex,
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

    var instructionNode = new NodeState(instructions.blockPosition(), newBlocks);

    // Pre-check if we can reach this node with the current amount of items
    // We don't want to consider nodes again where we have even less usable items
    var bestUsableItems = blockItemsIndex.compute(instructionNode.blockPosition().asMinecraftLong(), (k, v) -> {
      if (v == null || instructionNode.usableBlockItems() > v) {
        return instructionNode.usableBlockItems();
      }

      return v;
    });
    if (bestUsableItems > instructionNode.usableBlockItems()) {
      return;
    }

    var actionCost = instructions.actionCost();
    var worldActions = instructions.actions();

    // Calculate new distance from start to this connection,
    // Get distance from the current element
    // and add the distance from the current element to the next element
    var newSourceCost = current.sourceCost() + actionCost;
    var newTargetCost = scorer.computeScore(graph, instructionNode.blockPosition(), worldActions);
    var newTotalRouteScore = newSourceCost + newTargetCost;

    routeIndex.compute(
      instructionNode,
      (k, v) -> {
        // The first time we see this node
        if (v == null) {
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

          log.debug("Found a better route to node: {}", instructionNode);

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
