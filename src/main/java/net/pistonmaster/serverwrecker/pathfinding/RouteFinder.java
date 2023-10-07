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
import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;
import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.serverwrecker.pathfinding.execution.MovementAction;
import net.pistonmaster.serverwrecker.pathfinding.execution.RecalculatePathAction;
import net.pistonmaster.serverwrecker.pathfinding.execution.WorldAction;
import net.pistonmaster.serverwrecker.pathfinding.goals.GoalScorer;
import net.pistonmaster.serverwrecker.pathfinding.graph.GraphInstructions;
import net.pistonmaster.serverwrecker.pathfinding.graph.MinecraftGraph;
import net.pistonmaster.serverwrecker.pathfinding.graph.OutOfLevelException;

import java.util.Collections;
import java.util.List;

@Slf4j
public record RouteFinder(MinecraftGraph graph, GoalScorer scorer) {
    private static List<WorldAction> getActions(MinecraftRouteNode current) {
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

    public List<WorldAction> findRoute(BotEntityState from) {
        var stopwatch = Stopwatch.createStarted();

        // Store block positions and the best route to them
        Int2ObjectMap<MinecraftRouteNode> routeIndex = new Int2ObjectOpenHashMap<>();

        // Store block positions that we need to look at
        PriorityQueue<MinecraftRouteNode> openSet = new ObjectHeapPriorityQueue<>();

        var startScore = scorer.computeScore(graph, from);
        log.info("Start score (Usually distance): {}", startScore);

        var start = new MinecraftRouteNode(from, null, List.of(new MovementAction(from.position(), false)), 0d, startScore);
        routeIndex.put(from.precalculatedHash(), start);
        openSet.enqueue(start);

        while (!openSet.isEmpty()) {
            var current = openSet.dequeue();
            log.debug("Looking at node: {}", current.getEntityState().position());

            // If we found our destination, we can stop looking
            if (scorer.isFinished(current.getEntityState())) {
                stopwatch.stop();
                log.info("Success! Took {}ms to find route", stopwatch.elapsed().toMillis());

                return getActions(current);
            }

            List<GraphInstructions> instructionsList;
            try {
                instructionsList = graph.getActions(current.getEntityState());
            } catch (OutOfLevelException e) {
                log.debug("Found a node out of the level: {}", current.getEntityState().position());
                stopwatch.stop();
                log.info("Took {}ms to find route to this point", stopwatch.elapsed().toMillis());

                // This is the best node we found so far
                // We will add a recalculating action and return the best route
                var recalculatingNode = new MinecraftRouteNode(
                        current.getEntityState(),
                        current,
                        List.of(new RecalculatePathAction()),
                        current.getSourceCost(), current.getTotalRouteScore()
                );
                return getActions(recalculatingNode);
            }

            for (var instructions : instructionsList) {
                if (instructions.isImpossible()) {
                    continue;
                }

                var actionCost = instructions.actionCost();
                var actionTargetState = instructions.targetState();
                var worldActions = instructions.actions();
                routeIndex.compute(actionTargetState.precalculatedHash(), (k, v) -> {
                    // Calculate new distance from start to this connection,
                    // Get distance from the current element
                    // and add the distance from the current element to the next element
                    var newSourceCost = current.getSourceCost() + actionCost;
                    var newTotalRouteScore = newSourceCost + scorer.computeScore(graph, actionTargetState);

                    // The first time we see this node
                    if (v == null) {
                        var node = new MinecraftRouteNode(actionTargetState, current, worldActions, newSourceCost, newTotalRouteScore);
                        log.debug("Found a new node: {}", actionTargetState.position());
                        openSet.enqueue(node);

                        return node;
                    }

                    // If we found a better route to this node, update it
                    if (newSourceCost < v.getSourceCost()) {
                        v.setPrevious(current);
                        v.setPreviousActions(worldActions);
                        v.setSourceCost(newSourceCost);
                        v.setTotalRouteScore(newTotalRouteScore);

                        log.debug("Found a better route to node: {}", actionTargetState.position());
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
}
