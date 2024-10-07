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
package com.soulfiremc.server.protocol;

import com.viaversion.viaversion.api.connection.UserConnection;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import net.kyori.adventure.key.Key;
import org.geysermc.mcprotocollib.network.Flag;
import org.geysermc.mcprotocollib.network.crypt.EncryptionConfig;

public class SFProtocolConstants {
  public static final Flag<EncryptionConfig> VL_ENCRYPTION_CONFIG = new Flag<>("vl-encryption-config", EncryptionConfig.class);
  public static final Flag<UserConnection> VIA_USER_CONNECTION = new Flag<>("via-user-connection", UserConnection.class);
  public static final Flag<GlobalTrafficShapingHandler> TRAFFIC_HANDLER = new Flag<>("netty-traffic-handler", GlobalTrafficShapingHandler.class);
  public static final Key BRAND_PAYLOAD_KEY = Key.key("minecraft:brand");
  public static final Key REGISTER_KEY = Key.key("minecraft:register");
  public static final Key UNREGISTER_KEY = Key.key("minecraft:unregister");

  private SFProtocolConstants() {}
}
