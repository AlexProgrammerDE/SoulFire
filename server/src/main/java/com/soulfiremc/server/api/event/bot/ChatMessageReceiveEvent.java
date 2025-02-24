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

import com.soulfiremc.server.adventure.SoulFireAdventure;
import com.soulfiremc.server.api.event.SoulFireBotEvent;
import com.soulfiremc.server.protocol.BotConnection;
import net.kyori.adventure.text.Component;

/**
 * This event is called when a chat message is received from the server.
 *
 * @param connection The bot connection instance.
 * @param timestamp  The timestamp when the message was received.
 * @param message    The message that was received.
 */
public record ChatMessageReceiveEvent(BotConnection connection, long timestamp, Component message)
  implements SoulFireBotEvent {
  public String parseToPlainText() {
    return SoulFireAdventure.PLAIN_MESSAGE_SERIALIZER.serialize(message);
  }
}
