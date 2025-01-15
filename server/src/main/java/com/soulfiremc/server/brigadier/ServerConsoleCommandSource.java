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
package com.soulfiremc.server.brigadier;

import com.soulfiremc.server.user.PermissionContext;
import com.soulfiremc.server.user.ServerCommandSource;
import com.soulfiremc.server.util.SoulFireAdventure;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

public class ServerConsoleCommandSource implements ServerCommandSource {
  public static final ServerConsoleCommandSource INSTANCE = new ServerConsoleCommandSource();
  private static final Logger log = LoggerFactory.getLogger("Console");

  @Override
  public void sendMessage(Level level, Component message) {
    log.atLevel(level).setMessage("{}").addArgument(() -> SoulFireAdventure.ANSI_SERIALIZER.serialize(message)).log();
  }

  @Override
  public TriState getPermission(PermissionContext permission) {
    return TriState.TRUE;
  }

  @Override
  public String identifier() {
    return "console";
  }
}
