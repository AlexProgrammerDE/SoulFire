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
import com.soulfiremc.server.viaversion.StorableSession;
import com.viaversion.viaversion.api.connection.UserConnection;
import net.raphimc.vialegacy.protocol.release.r1_2_4_5tor1_3_1_2.provider.OldAuthProvider;

import java.util.Objects;

public class SFViaOldAuthProvider extends OldAuthProvider {
  @Override
  public void sendAuthRequest(UserConnection user, String serverId) {
    var session = (ViaClientSession) Objects.requireNonNull(user.get(StorableSession.class), "Session provider is null")
      .session();

    session.botConnection().joinServerId(serverId);
  }
}
