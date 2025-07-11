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
public final class DevSettings implements SettingsObject {
  private static final String NAMESPACE = "dev";
  public static final BooleanProperty SOULFIRE_DEBUG =
    ImmutableBooleanProperty.builder()
      .namespace(NAMESPACE)
      .key("soulfire-debug")
      .uiName("SoulFire debug")
      .description("Enable SoulFire debug logging")
      .defaultValue(false)
      .build();
  public static final BooleanProperty MINECRAFT_DEBUG =
    ImmutableBooleanProperty.builder()
      .namespace(NAMESPACE)
      .key("minecraft-debug")
      .uiName("Minecraft debug")
      .description("Enable Minecraft debug logging")
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
  public static final BooleanProperty HIBERNATE_DEBUG =
    ImmutableBooleanProperty.builder()
      .namespace(NAMESPACE)
      .key("hibernate-debug")
      .uiName("Hibernate debug")
      .description("Enable Hibernate debug logging")
      .defaultValue(false)
      .build();
  public static final BooleanProperty VIA_DEBUG =
    ImmutableBooleanProperty.builder()
      .namespace(NAMESPACE)
      .key("via-debug")
      .uiName("Via debug")
      .description("Enable Via* debug logging")
      .defaultValue(false)
      .build();
  public static final BooleanProperty OTHER_DEBUG =
    ImmutableBooleanProperty.builder()
      .namespace(NAMESPACE)
      .key("other-debug")
      .uiName("Other debug")
      .description("Enable other debug logging")
      .defaultValue(false)
      .build();
}
