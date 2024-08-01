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

import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.api.InternalPlugin;
import com.soulfiremc.server.api.PluginHelper;
import com.soulfiremc.server.api.PluginInfo;
import com.soulfiremc.server.api.event.bot.BotDisconnectedEvent;
import com.soulfiremc.server.api.event.lifecycle.InstanceSettingsRegistryInitEvent;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.BooleanProperty;
import com.soulfiremc.server.settings.property.MinMaxPropertyLink;
import com.soulfiremc.server.settings.property.Property;
import com.soulfiremc.server.util.RandomUtil;
import java.util.concurrent.TimeUnit;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.lenni0451.lambdaevents.EventHandler;

public class AutoReconnect implements InternalPlugin {
  public static final PluginInfo PLUGIN_INFO = new PluginInfo(
    "auto-reconnect",
    "1.0.0",
    "Automatically reconnects bots when they time out or are kicked",
    "AlexProgrammerDE",
    "GPL-3.0"
  );

  @EventHandler
  public static void onSettingsRegistryInit(InstanceSettingsRegistryInitEvent event) {
    event.settingsRegistry().addClass(AutoReconnectSettings.class, "Auto Reconnect", PLUGIN_INFO);
  }

  @Override
  public PluginInfo pluginInfo() {
    return PLUGIN_INFO;
  }

  @Override
  public void onServer(SoulFireServer soulFireServer) {
    soulFireServer.registerListeners(AutoReconnect.class);
    PluginHelper.registerBotEventConsumer(soulFireServer, BotDisconnectedEvent.class, this::onDisconnect);
  }

  public void onDisconnect(BotDisconnectedEvent event) {
    var connection = event.connection();
    var settingsHolder = connection.settingsHolder();
    if (!settingsHolder.get(AutoReconnectSettings.ENABLED)
      || connection.instanceManager().attackState().isInactive()) {
      return;
    }

    connection
      .instanceManager()
      .scheduler()
      .schedule(
        () -> {
          var eventLoopGroup = connection.session().eventLoopGroup();
          if (eventLoopGroup.isShuttingDown()
            || eventLoopGroup.isShutdown()
            || eventLoopGroup.isTerminated()) {
            return;
          }

          connection.gracefulDisconnect();
          var newConnection = connection.factory().prepareConnection();

          connection
            .instanceManager()
            .botConnections()
            .put(connection.connectionId(), newConnection);

          newConnection.connect();
        },
        RandomUtil.getRandomInt(
          settingsHolder.get(AutoReconnectSettings.DELAY.min()),
          settingsHolder.get(AutoReconnectSettings.DELAY.max())),
        TimeUnit.SECONDS);
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  private static class AutoReconnectSettings implements SettingsObject {
    private static final Property.Builder BUILDER = Property.builder("auto-reconnect");
    public static final BooleanProperty ENABLED =
      BUILDER.ofBoolean(
        "enabled",
        "Enable Auto Reconnect",
        new String[] {"--auto-reconnect"},
        "Reconnect a bot when it times out/is kicked",
        true);
    public static final MinMaxPropertyLink DELAY =
      new MinMaxPropertyLink(
        BUILDER.ofInt(
          "min-delay",
          "Min delay (seconds)",
          new String[] {"--reconnect-min-delay"},
          "Minimum delay between reconnects",
          1,
          0,
          Integer.MAX_VALUE,
          1),
        BUILDER.ofInt(
          "max-delay",
          "Max delay (seconds)",
          new String[] {"--reconnect-max-delay"},
          "Maximum delay between reconnects",
          5,
          0,
          Integer.MAX_VALUE,
          1));
  }
}
