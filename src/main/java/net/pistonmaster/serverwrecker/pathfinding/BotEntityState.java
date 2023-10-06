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

import net.pistonmaster.serverwrecker.pathfinding.graph.ProjectedInventory;
import net.pistonmaster.serverwrecker.pathfinding.graph.ProjectedLevelState;
import net.pistonmaster.serverwrecker.util.VectorHelper;
import org.cloudburstmc.math.vector.Vector3d;

import java.util.Objects;

/**
 * Represents the state of the bot in the level.
 * This means the positions and in the future also inventory.
 *
 * @param position   The position of the bot.
 *                   This is always the middle of the block.
 * @param levelState The level state of the world the bot is in.
 * @param inventory  The inventory state of the bot.
 * @param precalculatedHash The precalculated hash of the object.
 */
public record BotEntityState(Vector3d position, ProjectedLevelState levelState, ProjectedInventory inventory,
                             int precalculatedHash) {
    public BotEntityState {
        position = VectorHelper.middleOfBlockNormalize(position);
    }

    public BotEntityState(Vector3d position, ProjectedLevelState levelState, ProjectedInventory inventory) {
        this(position, levelState, inventory, Objects.hash(position, levelState, inventory));
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BotEntityState other)) {
            return false;
        }

        return other.precalculatedHash == precalculatedHash;
    }

    @Override
    public int hashCode() {
        return precalculatedHash;
    }
}
