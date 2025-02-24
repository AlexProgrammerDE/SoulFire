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

import com.soulfiremc.server.adventure.SoulFireAdventure;
import com.soulfiremc.server.database.UserEntity;
import com.soulfiremc.server.user.AuthSystem;
import com.soulfiremc.server.user.PermissionContext;
import com.soulfiremc.server.user.SoulFireUser;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.util.UUID;

@RequiredArgsConstructor
public class ConsoleCommandSource implements SoulFireUser {
  private static final Logger log = LoggerFactory.getLogger("Console");
  private final AuthSystem authSystem;

  @Override
  public void sendMessage(Level level, Component message) {
    log.atLevel(level).setMessage("{}").addArgument(() -> SoulFireAdventure.ANSI_SERIALIZER.serialize(message)).log();
  }

  @Override
  public TriState getPermission(PermissionContext permission) {
    return TriState.TRUE;
  }

  @Override
  public UUID getUniqueId() {
    return authSystem.rootUserId();
  }

  @Override
  public String getUsername() {
    return authSystem.rootUserData().username();
  }

  @Override
  public String getEmail() {
    return authSystem.rootUserData().email();
  }

  @Override
  public UserEntity.Role getRole() {
    return authSystem.rootUserData().role();
  }
}
