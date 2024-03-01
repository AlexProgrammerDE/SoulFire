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

import com.soulfiremc.server.protocol.SFProtocolConstants;
import com.soulfiremc.server.viaversion.StorableSession;
import com.viaversion.viaversion.api.connection.UserConnection;
import java.util.Objects;
import javax.crypto.SecretKey;
import net.raphimc.vialegacy.protocols.release.protocol1_7_2_5to1_6_4.providers.EncryptionProvider;

public class SFViaEncryptionProvider extends EncryptionProvider {
  @Override
  public void enableDecryption(UserConnection user) {
    var session = Objects.requireNonNull(user.get(StorableSession.class)).session();
    SecretKey key = session.getFlag(SFProtocolConstants.ENCRYPTION_SECRET_KEY);
    Objects.requireNonNull(key, "Key is null!");
    session.setFlag(SFProtocolConstants.ENCRYPTION_SECRET_KEY, null);

    session.enableJavaEncryption(key);
  }
}
