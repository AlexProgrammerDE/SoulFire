/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.pathfinding;

import com.google.common.base.Stopwatch;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;
import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.serverwrecker.pathfinding.execution.MovementAction;
import net.pistonmaster.serverwrecker.pathfinding.execution.RecalculatePathAction;
import net.pistonmaster.serverwrecker.pathfinding.execution.WorldAction;
import net.pistonmaster.serverwrecker.pathfinding.goals.GoalScorer;
import net.pistonmaster.serverwrecker.pathfinding.graph.MinecraftGraph;
import net.pistonmaster.serverwrecker.pathfinding.graph.OutOfLevelException;
import net.pistonmaster.serverwrecker.pathfinding.graph.actions.GraphInstructions;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.Collections;
import java.util.List;

@Slf4j
public record RouteFinder(MinecraftGraph graph, GoalScorer scorer) {
    private static List<WorldAction> getActionTrace(MinecraftRouteNode current) {
        var actions = new ObjectArrayList<WorldAction>();
        var previousElement = current;
        do {
            var previousActions = new ObjectArrayList<>(previousElement.getPreviousActions());

            // So they get executed in the right order
            Collections.reverse(previousActions);
            for (var action : previousActions) {
                actions.add(0, action);
            }

            previousElement = previousElement.getPrevious();
        } while (previousElement != null);

        return actions;
    }

    public List<WorldAction> findRoute(BotEntityState from, boolean requiresRepositioning) {
        var stopwatch = Stopwatch.createStarted();

        // Store block positions and the best route to them
        var routeIndex = new Object2ObjectOpenHashMap<Vector3i, MinecraftRouteNode>();

        // Store block positions that we need to look at
        var openSet = new ObjectHeapPriorityQueue<MinecraftRouteNode>(1);

        {
            var startScore = scorer.computeScore(graph, from);
            log.info("Start score (Usually distance): {}", startScore);

            var start = new MinecraftRouteNode(
                    from,
                    null,
                    requiresRepositioning ? List.of(new MovementAction(from.position(), false)) : List.of(),
                    0,
                    startScore
            );
            routeIndex.put(from.positionBlock(), start);
            openSet.enqueue(start);
        }

        while (!openSet.isEmpty()) {
            var current = openSet.dequeue();
            log.debug("Looking at node: {}", current.getEntityState().position());

            // If we found our destination, we can stop looking
            if (scorer.isFinished(current.getEntityState())) {
                stopwatch.stop();
                log.info("Success! Took {}ms to find route", stopwatch.elapsed().toMillis());

                return getActionTrace(current);
            }

            GraphInstructions[] instructionsList;
            try {
                instructionsList = graph.getActions(current.getEntityState());
            } catch (OutOfLevelException e) {
                log.debug("Found a node out of the level: {}", current.getEntityState().positionBlock());
                stopwatch.stop();
                log.info("Took {}ms to find route to reach the edge of view distance", stopwatch.elapsed().toMillis());

                // The current node is not always the best node. We need to find the best node.
                var bestNode = findBestNode(routeIndex.values());

                // This is the best node we found so far
                // We will add a recalculating action and return the best route
                var recalculateTrace = getActionTrace(new MinecraftRouteNode(
                        bestNode.getEntityState(),
                        bestNode,
                        List.of(new RecalculatePathAction()),
                        bestNode.getSourceCost(), bestNode.getTotalRouteScore()
                ));

                if (recalculateTrace.size() <= 2) {
                    throw new IllegalStateException("Could not find a path and this is already the closest we can get to the goal.");
                }

                return recalculateTrace;
            }

            for (var instructions : instructionsList) {
                if (instructions == null) {
                    break;
                }

                var actionTargetState = instructions.targetState();
                routeIndex.compute(actionTargetState.positionBlock(), (k, v) -> {
                    var actionCost = instructions.actionCost();
                    var worldActions = instructions.actions();

                    // Calculate new distance from start to this connection,
                    // Get distance from the current element
                    // and add the distance from the current element to the next element
                    var newSourceCost = current.getSourceCost() + actionCost;
                    var newTotalRouteScore = newSourceCost + scorer.computeScore(graph, actionTargetState);

                    // The first time we see this node
                    if (v == null) {
                        var node = new MinecraftRouteNode(actionTargetState, current, worldActions, newSourceCost, newTotalRouteScore);
                        log.debug("Found a new node: {}", actionTargetState.positionBlock());
                        openSet.enqueue(node);

                        return node;
                    }

                    // If we found a better route to this node, update it
                    if (newSourceCost < v.getSourceCost()) {
                        v.setPrevious(current);
                        v.setPreviousActions(worldActions);
                        v.setSourceCost(newSourceCost);
                        v.setTotalRouteScore(newTotalRouteScore);

                        log.debug("Found a better route to node: {}", actionTargetState.positionBlock());
                        openSet.enqueue(v);
                    }

                    return v;
                });
            }
        }

        stopwatch.stop();
        log.info("Failed to find route after {}ms", stopwatch.elapsed().toMillis());
        throw new IllegalStateException("No route found");
    }

    private MinecraftRouteNode findBestNode(ObjectCollection<MinecraftRouteNode> values) {
        MinecraftRouteNode bestNode = null;
        var smallestScore = Double.MAX_VALUE;

        for (var node : values) {
            // Our implementation calculates the score from this node to the start,
            // so we need to get it by subtracting the source cost
            var targetScore = node.getTotalRouteScore() - node.getSourceCost();

            if (targetScore < smallestScore) {
                smallestScore = targetScore;
                bestNode = node;
            }
        }

        return bestNode;
    }
}
