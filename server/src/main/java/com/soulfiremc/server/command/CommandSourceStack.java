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

import com.soulfiremc.server.user.ServerCommandSource;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public record CommandSourceStack(
  ServerCommandSource source,
  @Nullable
  List<UUID> instanceIds,
  @Nullable
  List<String> botNames
) {
  public static CommandSourceStack ofUnrestricted(ServerCommandSource source) {
    return new CommandSourceStack(source, null, null);
  }

  public static CommandSourceStack ofInstance(ServerCommandSource source, List<UUID> instanceIds) {
    return new CommandSourceStack(source, instanceIds, null);
  }

  public CommandSourceStack withInstanceIds(List<UUID> instanceIds) {
    if (this.instanceIds != null) {
      throw new IllegalStateException("Instance IDs already set");
    }

    return new CommandSourceStack(source, instanceIds, botNames);
  }

  public CommandSourceStack withBotNames(List<String> botNames) {
    if (this.botNames != null) {
      throw new IllegalStateException("Bot names already set");
    }

    return new CommandSourceStack(source, instanceIds, botNames);
  }
}
