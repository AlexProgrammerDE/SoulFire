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
package com.soulfiremc.server.grpc;

import com.soulfiremc.server.plugins.ChatMessageLogger;
import com.soulfiremc.server.user.AuthSystem;
import com.soulfiremc.server.user.PermissionContext;
import com.soulfiremc.server.user.ServerCommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import org.slf4j.event.Level;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

public class DefaultAuthSystem implements AuthSystem {
  @Override
  public ServerCommandSource authenticate(String subject, Date issuedAt) {
    return new GrpcUser(
      subject,
      UUID.nameUUIDFromBytes("RemoteUser:%s".formatted(subject).getBytes(StandardCharsets.UTF_8))
    );
  }

  private record GrpcUser(String subject, UUID uuid) implements ServerCommandSource {
    @Override
    public UUID getUniqueId() {
      return uuid;
    }

    @Override
    public String getUsername() {
      return subject;
    }

    @Override
    public TriState getPermission(PermissionContext permission) {
      return TriState.TRUE;
    }

    @Override
    public void sendMessage(Level level, Component message) {
      LogServiceImpl.sendMessage(uuid, ChatMessageLogger.ANSI_MESSAGE_SERIALIZER.serialize(message));
    }
  }
}
