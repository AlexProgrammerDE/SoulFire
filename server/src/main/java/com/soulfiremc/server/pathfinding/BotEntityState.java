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
package com.soulfiremc.server.pathfinding;

import com.soulfiremc.server.pathfinding.graph.ProjectedInventory;
import com.soulfiremc.server.pathfinding.graph.ProjectedLevel;

/**
 * Represents the state of the bot in the level. This means the positions and in the future also
 * inventory.
 *
 * @param blockPosition The position of the bot in block coordinates. This is the block the bottom
 *                      of the bot is in, so the "feet" block.
 * @param level         The level state of the world the bot is in.
 * @param inventory     The inventory state of the bot.
 */
public record BotEntityState(
  SFVec3i blockPosition, ProjectedLevel level, ProjectedInventory inventory) {
}
