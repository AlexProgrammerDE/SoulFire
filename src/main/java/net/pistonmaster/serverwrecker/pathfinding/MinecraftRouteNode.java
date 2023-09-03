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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
class MinecraftRouteNode implements Comparable<MinecraftRouteNode> {
    /**
     * The block position of this node.
     */
    private final BlockPosition position;
    /**
     * The currently best known node to this node.
     */
    private MinecraftRouteNode previous;
    /**
     * The action from the previous node that was used to get to this node.
     */
    private MinecraftAction previousAction;
    /**
     * The cost of the route from the start node to this node.
     */
    private double sourceCost;
    /**
     * The estimated cost of the route from this node to the target.
     */
    private double totalRouteScore;

    MinecraftRouteNode(BlockPosition position) {
        this(position, null, null, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    @Override
    public int compareTo(MinecraftRouteNode other) {
        return Double.compare(this.totalRouteScore, other.totalRouteScore);
    }
}
