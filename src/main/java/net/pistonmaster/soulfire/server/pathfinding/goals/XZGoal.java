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
import net.pistonmaster.soulfire.server.pathfinding.graph.MinecraftGraph;
import org.cloudburstmc.math.vector.Vector2i;

public record XZGoal(Vector2i goal) implements GoalScorer {
    public XZGoal(int x, int z) {
        this(Vector2i.from(x, z));
    }

    @Override
    public double computeScore(MinecraftGraph graph, BotEntityState entityState) {
        return Vector2i.from(entityState.blockPosition().x, entityState.blockPosition().z)
                .distance(goal);
    }

    @Override
    public boolean isFinished(BotEntityState entityState) {
        return Vector2i.from(entityState.blockPosition().x, entityState.blockPosition().z)
                .equals(goal);
    }
}
