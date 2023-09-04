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

import net.pistonmaster.serverwrecker.pathfinding.BotWorldState;
import org.cloudburstmc.math.vector.Vector3d;

public record CollectBlockGoal(Vector3d goal) implements GoalScorer {
    public CollectBlockGoal(double x, double y, double z) {
        this(Vector3d.from(x, y, z));
    }

    // TODO: When inventory is implemented, check if the block is in the inventory and apply higher score if it is.
    @Override
    public double computeScore(BotWorldState worldState) {
        return worldState.position().distance(goal);
    }

    @Override
    public boolean isFinished(BotWorldState worldState) {
        return worldState.position().equals(goal);
    }
}
