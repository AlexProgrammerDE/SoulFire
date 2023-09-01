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

import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public record RouteFinder(MinecraftGraph graph, BlockDistanceScorer scorer) {
    public List<BlockPosition> findRoute(BlockPosition from, BlockPosition to) {
        // Store block positions and the best route to them
        Map<BlockPosition, MinecraftRouteNode> routeIndex = new HashMap<>();

        // Store block positions that we need to look at
        Queue<MinecraftRouteNode> openSet = new PriorityQueue<>();

        MinecraftRouteNode start = new MinecraftRouteNode(from, null, 0d, scorer.computeCost(from, to));
        routeIndex.put(from, start);
        openSet.add(start);

        MinecraftRouteNode current;
        while ((current = openSet.poll()) != null) {
            log.debug("Looking at node: " + current);

            // If we found our destination, we can stop looking
            if (current.getPosition().equals(to)) {
                log.debug("Found our destination!");

                List<BlockPosition> route = new ArrayList<>();
                MinecraftRouteNode previousElement = current;
                do {
                    route.add(0, previousElement.getPosition());
                    previousElement = previousElement.getPrevious();
                } while (previousElement != null);

                log.debug("Route: " + route);
                return route;
            }

            for (var action : graph.getConnections(current.getPosition())) {
                BlockPosition actionTargetPos = action.getTargetBlockPos();
                MinecraftRouteNode targetPosNode = routeIndex.computeIfAbsent(actionTargetPos,
                        k -> new MinecraftRouteNode(actionTargetPos));

                // Calculate new distance from start to this connection,
                // Get distance from the current element
                // and add the distance from the current element to the next element
                double newSourceCost = current.getSourceCost() + action.getActionCost();
                if (newSourceCost < targetPosNode.getSourceCost()) {
                    targetPosNode.setPrevious(current);
                    targetPosNode.setSourceCost(newSourceCost);
                    targetPosNode.setTotalRouteScore(newSourceCost + scorer.computeCost(actionTargetPos, to));
                    openSet.add(targetPosNode);
                    log.debug("Found a better route to node: " + targetPosNode);
                }
            }
        }

        throw new IllegalStateException("No route found");
    }
}
