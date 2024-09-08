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

import com.soulfiremc.server.InstanceManager;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.api.InternalPlugin;
import com.soulfiremc.server.api.PluginHelper;
import com.soulfiremc.server.api.PluginInfo;
import com.soulfiremc.server.api.event.attack.AttackTickEvent;
import com.soulfiremc.server.api.event.bot.BotDisconnectedEvent;
import com.soulfiremc.server.api.event.lifecycle.InstanceSettingsRegistryInitEvent;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.BooleanProperty;
import com.soulfiremc.server.settings.property.MinMaxPropertyLink;
import com.soulfiremc.server.settings.property.Property;
import com.soulfiremc.server.util.RandomUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.lenni0451.lambdaevents.EventHandler;

import java.util.List;
import java.util.concurrent.TimeUnit;

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
    PluginHelper.registerAttackEventConsumer(soulFireServer, AttackTickEvent.class, this::onAttackTick);
  }

  public void onAttackTick(AttackTickEvent event) {
    var instanceManager = event.instanceManager();
    for (var entries : List.copyOf(instanceManager.botConnections().entrySet())) {
      var bot = entries.getValue();
      if (!bot.session().isDisconnected()) {
        continue;
      }

      var settingsSource = bot.settingsSource();
      if (!settingsSource.get(AutoReconnectSettings.ENABLED)) {
        continue;
      }

      // Ensure this bot is not reconnected twice
      instanceManager.botConnections().remove(entries.getKey());

      instanceManager
        .scheduler()
        .schedule(
          () -> {
            var eventLoopGroup = bot.session().eventLoopGroup();
            if (eventLoopGroup.isShuttingDown()
              || eventLoopGroup.isShutdown()
              || eventLoopGroup.isTerminated()) {
              return;
            }

            bot.gracefulDisconnect();
            var newConnection = bot.factory().prepareConnection();

            instanceManager
              .botConnections()
              .put(bot.connectionId(), newConnection);

            newConnection.connect();
          },
          RandomUtil.getRandomInt(
            settingsSource.get(AutoReconnectSettings.DELAY.min()),
            settingsSource.get(AutoReconnectSettings.DELAY.max())),
          TimeUnit.SECONDS);
    }
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  private static class AutoReconnectSettings implements SettingsObject {
    private static final Property.Builder BUILDER = Property.builder("auto-reconnect");
    public static final BooleanProperty ENABLED =
      BUILDER.ofBoolean(
        "enabled",
        "Enable Auto Reconnect",
        "Reconnect a bot when it times out/is kicked",
        true);
    public static final MinMaxPropertyLink DELAY =
      new MinMaxPropertyLink(
        BUILDER.ofInt(
          "min-delay",
          "Min delay (seconds)",
          "Minimum delay between reconnects",
          1,
          0,
          Integer.MAX_VALUE,
          1),
        BUILDER.ofInt(
          "max-delay",
          "Max delay (seconds)",
          "Maximum delay between reconnects",
          5,
          0,
          Integer.MAX_VALUE,
          1));
  }
}
