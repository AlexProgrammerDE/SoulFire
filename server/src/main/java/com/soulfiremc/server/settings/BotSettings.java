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
import com.soulfiremc.server.settings.property.ComboProperty;
import com.soulfiremc.server.settings.property.IntProperty;
import com.soulfiremc.server.settings.property.MinMaxPropertyLink;
import com.soulfiremc.server.settings.property.Property;
import com.soulfiremc.server.settings.property.StringProperty;
import com.soulfiremc.server.viaversion.SFVersionConstants;
import com.soulfiremc.util.BuiltinSettingsConstants;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.protocol.version.VersionType;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.raphimc.vialoader.util.ProtocolVersionList;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BotSettings implements SettingsObject {
  public static final Function<String, ProtocolVersion> PROTOCOL_VERSION_PARSER =
    version -> {
      var split = version.split("\\|");
      if (split.length == 1) {
        return ProtocolVersion.getClosest(split[0]);
      }

      return ProtocolVersion.getProtocol(VersionType.valueOf(split[0]), Integer.parseInt(split[1]));
    };

  private static final Property.Builder BUILDER =
    Property.builder(BuiltinSettingsConstants.BOT_SETTINGS_ID);
  public static final StringProperty ADDRESS =
    BUILDER.ofString(
      "address",
      "Address",
      new String[] {"--address"},
      "Address to connect to",
      "127.0.0.1:25565");
  public static final IntProperty AMOUNT =
    BUILDER.ofInt(
      "amount",
      "Amount",
      new String[] {"--amount"},
      "Amount of bots to connect",
      1,
      1,
      Integer.MAX_VALUE,
      1);
  public static final MinMaxPropertyLink JOIN_DELAY =
    new MinMaxPropertyLink(
      BUILDER.ofInt(
        "join-min-delay",
        "Min Join Delay (ms)",
        new String[] {"--join-min-delay"},
        "Minimum delay between joins in milliseconds",
        1000,
        0,
        Integer.MAX_VALUE,
        1),
      BUILDER.ofInt(
        "join-max-delay",
        "Max Join Delay (ms)",
        new String[] {"--join-max-delay"},
        "Maximum delay between joins in milliseconds",
        3000,
        0,
        Integer.MAX_VALUE,
        1));
  public static final ComboProperty PROTOCOL_VERSION =
    BUILDER.ofCombo(
      "protocol-version",
      "Protocol Version",
      new String[] {"--protocol-version"},
      "Minecraft protocol version to use",
      getProtocolVersionOptions(),
      getLatestProtocolVersionIndex());
  public static final IntProperty READ_TIMEOUT =
    BUILDER.ofInt(
      "read-timeout",
      "Read Timeout",
      new String[] {"--read-timeout"},
      "Read timeout in seconds",
      30,
      0,
      Integer.MAX_VALUE,
      1);
  public static final IntProperty WRITE_TIMEOUT =
    BUILDER.ofInt(
      "write-timeout",
      "Write Timeout",
      new String[] {"--write-timeout"},
      "Write timeout in seconds",
      0,
      0,
      Integer.MAX_VALUE,
      1);
  public static final IntProperty CONNECT_TIMEOUT =
    BUILDER.ofInt(
      "connect-timeout",
      "Connect Timeout",
      new String[] {"--connect-timeout"},
      "Connect timeout in seconds",
      30,
      0,
      Integer.MAX_VALUE,
      1);
  public static final BooleanProperty RESOLVE_SRV =
    BUILDER.ofBoolean(
      "resolve-srv",
      "Resolve SRV",
      new String[] {"--resolve-srv"},
      "Try to resolve SRV records from the address",
      true);
  public static final IntProperty CONCURRENT_CONNECTS =
    BUILDER.ofInt(
      "concurrent-connects",
      "Concurrent Connects",
      new String[] {"--concurrent-connects"},
      "Max amount of bots attempting to connect at once",
      1,
      1,
      Integer.MAX_VALUE,
      1);

  private static ComboProperty.ComboOption[] getProtocolVersionOptions() {
    return ProtocolVersionList.getProtocolsNewToOld().stream()
      .map(version -> new ComboProperty.ComboOption("%s|%d".formatted(version.getVersionType().name(), version.getOriginalVersion()), version.getName()))
      .toArray(ComboProperty.ComboOption[]::new);
  }

  private static int getLatestProtocolVersionIndex() {
    return ProtocolVersionList.getProtocolsNewToOld()
      .indexOf(SFVersionConstants.CURRENT_PROTOCOL_VERSION);
  }
}
