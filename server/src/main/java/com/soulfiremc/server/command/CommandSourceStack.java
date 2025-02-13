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

import com.soulfiremc.server.SoulFireServer;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record CommandSourceStack(
  SoulFireServer soulFire,
  CommandSource source,
  @Nullable
  Set<UUID> instanceIds,
  @Nullable
  Set<UUID> botIds
) {
  public static CommandSourceStack ofUnrestricted(SoulFireServer soulFire, CommandSource source) {
    return new CommandSourceStack(soulFire, source, null, null);
  }

  public static CommandSourceStack ofInstance(SoulFireServer soulFire, CommandSource source, Set<UUID> instanceIds) {
    return new CommandSourceStack(soulFire, source, instanceIds, null);
  }

  public CommandSourceStack withInstanceIds(Set<UUID> instanceIds) {
    if (Objects.equals(this.instanceIds, instanceIds)) {
      return this;
    }

    if (this.instanceIds != null) {
      throw new IllegalStateException("Instance IDs already set");
    }

    return new CommandSourceStack(soulFire, source, instanceIds, botIds);
  }

  public CommandSourceStack withBotIds(Set<UUID> botIds) {
    if (Objects.equals(this.botIds, botIds)) {
      return this;
    }

    if (this.botIds != null) {
      throw new IllegalStateException("Bot names already set");
    }

    return new CommandSourceStack(soulFire, source, instanceIds, botIds);
  }
}
