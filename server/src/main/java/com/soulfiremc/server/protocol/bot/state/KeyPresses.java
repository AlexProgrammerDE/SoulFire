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
package com.soulfiremc.server.protocol.bot.state;

import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundPlayerInputPacket;

public record KeyPresses(
  boolean forward,
  boolean backward,
  boolean left,
  boolean right,
  boolean jump,
  boolean shift,
  boolean sprint
) {
  public static final KeyPresses EMPTY = new KeyPresses(false, false, false, false, false, false, false);

  public ServerboundPlayerInputPacket toServerboundPlayerInputPacket() {
    return new ServerboundPlayerInputPacket(forward, backward, left, right, jump, shift, sprint);
  }
}
