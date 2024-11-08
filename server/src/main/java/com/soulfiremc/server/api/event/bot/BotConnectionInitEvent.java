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
package com.soulfiremc.server.api.event.bot;

import com.soulfiremc.server.api.event.SoulFireBotEvent;
import com.soulfiremc.server.protocol.BotConnection;

/**
 * The event is called the moment after a bot connection object was created. The BotConnection
 * instance has all fields filled, but most methods are unusable as the bot is not connected yet.
 * <br>
 * This event is recommended for when you want to add a plugin listener to the bot connection.
 *
 * @param connection The bot connection instance.
 */
public record BotConnectionInitEvent(BotConnection connection) implements SoulFireBotEvent {
}
