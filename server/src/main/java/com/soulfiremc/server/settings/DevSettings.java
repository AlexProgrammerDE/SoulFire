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
package com.soulfiremc.server.settings;

import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.BooleanProperty;
import com.soulfiremc.server.settings.property.Property;
import com.soulfiremc.util.BuiltinSettingsConstants;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DevSettings implements SettingsObject {
  public static final Property.Builder BUILDER =
    Property.builder(BuiltinSettingsConstants.DEV_SETTINGS_ID);
  public static final BooleanProperty CORE_DEBUG =
    BUILDER.ofBoolean(
      "core-debug",
      "Core debug",
      new String[] {"--core-debug"},
      "Enable core code debug logging",
      false);
  public static final BooleanProperty VIA_DEBUG =
    BUILDER.ofBoolean(
      "via-debug",
      "Via debug",
      new String[] {"--via-debug"},
      "Enable Via* code debug logging",
      false);
  public static final BooleanProperty NETTY_DEBUG =
    BUILDER.ofBoolean(
      "netty-debug",
      "Netty debug",
      new String[] {"--netty-debug"},
      "Enable Netty debug logging",
      false);
  public static final BooleanProperty GRPC_DEBUG =
    BUILDER.ofBoolean(
      "grpc-debug",
      "gRPC debug",
      new String[] {"--grpc-debug"},
      "Enable gRPC debug logging",
      false);
}
