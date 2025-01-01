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
package com.soulfiremc.server.settings.server;

import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.BooleanProperty;
import com.soulfiremc.server.settings.property.ImmutableBooleanProperty;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DevSettings implements SettingsObject {
  private static final String NAMESPACE = "dev";
  public static final BooleanProperty CORE_DEBUG =
    ImmutableBooleanProperty.builder()
      .namespace(NAMESPACE)
      .key("core-debug")
      .uiName("Core debug")
      .description("Enable core code debug logging")
      .defaultValue(false)
      .build();
  public static final BooleanProperty VIA_DEBUG =
    ImmutableBooleanProperty.builder()
      .namespace(NAMESPACE)
      .key("via-debug")
      .uiName("Via debug")
      .description("Enable Via* code debug logging")
      .defaultValue(false)
      .build();
  public static final BooleanProperty NETTY_DEBUG =
    ImmutableBooleanProperty.builder()
      .namespace(NAMESPACE)
      .key("netty-debug")
      .uiName("Netty debug")
      .description("Enable Netty debug logging")
      .defaultValue(false)
      .build();
  public static final BooleanProperty GRPC_DEBUG =
    ImmutableBooleanProperty.builder()
      .namespace(NAMESPACE)
      .key("grpc-debug")
      .uiName("gRPC debug")
      .description("Enable gRPC debug logging")
      .defaultValue(false)
      .build();
  public static final BooleanProperty MCPROTOCOLLIB_DEBUG =
    ImmutableBooleanProperty.builder()
      .namespace(NAMESPACE)
      .key("mcprotocollib-debug")
      .uiName("MCProtocolLib debug")
      .description("Enable MCProtocolLib debug logging")
      .defaultValue(false)
      .build();
}
