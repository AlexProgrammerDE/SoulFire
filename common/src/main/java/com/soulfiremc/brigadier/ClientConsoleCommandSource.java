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
package com.soulfiremc.brigadier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientConsoleCommandSource implements CommandSource {
  public static final ClientConsoleCommandSource INSTANCE = new ClientConsoleCommandSource();
  private static final Logger log = LoggerFactory.getLogger("Console");

  @Override
  public void sendInfo(String message, Object... args) {
    log.info(message, args);
  }

  @Override
  public void sendWarn(String message, Object... args) {
    log.warn(message, args);
  }

  @Override
  public void sendError(String message, Throwable t) {
    log.error(message, t);
  }

  @Override
  public void sendMessage(String message) {
    log.info(message);
  }
}
