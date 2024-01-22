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
package net.pistonmaster.soulfire.server.pathfinding.goals;

import net.pistonmaster.soulfire.server.data.BlockType;
import net.pistonmaster.soulfire.server.pathfinding.BotEntityState;
import net.pistonmaster.soulfire.server.pathfinding.Costs;
import net.pistonmaster.soulfire.server.pathfinding.SWVec3i;
import net.pistonmaster.soulfire.server.pathfinding.graph.MinecraftGraph;

// TODO: Extract into having more fine behaviour control
public record PlaceBlockGoal(SWVec3i goal, BlockType blockType) implements GoalScorer {
    public PlaceBlockGoal(int x, int y, int z, BlockType blockType) {
        this(new SWVec3i(x, y, z), blockType);
    }

    @Override
    public double computeScore(MinecraftGraph graph, BotEntityState entityState) {
        // We normally stand right next to the block, not inside, so we need to subtract 1.
        return entityState.blockPosition().distance(goal) - 1 + Costs.PLACE_BLOCK;
    }

    @Override
    public boolean isFinished(BotEntityState entityState) {
        return entityState.levelState().isChanged(goal);
    }
}
