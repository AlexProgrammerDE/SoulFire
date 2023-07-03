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
import net.pistonmaster.serverwrecker.pathfinding.minecraft.BlockPosition;

import java.util.*;

@Slf4j
public record RouteFinder(Graph graph, Scorer nextNodeScorer, Scorer targetScorer) {
    public List<BlockPosition> findRoute(BlockPosition from, BlockPosition to) {
        Map<BlockPosition, MinecraftRouteNode> routeIndex = new HashMap<>();
        Queue<MinecraftRouteNode> openSet = new PriorityQueue<>();

        MinecraftRouteNode start = new MinecraftRouteNode(from, null, 0d, targetScorer.computeCost(from, to));
        routeIndex.put(from, start);
        openSet.add(start);

        MinecraftRouteNode current;
        while ((current = openSet.poll()) != null) {
            log.debug("Looking at node: " + current);
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

            for (var entry : graph.getConnections(current.getPosition()).entrySet()) {
                BlockPosition blockPosition = entry.getKey();
                MinecraftRouteNode nextNode = routeIndex.computeIfAbsent(blockPosition, k -> new MinecraftRouteNode(blockPosition));

                // Calculate new distance from start to this node,
                // Get distance from the current element
                // and add the distance from the current element to the next element
                double newSourceCost = current.getSourceCost() + nextNodeScorer.computeCost(current.getPosition(), blockPosition);
                if (newSourceCost < nextNode.getSourceCost()) {
                    nextNode.setPrevious(current);
                    nextNode.setSourceCost(newSourceCost);
                    nextNode.setTotalRouteScore(newSourceCost + targetScorer.computeCost(blockPosition, to));
                    openSet.add(nextNode);
                    log.debug("Found a better route to node: " + nextNode);
                }
            }
        }

        throw new IllegalStateException("No route found");
    }
}
