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
package com.soulfiremc.server.plugins;

import com.soulfiremc.server.api.InternalPlugin;
import com.soulfiremc.server.api.PluginInfo;
import com.soulfiremc.server.api.event.bot.SFPacketSendingEvent;
import com.soulfiremc.server.api.event.lifecycle.InstanceSettingsRegistryInitEvent;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.lenni0451.lambdaevents.EventHandler;
import org.geysermc.mcprotocollib.protocol.packet.handshake.serverbound.ClientIntentionPacket;
import org.pf4j.Extension;

@Extension
public class FakeVirtualHost extends InternalPlugin {
  public FakeVirtualHost() {
    super(new PluginInfo(
      "fake-virtual-host",
      "1.0.0",
      "Fakes the virtual host",
      "AlexProgrammerDE",
      "GPL-3.0",
      "https://soulfiremc.com"
    ));
  }

  @EventHandler
  public static void onPacket(SFPacketSendingEvent event) {
    if (event.packet() instanceof ClientIntentionPacket intentionPacket) {
      var settingsSource = event.connection().settingsSource();

      if (!settingsSource.get(FakeVirtualHostSettings.ENABLED)) {
        return;
      }

      event.packet(
        intentionPacket
          .withHostname(settingsSource.get(FakeVirtualHostSettings.HOSTNAME))
          .withPort(settingsSource.get(FakeVirtualHostSettings.PORT)));
    }
  }

  @EventHandler
  public void onSettingsRegistryInit(InstanceSettingsRegistryInitEvent event) {
    event.settingsRegistry().addPluginPage(FakeVirtualHostSettings.class, "Fake Virtual Host", this, "globe", FakeVirtualHostSettings.ENABLED);
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  private static class FakeVirtualHostSettings implements SettingsObject {
    private static final String NAMESPACE = "fake-virtual-host";
    public static final BooleanProperty ENABLED =
      ImmutableBooleanProperty.builder()
        .namespace(NAMESPACE)
        .key("enabled")
        .uiName("Fake virtual host")
        .description("Whether to fake the virtual host or not")
        .defaultValue(false)
        .build();
    public static final StringProperty HOSTNAME =
      ImmutableStringProperty.builder()
        .namespace(NAMESPACE)
        .key("hostname")
        .uiName("Hostname")
        .description("The hostname to fake")
        .defaultValue("localhost")
        .build();
    public static final IntProperty PORT =
      ImmutableIntProperty.builder()
        .namespace(NAMESPACE)
        .key("port")
        .uiName("Port")
        .description("The port to fake")
        .defaultValue(25565)
        .minValue(1)
        .maxValue(65535)
        .thousandSeparator(false)
        .build();
  }
}
