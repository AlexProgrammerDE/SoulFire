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
package net.pistonmaster.serverwrecker.pathfinding.goals;

import net.pistonmaster.serverwrecker.data.BlockType;
import net.pistonmaster.serverwrecker.pathfinding.BotEntityState;
import net.pistonmaster.serverwrecker.pathfinding.SWVec3i;
import net.pistonmaster.serverwrecker.pathfinding.graph.MinecraftGraph;
import net.pistonmaster.serverwrecker.util.BlockTypeHelper;

public record BreakBlockPosGoal(SWVec3i goal) implements GoalScorer {
    @Override
    public double computeScore(MinecraftGraph graph, BotEntityState entityState) {
        var distance = entityState.position().distance(goal.x, goal.y, goal.z);
        var blockStateMeta = entityState.levelState().getBlockStateAt(goal);

        // Instead of failing when the block is not in render distance, we just return the distance.
        if (blockStateMeta.blockType() == BlockType.VOID_AIR) {
            return distance;
        }

        // Don't try to find a way to dig bedrock
        if (!blockStateMeta.blockType().diggable()) {
            throw new IllegalStateException("Block is not diggable!");
        }

        // We only want to dig full blocks (not slabs, stairs, etc.), removes a lot of edge cases
        if (!blockStateMeta.blockShapeType().isFullBlock()) {
            throw new IllegalStateException("Block is not a full block!");
        }

        var breakCost = entityState.inventory().getMiningCosts(
                graph.tagsState(),
                blockStateMeta
        ).miningCost();

        return distance + breakCost;
    }

    @Override
    public boolean isFinished(BotEntityState entityState) {
        return BlockTypeHelper.isAir(entityState.levelState()
                .getBlockStateAt(goal)
                .blockType());
    }
}
