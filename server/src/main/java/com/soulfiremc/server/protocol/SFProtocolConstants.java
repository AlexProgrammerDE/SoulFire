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

import com.soulfiremc.server.data.ResourceKey;

public class SFProtocolConstants {
  public static final String ENCRYPTION_SECRET_KEY = "encryption-secret-key";
  public static final String VIA_USER_CONNECTION = "via-user-connection";
  public static final String TRAFFIC_HANDLER = "netty-traffic-handler";
  public static final ResourceKey BRAND_PAYLOAD_KEY = ResourceKey.fromString("minecraft:brand");
  public static final ResourceKey REGISTER_KEY = ResourceKey.fromString("minecraft:register");
  public static final ResourceKey UNREGISTER_KEY = ResourceKey.fromString("minecraft:unregister");

  private SFProtocolConstants() {}
}
