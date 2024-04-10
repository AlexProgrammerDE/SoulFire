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

import com.soulfiremc.server.viaversion.StorableSession;
import com.viaversion.viaversion.api.connection.UserConnection;
import java.util.Objects;
import net.raphimc.vialegacy.protocols.release.protocol1_3_1_2to1_2_4_5.providers.OldAuthProvider;

public class SFViaOldAuthProvider extends OldAuthProvider {
  @Override
  public void sendAuthRequest(UserConnection user, String serverId) {
    var session = Objects.requireNonNull(user.get(StorableSession.class)).session();
    session.botConnection().joinServerId(serverId, session);
  }
}
