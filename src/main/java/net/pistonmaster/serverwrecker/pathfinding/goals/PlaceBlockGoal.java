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
import net.pistonmaster.serverwrecker.pathfinding.Costs;
import net.pistonmaster.serverwrecker.pathfinding.graph.MinecraftGraph;
import org.cloudburstmc.math.vector.Vector3d;
import net.pistonmaster.serverwrecker.pathfinding.SWVec3i;

// TODO: Extract into having more fine behaviour control
public record PlaceBlockGoal(SWVec3i goal, Vector3d goal3d, BlockType blockType) implements GoalScorer {
    public PlaceBlockGoal(int x, int y, int z, BlockType blockType) {
        this(Vector3d.from(x, y, z), blockType);
    }

    public PlaceBlockGoal(Vector3d goalBlock, BlockType blockType) {
        this(SWVec3i.fromDouble(goalBlock), goalBlock, blockType);
    }

    @Override
    public double computeScore(MinecraftGraph graph, BotEntityState entityState) {
        // We normally stand right next to the block, not inside, so we need to subtract 1.
        return entityState.position().distance(goal3d) - 1 + Costs.PLACE_BLOCK;
    }

    @Override
    public boolean isFinished(BotEntityState entityState) {
        return entityState.levelState().isChanged(goal);
    }
}
