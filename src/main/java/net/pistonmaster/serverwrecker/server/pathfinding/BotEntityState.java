/*
 * ServerWrecker
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
package net.pistonmaster.serverwrecker.server.pathfinding;

import net.pistonmaster.serverwrecker.server.pathfinding.graph.ProjectedInventory;
import net.pistonmaster.serverwrecker.server.pathfinding.graph.ProjectedLevelState;
import net.pistonmaster.serverwrecker.server.protocol.bot.state.entity.ClientEntity;
import net.pistonmaster.serverwrecker.server.util.BlockTypeHelper;
import net.pistonmaster.serverwrecker.server.util.VectorHelper;
import org.cloudburstmc.math.vector.Vector3d;

/**
 * Represents the state of the bot in the level.
 * This means the positions and in the future also inventory.
 *
 * @param position      The position of the bot.
 *                      This is always the middle of the block.
 * @param positionBlock The position of the bot in block coordinates.
 * @param levelState    The level state of the world the bot is in.
 * @param inventory     The inventory state of the bot.
 */
public record BotEntityState(Vector3d position, SWVec3i positionBlock, ProjectedLevelState levelState,
                             ProjectedInventory inventory) {
    public BotEntityState(Vector3d position, ProjectedLevelState levelState, ProjectedInventory inventory) {
        this(position, SWVec3i.fromDouble(position), levelState, inventory);
    }

    public static BotEntityState initialState(ClientEntity clientEntity, ProjectedLevelState levelState, ProjectedInventory inventory) {
        var pos = clientEntity.pos();
        var insideBlock = levelState.getBlockStateAt(SWVec3i.fromDouble(pos));
        if (BlockTypeHelper.isRoughlyFullBlock(insideBlock.blockShapeType())) {
            // We are inside a block that is likely 0.9 blocks high,
            // so we want to start calculating as if we stand on top of it.
            pos = pos.add(0, 1, 0);
        }

        return new BotEntityState(VectorHelper.middleOfBlockNormalize(pos), levelState, inventory);
    }
}
