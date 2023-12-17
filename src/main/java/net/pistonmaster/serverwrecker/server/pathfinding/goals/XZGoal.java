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
package net.pistonmaster.serverwrecker.server.pathfinding.goals;

import net.pistonmaster.serverwrecker.server.pathfinding.BotEntityState;
import net.pistonmaster.serverwrecker.server.pathfinding.graph.MinecraftGraph;
import net.pistonmaster.serverwrecker.server.util.VectorHelper;
import org.cloudburstmc.math.vector.Vector2d;

public record XZGoal(Vector2d goal) implements GoalScorer {
    public XZGoal {
        goal = VectorHelper.middleOfBlockNormalize(goal);
    }

    public XZGoal(double x, double z) {
        this(Vector2d.from(x, z));
    }

    @Override
    public double computeScore(MinecraftGraph graph, BotEntityState entityState) {
        var position = entityState.position();
        return Vector2d.from(position.getX(), position.getZ()).distance(goal);
    }

    @Override
    public boolean isFinished(BotEntityState entityState) {
        var position = entityState.position();
        return Vector2d.from(position.getX(), position.getZ()).equals(goal);
    }
}
