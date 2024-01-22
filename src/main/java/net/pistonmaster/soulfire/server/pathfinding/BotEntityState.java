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
package net.pistonmaster.soulfire.server.pathfinding;

import net.pistonmaster.soulfire.server.pathfinding.graph.ProjectedInventory;
import net.pistonmaster.soulfire.server.pathfinding.graph.ProjectedLevelState;
import net.pistonmaster.soulfire.server.protocol.bot.state.entity.ClientEntity;

/**
 * Represents the state of the bot in the level.
 * This means the positions and in the future also inventory.
 *
 * @param blockPosition The position of the bot in block coordinates.
 *                      This is the block the bottom of the bot is in, so the "feet" block.
 * @param levelState    The level state of the world the bot is in.
 * @param inventory     The inventory state of the bot.
 */
public record BotEntityState(SWVec3i blockPosition, ProjectedLevelState levelState,
                             ProjectedInventory inventory) {
    public static BotEntityState initialState(ClientEntity clientEntity, ProjectedLevelState levelState, ProjectedInventory inventory) {
        return new BotEntityState(
                SWVec3i.fromDouble(clientEntity.pos()),
                levelState,
                inventory
        );
    }
}
