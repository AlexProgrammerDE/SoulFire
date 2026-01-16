/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.settings.instance;

import com.soulfiremc.server.settings.lib.BotSettingsSource;
import com.soulfiremc.server.settings.lib.InstanceSettingsSource;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.*;
import com.viaversion.viaaprilfools.api.AprilFoolsProtocolVersion;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.vialoader.util.ProtocolVersionList;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.protocol.version.VersionType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BotSettings implements SettingsObject {
  public static final Function<String, ProtocolVersion> PROTOCOL_VERSION_PARSER =
    version -> {
      var split = version.split("\\|");
      if (split.length == 1) {
        return ProtocolVersion.getClosest(split[0]);
      }

      return ProtocolVersion.getProtocol(VersionType.valueOf(split[0]), Integer.parseInt(split[1]));
    };
  private static final String NAMESPACE = "bot";
  public static final StringProperty<InstanceSettingsSource> ADDRESS =
    ImmutableStringProperty.<InstanceSettingsSource>builder()
      .namespace(NAMESPACE)
      .key("address")
      .uiName("Address")
      .description("Address to connect to")
      .defaultValue("127.0.0.1:25565")
      .build();
  public static final IntProperty<InstanceSettingsSource> AMOUNT =
    ImmutableIntProperty.<InstanceSettingsSource>builder()
      .namespace(NAMESPACE)
      .key("amount")
      .uiName("Amount")
      .description("Amount of bots to connect")
      .defaultValue(1)
      .minValue(1)
      .maxValue(Integer.MAX_VALUE)
      .build();
  public static final MinMaxProperty<InstanceSettingsSource> JOIN_DELAY = ImmutableMinMaxProperty.<InstanceSettingsSource>builder()
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
  public static final ComboProperty<InstanceSettingsSource> PROTOCOL_VERSION =
    ImmutableComboProperty.<InstanceSettingsSource>builder()
      .namespace(NAMESPACE)
      .key("protocol-version")
      .uiName("Protocol Version")
      .description("Minecraft protocol version to use")
      .defaultValue(getLatestProtocolVersionId())
      .addOptions(getProtocolVersionOptions())
      .build();
  public static final IntProperty<BotSettingsSource> READ_TIMEOUT =
    ImmutableIntProperty.<BotSettingsSource>builder()
      .namespace(NAMESPACE)
      .key("read-timeout")
      .uiName("Read Timeout")
      .description("Read timeout in seconds")
      .defaultValue(30)
      .minValue(0)
      .maxValue(Integer.MAX_VALUE)
      .build();
  public static final IntProperty<BotSettingsSource> WRITE_TIMEOUT =
    ImmutableIntProperty.<BotSettingsSource>builder()
      .namespace(NAMESPACE)
      .key("write-timeout")
      .uiName("Write Timeout")
      .description("Write timeout in seconds")
      .defaultValue(0)
      .minValue(0)
      .maxValue(Integer.MAX_VALUE)
      .build();
  public static final IntProperty<BotSettingsSource> CONNECT_TIMEOUT =
    ImmutableIntProperty.<BotSettingsSource>builder()
      .namespace(NAMESPACE)
      .key("connect-timeout")
      .uiName("Connect Timeout")
      .description("Connect timeout in seconds")
      .defaultValue(30)
      .minValue(0)
      .maxValue(Integer.MAX_VALUE)
      .build();
  public static final BooleanProperty<InstanceSettingsSource> RESOLVE_SRV =
    ImmutableBooleanProperty.<InstanceSettingsSource>builder()
      .namespace(NAMESPACE)
      .key("resolve-srv")
      .uiName("Resolve SRV")
      .description("Try to resolve SRV records from the address")
      .defaultValue(true)
      .build();
  public static final IntProperty<InstanceSettingsSource> CONCURRENT_CONNECTS =
    ImmutableIntProperty.<InstanceSettingsSource>builder()
      .namespace(NAMESPACE)
      .key("concurrent-connects")
      .uiName("Concurrent Connects")
      .description("Max amount of bots attempting to connect at once")
      .defaultValue(1)
      .minValue(1)
      .maxValue(Integer.MAX_VALUE)
      .build();
  public static final BooleanProperty<InstanceSettingsSource> RESTORE_ON_REBOOT =
    ImmutableBooleanProperty.<InstanceSettingsSource>builder()
      .namespace(NAMESPACE)
      .key("restore-on-reboot")
      .uiName("Restore on Reboot")
      .description("""
        Whether the attack should be restored after a reboot of the SoulFire machine.
        If turned off, the attack will not be restored after a reboot.""")
      .defaultValue(true)
      .build();
  public static final BooleanProperty<BotSettingsSource> IGNORE_PACKET_HANDLING_ERRORS =
    ImmutableBooleanProperty.<BotSettingsSource>builder()
      .namespace(NAMESPACE)
      .key("ignore-packet-handling-errors")
      .uiName("Ignore Packet Handling Errors")
      .description("""
        Sometimes a bot fails to process a packet. When that happens it disconnects due to "packet errors".
        When this option is turned on, SoulFire will ignore errors during the packet handling process and keep bots connected.
        This might cause bots to have inconsistent world state though, so it could be detected by the server.""")
      .defaultValue(true)
      .build();

  private static String formatVersion(ProtocolVersion version) {
    return "%s|%d".formatted(version.getVersionType().name(), version.getOriginalVersion());
  }

  public static List<ProtocolVersion> getAvailableProtocolVersions() {
    return ProtocolVersionList.getProtocolsNewToOld()
      .stream()
      .filter(version -> version != ProtocolTranslator.AUTO_DETECT_PROTOCOL)
      .toList();
  }

  private static ComboProperty.ComboOption[] getProtocolVersionOptions() {
    return getAvailableProtocolVersions()
      .stream()
      .map(version -> new ComboProperty.ComboOption(
        formatVersion(version),
        version.getName(),
        switch (version.getVersionType()) {
          case CLASSIC -> "archive";
          case ALPHA_INITIAL, ALPHA_LATER -> "flask-conical";
          case BETA_INITIAL, BETA_LATER -> "test-tube";
          case RELEASE_INITIAL, RELEASE -> "box";
          case SPECIAL -> {
            if (BedrockProtocolVersion.PROTOCOLS.contains(version)) {
              yield "brick-wall";
            } else if (AprilFoolsProtocolVersion.PROTOCOLS.contains(version)) {
              yield "ghost";
            } else if (LegacyProtocolVersion.PROTOCOLS.contains(version)) {
              yield "archive";
            } else {
              throw new RuntimeException("Unknown version: " + version);
            }
          }
        },
        Stream.concat(
          version.getIncludedVersions().stream(),
          Stream.of(String.valueOf(version.getOriginalVersion()))
        ).toList()
      ))
      .toArray(ComboProperty.ComboOption[]::new);
  }

  private static String getLatestProtocolVersionId() {
    return formatVersion(ProtocolTranslator.NATIVE_VERSION);
  }
}
