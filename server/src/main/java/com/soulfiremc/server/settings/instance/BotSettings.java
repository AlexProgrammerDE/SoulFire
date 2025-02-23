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
package com.soulfiremc.server.settings.instance;

import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.*;
import com.soulfiremc.server.viaversion.SFVersionConstants;
import com.viaversion.vialoader.util.ProtocolVersionList;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.protocol.version.VersionType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.function.Function;

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
  private static final String NAMESPACE = "bot";
  public static final StringProperty ADDRESS =
    ImmutableStringProperty.builder()
      .namespace(NAMESPACE)
      .key("address")
      .uiName("Address")
      .description("Address to connect to")
      .defaultValue("127.0.0.1:25565")
      .build();
  public static final IntProperty AMOUNT =
    ImmutableIntProperty.builder()
      .namespace(NAMESPACE)
      .key("amount")
      .uiName("Amount")
      .description("Amount of bots to connect")
      .defaultValue(1)
      .minValue(1)
      .maxValue(Integer.MAX_VALUE)
      .build();
  public static final MinMaxProperty JOIN_DELAY = ImmutableMinMaxProperty.builder()
    .namespace(NAMESPACE)
    .key("join-delay")
    .minValue(0)
    .maxValue(Integer.MAX_VALUE)
    .minEntry(ImmutableMinMaxPropertyEntry.builder()
      .uiName("Min Join Delay (ms)")
      .description("Minimum delay between joins in milliseconds")
      .defaultValue(1000)
      .build())
    .maxEntry(ImmutableMinMaxPropertyEntry.builder()
      .uiName("Max Join Delay (ms)")
      .description("Maximum delay between joins in milliseconds")
      .defaultValue(3000)
      .build())
    .build();
  public static final ComboProperty PROTOCOL_VERSION =
    ImmutableComboProperty.builder()
      .namespace(NAMESPACE)
      .key("protocol-version")
      .uiName("Protocol Version")
      .description("Minecraft protocol version to use")
      .defaultValue(getLatestProtocolVersionId())
      .addOptions(getProtocolVersionOptions())
      .build();
  public static final IntProperty READ_TIMEOUT =
    ImmutableIntProperty.builder()
      .namespace(NAMESPACE)
      .key("read-timeout")
      .uiName("Read Timeout")
      .description("Read timeout in seconds")
      .defaultValue(30)
      .minValue(0)
      .maxValue(Integer.MAX_VALUE)
      .build();
  public static final IntProperty WRITE_TIMEOUT =
    ImmutableIntProperty.builder()
      .namespace(NAMESPACE)
      .key("write-timeout")
      .uiName("Write Timeout")
      .description("Write timeout in seconds")
      .defaultValue(0)
      .minValue(0)
      .maxValue(Integer.MAX_VALUE)
      .build();
  public static final IntProperty CONNECT_TIMEOUT =
    ImmutableIntProperty.builder()
      .namespace(NAMESPACE)
      .key("connect-timeout")
      .uiName("Connect Timeout")
      .description("Connect timeout in seconds")
      .defaultValue(30)
      .minValue(0)
      .maxValue(Integer.MAX_VALUE)
      .build();
  public static final BooleanProperty RESOLVE_SRV =
    ImmutableBooleanProperty.builder()
      .namespace(NAMESPACE)
      .key("resolve-srv")
      .uiName("Resolve SRV")
      .description("Try to resolve SRV records from the address")
      .defaultValue(true)
      .build();
  public static final IntProperty CONCURRENT_CONNECTS =
    ImmutableIntProperty.builder()
      .namespace(NAMESPACE)
      .key("concurrent-connects")
      .uiName("Concurrent Connects")
      .description("Max amount of bots attempting to connect at once")
      .defaultValue(1)
      .minValue(1)
      .maxValue(Integer.MAX_VALUE)
      .build();
  public static final BooleanProperty RESTORE_ON_REBOOT =
    ImmutableBooleanProperty.builder()
      .namespace(NAMESPACE)
      .key("restore-on-reboot")
      .uiName("Restore on Reboot")
      .description("""
        Whether the attack should be restored after a reboot of the SoulFire machine.
        If turned off, the attack will not be restored after a reboot.""")
      .defaultValue(true)
      .build();

  private static String formatVersion(ProtocolVersion version) {
    return "%s|%d".formatted(version.getVersionType().name(), version.getOriginalVersion());
  }

  private static ComboProperty.ComboOption[] getProtocolVersionOptions() {
    return ProtocolVersionList.getProtocolsNewToOld().stream()
      .map(version -> new ComboProperty.ComboOption(formatVersion(version), version.getName()))
      .toArray(ComboProperty.ComboOption[]::new);
  }

  private static String getLatestProtocolVersionId() {
    return formatVersion(SFVersionConstants.CURRENT_PROTOCOL_VERSION);
  }
}
