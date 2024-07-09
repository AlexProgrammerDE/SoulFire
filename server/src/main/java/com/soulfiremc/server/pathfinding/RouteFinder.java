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
import com.soulfiremc.server.util.CallLimiter;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

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

  public List<WorldAction> findRoute(NodeState from, boolean requiresRepositioning,
                                     CompletableFuture<Void> pathCompletionFuture) {
    return findRoute(from, requiresRepositioning, pathCompletionFuture, Integer.getInteger("sf.pathfinding-expire", 180), TimeUnit.SECONDS);
  }

  public List<WorldAction> findRoute(NodeState from, boolean requiresRepositioning,
                                     CompletableFuture<Void> pathCompletionFuture,
                                     long expireDelay, TimeUnit expireTimeUnit) {
    var stopwatch = Stopwatch.createStarted();
    var expireTime = System.currentTimeMillis() + expireTimeUnit.toMillis(expireDelay);

    // Store block positions and the best route to them
    var routeIndex = new Object2ObjectOpenHashMap<NodeState, MinecraftRouteNode>();

    // Store block positions that we need to look at
    var openSet = new ObjectHeapPriorityQueue<MinecraftRouteNode>();
    var shortestPathFound = new HashSet<NodeState>();

    {
      var startScore = scorer.computeScore(graph, from.blockPosition(), List.of());
      log.debug("Start score (Usually distance): {}", startScore);

      var start =
        new MinecraftRouteNode(
          from,
          requiresRepositioning
            ? List.of(new MovementAction(from.blockPosition(), false))
            : List.of(),
          0,
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
    }, 1, TimeUnit.SECONDS);
    var cleaner = new CallLimiter(() -> {
      if (!log.isInfoEnabled()) {
        return;
      }

      var bestNode = findBestNode(routeIndex.values());
      openSet.clear();
      openSet.enqueue(bestNode);
      shortestPathFound.clear();
      routeIndex.clear();
    }, 5, TimeUnit.SECONDS);
    while (!openSet.isEmpty()) {
      if (pathCompletionFuture.isDone()) {
        stopwatch.stop();
        log.info("Cancelled pathfinding after {}ms", stopwatch.elapsed().toMillis());
        return List.of();
      } else if (System.currentTimeMillis() > expireTime) {
        stopwatch.stop();
        log.info("Expired pathfinding after {}ms", stopwatch.elapsed().toMillis());
        return List.of();
      }

      progressInfo.run();
      cleaner.run();

      var current = openSet.dequeue();
      shortestPathFound.add(current.node());
      routeIndex.remove(current.node());

      log.debug("Looking at node: {}", current.node());

      // If we found our destination, we can stop looking
      if (scorer.isFinished(current)) {
        stopwatch.stop();
        log.info("Success! Took {}ms to find route", stopwatch.elapsed().toMillis());

        return reconstructPath(current);
      }

      Consumer<GraphInstructions> callback =
        instructions -> {
          var actionCost = instructions.actionCost();
          var worldActions = instructions.actions();
          var instructionNode = instructions.node();
          if (shortestPathFound.contains(instructionNode)) {
            return;
          }

          routeIndex.compute(
            instructionNode,
            (k, v) -> {
              // Calculate new distance from start to this connection,
              // Get distance from the current element
              // and add the distance from the current element to the next element
              var newSourceCost = current.sourceCost() + actionCost;
              var newTotalRouteScore = newSourceCost + scorer.computeScore(graph, instructionNode.blockPosition(), worldActions);

              // The first time we see this node
              if (v == null) {
                var node =
                  new MinecraftRouteNode(
                    instructionNode,
                    current,
                    worldActions,
                    newSourceCost,
                    newTotalRouteScore);

                log.debug("Found a new node: {}", instructionNode);

                openSet.enqueue(node);

                return node;
              }

              // If we found a better route to this node, update it
              if (newSourceCost < v.sourceCost()) {
                v.parent(current);
                v.actions(worldActions);
                v.sourceCost(newSourceCost);
                v.totalRouteScore(newTotalRouteScore);

                log.debug("Found a better route to node: {}", instructionNode);

                openSet.enqueue(v);
              }

              return v;
            });
        };

      try {
        graph.insertActions(current.node(), callback);
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
        var recalculateTrace =
          reconstructPath(
            new MinecraftRouteNode(
              bestNode.node(),
              bestNode,
              List.of(new RecalculatePathAction()),
              bestNode.sourceCost(),
              bestNode.totalRouteScore()));

        if (recalculateTrace.size() == (requiresRepositioning ? 2 : 1)) {
          throw new AlreadyClosestException();
        }

        return recalculateTrace;
      }
    }

    stopwatch.stop();
    log.info("Failed to find route after {}ms", stopwatch.elapsed().toMillis());
    throw new NoRouteFoundException();
  }
}
