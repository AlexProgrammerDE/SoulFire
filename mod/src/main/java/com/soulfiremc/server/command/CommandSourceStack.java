/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.command;

import com.soulfiremc.server.InstanceManager;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.bot.BotConnection;
import com.soulfiremc.server.database.UserRole;
import com.soulfiremc.server.user.SoulFireUser;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public record CommandSourceStack(
  SoulFireServer soulFire,
  SoulFireUser source,
  @Nullable
  Set<UUID> instanceIds,
  @Nullable
  Set<UUID> botIds
) {
  public static final Predicate<CommandSourceStack> IS_ADMIN = stack -> stack.source.getRole() == UserRole.ADMIN;

  public static CommandSourceStack ofUnrestricted(SoulFireServer soulFire, SoulFireUser source) {
    return new CommandSourceStack(soulFire, source, null, null);
  }

  public static CommandSourceStack ofInstance(SoulFireServer soulFire, SoulFireUser source, Set<UUID> instanceIds) {
    return new CommandSourceStack(soulFire, source, instanceIds, null);
  }

  public static CommandSourceStack ofInstanceAndBot(SoulFireServer soulFire, SoulFireUser source, Set<UUID> instanceIds, Set<UUID> botIds) {
    return new CommandSourceStack(soulFire, source, instanceIds, botIds);
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

  public List<InstanceManager> getVisibleInstances() {
    return soulFire.instances()
      .values()
      .stream()
      .filter(instance -> instanceIds == null || instanceIds
        .stream()
        .anyMatch(instance.id()::equals))
      .toList();
  }

  public List<BotConnection> getInstanceVisibleBots(InstanceManager instance) {
    return instance.getConnectedBots()
      .stream()
      .filter(bot -> botIds == null || botIds
        .stream()
        .anyMatch(bot.accountProfileId()::equals))
      .toList();
  }

  public List<BotConnection> getGlobalVisibleBots() {
    return getVisibleInstances()
      .stream()
      .flatMap(instance -> getInstanceVisibleBots(instance).stream())
      .toList();
  }
}
