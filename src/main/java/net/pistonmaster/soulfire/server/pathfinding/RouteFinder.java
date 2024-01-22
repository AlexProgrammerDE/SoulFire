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
package net.pistonmaster.soulfire.server.pathfinding;

import com.google.common.base.Stopwatch;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;
import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.soulfire.server.pathfinding.execution.MovementAction;
import net.pistonmaster.soulfire.server.pathfinding.execution.RecalculatePathAction;
import net.pistonmaster.soulfire.server.pathfinding.execution.WorldAction;
import net.pistonmaster.soulfire.server.pathfinding.goals.GoalScorer;
import net.pistonmaster.soulfire.server.pathfinding.graph.GraphInstructions;
import net.pistonmaster.soulfire.server.pathfinding.graph.MinecraftGraph;
import net.pistonmaster.soulfire.server.pathfinding.graph.OutOfLevelException;
import net.pistonmaster.soulfire.server.util.Vec2ObjectOpenHashMap;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public record RouteFinder(MinecraftGraph graph, GoalScorer scorer) {
    private static List<WorldAction> getActionTrace(MinecraftRouteNode current) {
        var actions = new ObjectArrayList<WorldAction>();
        var previousElement = current;
        do {
            var previousActions = new ObjectArrayList<>(previousElement.previousActions());

            // So they get executed in the right order
            Collections.reverse(previousActions);
            for (var action : previousActions) {
                actions.add(0, action);
            }

            previousElement = previousElement.previous();
        } while (previousElement != null);

        return actions;
    }

    public List<WorldAction> findRoute(BotEntityState from, boolean requiresRepositioning) {
        var stopwatch = Stopwatch.createStarted();

        // Store block positions and the best route to them
        var routeIndex = new Vec2ObjectOpenHashMap<SWVec3i, MinecraftRouteNode>();

        // Store block positions that we need to look at
        var openSet = new ObjectHeapPriorityQueue<MinecraftRouteNode>(1);

        {
            var startScore = scorer.computeScore(graph, from);
            log.debug("Start score (Usually distance): {}", startScore);

            var start = new MinecraftRouteNode(
                    from,
                    null,
                    requiresRepositioning ? List.of(new MovementAction(from.blockPosition(), false)) : List.of(),
                    0,
                    startScore
            );
            routeIndex.put(from.blockPosition(), start);
            openSet.enqueue(start);
        }

        while (!openSet.isEmpty()) {
            var current = openSet.dequeue();
            log.debug("Looking at node: {}", current.entityState().blockPosition());

            // If we found our destination, we can stop looking
            if (scorer.isFinished(current.entityState())) {
                stopwatch.stop();
                log.info("Success! Took {}ms to find route", stopwatch.elapsed().toMillis());

                return getActionTrace(current);
            }

            Consumer<GraphInstructions> callback = instructions -> {
                var actionCost = instructions.actionCost();
                var worldActions = instructions.actions();
                var actionTargetState = instructions.targetState();
                routeIndex.compute(actionTargetState.blockPosition(), (k, v) -> {
                    // Calculate new distance from start to this connection,
                    // Get distance from the current element
                    // and add the distance from the current element to the next element
                    var newSourceCost = current.sourceCost() + actionCost;
                    var newTotalRouteScore = newSourceCost + scorer.computeScore(graph, actionTargetState);

                    // The first time we see this node
                    if (v == null) {
                        var node = new MinecraftRouteNode(actionTargetState, current, worldActions, newSourceCost, newTotalRouteScore);
                        log.debug("Found a new node: {}", actionTargetState.blockPosition());
                        openSet.enqueue(node);

                        return node;
                    }

                    // If we found a better route to this node, update it
                    if (newSourceCost < v.sourceCost()) {
                        v.previous(current);
                        v.previousActions(worldActions);
                        v.sourceCost(newSourceCost);
                        v.totalRouteScore(newTotalRouteScore);

                        log.debug("Found a better route to node: {}", actionTargetState.blockPosition());
                        openSet.enqueue(v);
                    }

                    return v;
                });
            };

            try {
                graph.insertActions(current.entityState(), callback, routeIndex::containsKey);
            } catch (OutOfLevelException e) {
                log.debug("Found a node out of the level: {}", current.entityState().blockPosition());
                stopwatch.stop();
                log.info("Took {}ms to find route to reach the edge of view distance", stopwatch.elapsed().toMillis());

                // The current node is not always the best node. We need to find the best node.
                var bestNode = findBestNode(routeIndex.values());

                // This is the best node we found so far
                // We will add a recalculating action and return the best route
                var recalculateTrace = getActionTrace(new MinecraftRouteNode(
                        bestNode.entityState(),
                        bestNode,
                        List.of(new RecalculatePathAction()),
                        bestNode.sourceCost(),
                        bestNode.totalRouteScore()
                ));

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

    private MinecraftRouteNode findBestNode(ObjectCollection<MinecraftRouteNode> values) {
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
}
