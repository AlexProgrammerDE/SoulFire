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
package com.soulfiremc.server.user;

import com.soulfiremc.brigadier.CommandSource;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.util.TriState;

public interface ServerCommandSource extends CommandSource {
  @Override
  default void sendInfo(String message, Object... args) {
    sendMessage(Component.text(CommandSource.format(message, args, null)));
  }

  @Override
  default void sendWarn(String message, Object... args) {
    sendMessage(Component.text(CommandSource.format(message, args, null)).color(NamedTextColor.YELLOW));
  }

  @Override
  default void sendError(String message, Throwable t) {
    sendMessage(Component.text(CommandSource.format(message, new Object[0], t)).color(NamedTextColor.RED));
  }

  @Override
  default void sendMessage(String message) {
    sendMessage(Component.text(message));
  }

  void sendMessage(Component message);

  UUID getUniqueId();

  String getUsername();

  TriState getPermission(Permission permission);

  default boolean hasPermission(Permission permission) {
    return getPermission(permission).toBooleanOrElse(false);
  }

  default void hasPermissionOrThrow(Permission permission) {
    if (!hasPermission(permission)) {
      throw new StatusRuntimeException(
        Status.PERMISSION_DENIED.withDescription("You do not have permission to access this resource"));
    }
  }
}
