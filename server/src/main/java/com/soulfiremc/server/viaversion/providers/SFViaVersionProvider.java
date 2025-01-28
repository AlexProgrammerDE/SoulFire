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
package com.soulfiremc.server.viaversion.providers;

import com.soulfiremc.server.protocol.netty.ViaClientSession;
import com.soulfiremc.server.viaversion.SFVersionConstants;
import com.soulfiremc.server.viaversion.StorableSession;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.protocol.version.VersionType;
import com.viaversion.viaversion.protocol.version.BaseVersionProvider;

import java.util.Objects;

public class SFViaVersionProvider extends BaseVersionProvider {
  @Override
  public ProtocolVersion getClientProtocol(UserConnection connection) {
    final var clientProtocol = connection.getProtocolInfo().protocolVersion();
    if (!clientProtocol.isKnown() && ProtocolVersion.isRegistered(VersionType.SPECIAL, clientProtocol.getOriginalVersion())) {
      return ProtocolVersion.getProtocol(VersionType.SPECIAL, clientProtocol.getOriginalVersion());
    } else {
      return super.getClientProtocol(connection);
    }
  }

  @Override
  public ProtocolVersion getClosestServerProtocol(UserConnection connection) {
    if (connection.isClientSide()) {
      var session = (ViaClientSession) Objects.requireNonNull(connection.get(StorableSession.class), "Session provider is null")
        .session();

      return session.botConnection().protocolVersion();
    } else {
      return SFVersionConstants.CURRENT_PROTOCOL_VERSION;
    }
  }
}
