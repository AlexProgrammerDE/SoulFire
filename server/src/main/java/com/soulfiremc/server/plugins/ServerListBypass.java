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
import com.soulfiremc.server.api.event.bot.PreBotConnectEvent;
import com.soulfiremc.server.api.event.lifecycle.InstanceSettingsRegistryInitEvent;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.BooleanProperty;
import com.soulfiremc.server.settings.property.MinMaxPropertyLink;
import com.soulfiremc.server.settings.property.Property;
import com.soulfiremc.server.util.TimeUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.lenni0451.lambdaevents.EventHandler;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;

import java.util.concurrent.TimeUnit;

public class ServerListBypass extends InternalPlugin {
  public ServerListBypass() {
    super(new PluginInfo(
      "server-list-bypass",
      "1.0.0",
      "Bypasses server list anti-bots",
      "AlexProgrammerDE",
      "GPL-3.0"
    ));
  }

  @EventHandler
  public static void onPreConnect(PreBotConnectEvent event) {
    var connection = event.connection();
    if (connection.targetState() == ProtocolState.STATUS) {
      return;
    }

    var factory = connection.factory();
    var settingsSource = connection.settingsSource();
    if (!settingsSource.get(ServerListBypassSettings.ENABLED)) {
      return;
    }

    factory.prepareConnectionInternal(ProtocolState.STATUS).connect().join();
    TimeUtil.waitTime(
      settingsSource.getRandom(ServerListBypassSettings.DELAY).getAsLong(),
      TimeUnit.SECONDS);
  }

  @EventHandler
  public void onSettingsRegistryInit(InstanceSettingsRegistryInitEvent event) {
    event.settingsRegistry().addClass(ServerListBypassSettings.class, "Server List Bypass", this, "network");
  }

  @NoArgsConstructor(access = AccessLevel.NONE)
  private static class ServerListBypassSettings implements SettingsObject {
    private static final Property.Builder BUILDER = Property.builder("server-list-bypass");
    public static final BooleanProperty ENABLED =
      BUILDER.ofBoolean(
        "enabled",
        "Enable Server List Bypass",
        "Whether to ping the server list before connecting. (Bypasses anti-bots like EpicGuard)",
        false);
    public static final MinMaxPropertyLink DELAY =
      new MinMaxPropertyLink(
        BUILDER.ofInt(
          "min-delay",
          "Min delay (seconds)",
          "Minimum delay between joining the server",
          1,
          0,
          Integer.MAX_VALUE,
          1),
        BUILDER.ofInt(
          "max-delay",
          "Max delay (seconds)",
          "Maximum delay between joining the server",
          3,
          0,
          Integer.MAX_VALUE,
          1));
  }
}
