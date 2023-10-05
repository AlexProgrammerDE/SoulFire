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

import net.pistonmaster.serverwrecker.pathfinding.BotEntityState;
import net.pistonmaster.serverwrecker.pathfinding.graph.MinecraftGraph;

/**
 * A goal represents something that the user wants the bot to achieve.
 */
public interface GoalScorer {
    /**
     * Calculates the estimated score for a given block position to the goal.
     * Usually this means the distance from achieving the goal.
     *
     * @param entityState the world state to calculate the score for
     * @return the score for the given world state
     */
    double computeScore(MinecraftGraph graph, BotEntityState entityState);

    /**
     * Checks if the given world state indicates that the goal is reached.
     *
     * @param entityState the current world state
     * @return true if the goal is reached, false otherwise
     */
    boolean isFinished(BotEntityState entityState);
}
