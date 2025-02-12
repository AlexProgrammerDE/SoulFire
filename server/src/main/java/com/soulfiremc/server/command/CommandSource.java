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
package com.soulfiremc.server.command;

import com.soulfiremc.server.user.PermissionContext;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.util.TriState;
import org.slf4j.event.Level;
import org.slf4j.helpers.MessageFormatter;

public interface CommandSource {
  private static String format(String format, Object[] params, Throwable t) {
    return MessageFormatter.arrayFormat(format, params, t).getMessage();
  }

  default void sendInfo(String message, Object... args) {
    sendMessage(Level.INFO, format(message, args, null));
  }

  default void sendWarn(String message, Object... args) {
    sendMessage(Level.WARN, format(message, args, null));
  }

  default void sendError(String message, Throwable t) {
    sendMessage(Level.ERROR, format(message, new Object[0], t));
  }

  default void sendMessage(Level level, String message) {
    var component = Component.text(message);
    sendMessage(level, switch (level) {
      case WARN -> component.color(NamedTextColor.YELLOW);
      case ERROR -> component.color(NamedTextColor.RED);
      default -> component;
    });
  }

  void sendMessage(Level level, Component message);

  TriState getPermission(PermissionContext permission);

  default boolean hasPermission(PermissionContext permission) {
    return getPermission(permission).toBooleanOrElse(false);
  }

  default void hasPermissionOrThrow(PermissionContext permission) {
    if (!hasPermission(permission)) {
      throw new StatusRuntimeException(
        Status.PERMISSION_DENIED.withDescription("You do not have permission to access this resource"));
    }
  }

  String identifier();
}
