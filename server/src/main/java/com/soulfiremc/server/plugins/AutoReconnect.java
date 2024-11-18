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
import com.soulfiremc.server.api.event.attack.AttackTickEvent;
import com.soulfiremc.server.api.event.lifecycle.InstanceSettingsRegistryInitEvent;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.BooleanProperty;
import com.soulfiremc.server.settings.property.ImmutableBooleanProperty;
import com.soulfiremc.server.settings.property.ImmutableMinMaxProperty;
import com.soulfiremc.server.settings.property.MinMaxProperty;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.lenni0451.lambdaevents.EventHandler;
import org.pf4j.Extension;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Extension
public class AutoReconnect extends InternalPlugin {
  public AutoReconnect() {
    super(new PluginInfo(
      "auto-reconnect",
      "1.0.0",
      "Automatically reconnects bots when they time out or are kicked",
      "AlexProgrammerDE",
      "GPL-3.0",
      "https://soulfiremc.com"
    ));
  }

  @EventHandler
  public void onSettingsRegistryInit(InstanceSettingsRegistryInitEvent event) {
    event.settingsRegistry().addClass(AutoReconnectSettings.class, "Auto Reconnect", this, "refresh-ccw");
  }

  @EventHandler
  public void onAttackTick(AttackTickEvent event) {
    var instanceManager = event.instanceManager();
    for (var entries : List.copyOf(instanceManager.botConnections().entrySet())) {
      var bot = entries.getValue();
      if (!bot.session().isDisconnected() || bot.explicitlyShutdown()) {
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
          settingsSource.getRandom(AutoReconnectSettings.DELAY).getAsLong(),
          TimeUnit.SECONDS);
    }
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  private static class AutoReconnectSettings implements SettingsObject {
    private static final String NAMESPACE = "auto-reconnect";
    public static final BooleanProperty ENABLED =
      ImmutableBooleanProperty.builder()
        .namespace(NAMESPACE)
        .key("enabled")
        .uiName("Enable Auto Reconnect")
        .description("Reconnect a bot when it times out/is kicked")
        .defaultValue(true)
        .build();
    public static final MinMaxProperty DELAY =
      ImmutableMinMaxProperty.builder()
        .namespace(NAMESPACE)
        .key("delay")
        .minUiName("Min delay (seconds)")
        .maxUiName("Max delay (seconds)")
        .minDescription("Minimum delay between reconnects")
        .maxDescription("Maximum delay between reconnects")
        .minDefaultValue(1)
        .maxDefaultValue(5)
        .minValue(0)
        .maxValue(Integer.MAX_VALUE)
        .stepValue(1)
        .build();
  }
}
