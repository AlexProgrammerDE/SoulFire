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

import org.fusesource.jansi.Ansi;
import org.slf4j.helpers.MessageFormatter;

public interface CommandSource {
  private static String format(String format, Object[] params, Throwable t) {
    return MessageFormatter.arrayFormat(format, params, t).getMessage();
  }

  default void sendInfo(String message, Object... args) {
    sendMessage(format(message, args, null));
  }

  default void sendWarn(String message, Object... args) {
    sendMessage(Ansi.ansi().fgYellow() + format(message, args, null));
  }

  default void sendError(String message, Throwable t) {
    sendMessage(Ansi.ansi().fgRed() + format(message, new Object[0], t));
  }

  void sendMessage(String message);
}
