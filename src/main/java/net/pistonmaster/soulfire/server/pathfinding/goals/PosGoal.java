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

import net.pistonmaster.soulfire.server.pathfinding.BotEntityState;
import net.pistonmaster.soulfire.server.pathfinding.SWVec3i;
import net.pistonmaster.soulfire.server.pathfinding.graph.MinecraftGraph;

public record PosGoal(SWVec3i goal) implements GoalScorer {
    public PosGoal(int x, int y, int z) {
        this(SWVec3i.from(x, y, z));
    }

    @Override
    public double computeScore(MinecraftGraph graph, BotEntityState entityState) {
        return entityState.blockPosition().distance(goal);
    }

    @Override
    public boolean isFinished(BotEntityState entityState) {
        return entityState.blockPosition().equals(goal);
    }
}
