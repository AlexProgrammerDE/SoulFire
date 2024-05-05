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
import com.soulfiremc.server.util.Vec2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record RouteFinder(MinecraftGraph graph, GoalScorer scorer) {
  private static List<WorldAction> getActionTrace(MinecraftRouteNode current) {
    var actions = new ObjectArrayList<WorldAction>();

    var currentElement = current;
    do {
      var previousActions = new ObjectArrayList<>(currentElement.actions());

      // Insert the actions in reversed order
      for (var i = previousActions.size() - 1; i >= 0; i--) {
        actions.add(0, previousActions.get(i));
      }

      currentElement = currentElement.parent();
    } while (currentElement != null);

    return actions;
  }

  private static MinecraftRouteNode findBestNode(MinecraftRouteNode[] values) {
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

  public List<WorldAction> findRoute(SFVec3i from, boolean requiresRepositioning,
                                     CompletableFuture<Void> pathCompletionFuture) {
    return findRoute(from, requiresRepositioning, pathCompletionFuture, Integer.getInteger("sf.pathfinding-expire", 60), TimeUnit.SECONDS);
  }

  public List<WorldAction> findRoute(SFVec3i from, boolean requiresRepositioning,
                                     CompletableFuture<Void> pathCompletionFuture,
                                     long expireDelay, TimeUnit expireTimeUnit) {
    var stopwatch = Stopwatch.createStarted();
    var expireTime = System.currentTimeMillis() + expireTimeUnit.toMillis(expireDelay);

    // Store block positions and the best route to them
    var routeIndex = new Vec2ObjectOpenHashMap<SFVec3i, MinecraftRouteNode>();

    // Store block positions that we need to look at
    var openSet = new ObjectHeapPriorityQueue<MinecraftRouteNode>();

    {
      var startScore = scorer.computeScore(graph, from, List.of());
      log.debug("Start score (Usually distance): {}", startScore);

      var start =
        new MinecraftRouteNode(
          from,
          new BotEntityState(from, graph.level(), graph.inventory()),
          requiresRepositioning
            ? List.of(new MovementAction(from, false))
            : List.of(),
          0,
          startScore);
      routeIndex.put(from, start);
      openSet.enqueue(start);
    }

    var nextLog = System.currentTimeMillis() + 1000;
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

      if (System.currentTimeMillis() > nextLog && log.isInfoEnabled()) {
        var bestNode = findBestNode(routeIndex.valuesArray());
        log.info("Still looking for route... {}ms time left, {} nodes left, closest position is {} with distance {}",
          expireTime - System.currentTimeMillis(),
          openSet.size(),
          bestNode.blockPosition().formatXYZ(),
          bestNode.totalRouteScore() - bestNode.sourceCost()
        );
        nextLog = System.currentTimeMillis() + 1000;
      }

      var current = openSet.dequeue();
      log.debug("Looking at node: {}", current.blockPosition());

      // If we found our destination, we can stop looking
      if (scorer.isFinished(current)) {
        stopwatch.stop();
        log.info("Success! Took {}ms to find route", stopwatch.elapsed().toMillis());

        return getActionTrace(current);
      }

      Consumer<GraphInstructions> callback =
        instructions -> {
          var actionCost = instructions.actionCost();
          var worldActions = instructions.actions();
          var actionTargetBlockPosition = instructions.blockPosition();
          routeIndex.compute(
            actionTargetBlockPosition,
            (k, v) -> {
              // Calculate new distance from start to this connection,
              // Get distance from the current element
              // and add the distance from the current element to the next element
              var newSourceCost = current.sourceCost() + actionCost;
              var newTotalRouteScore =
                newSourceCost + scorer.computeScore(graph, actionTargetBlockPosition, worldActions);

              // The first time we see this node
              if (v == null) {
                var node =
                  new MinecraftRouteNode(
                    actionTargetBlockPosition,
                    current,
                    worldActions,
                    newSourceCost,
                    newTotalRouteScore);

                log.debug("Found a new node: {}", actionTargetBlockPosition);

                if (node.predicatedStateValid()) {
                  openSet.enqueue(node);
                }

                return node;
              }

              // If we found a better route to this node, update it
              if (!v.predicatedStateValid() || newSourceCost < v.sourceCost()) {
                v.parent(current);
                v.actions(worldActions);
                v.sourceCost(newSourceCost);
                v.totalRouteScore(newTotalRouteScore);

                log.debug(
                  "Found a better route to node: {}", actionTargetBlockPosition);

                if (v.predicatedStateValid()) {
                  openSet.enqueue(v);
                }
              }

              return v;
            });
        };

      try {
        graph.insertActions(current.blockPosition(), callback);
      } catch (OutOfLevelException e) {
        log.debug("Found a node out of the level: {}", current.blockPosition());
        stopwatch.stop();
        log.info(
          "Took {}ms to find route to reach the edge of view distance",
          stopwatch.elapsed().toMillis());

        // The current node is not always the best node. We need to find the best node.
        var bestNode = findBestNode(routeIndex.valuesArray());

        // This is the best node we found so far
        // We will add a recalculating action and return the best route
        var recalculateTrace =
          getActionTrace(
            new MinecraftRouteNode(
              bestNode.blockPosition(),
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
