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
package com.soulfiremc.server.plugins;

import com.soulfiremc.server.api.InternalPlugin;
import com.soulfiremc.server.api.InternalPluginClass;
import com.soulfiremc.server.api.PluginInfo;
import com.soulfiremc.server.api.event.attack.AttackBotRemoveEvent;
import com.soulfiremc.server.api.event.lifecycle.BotSettingsRegistryInitEvent;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.lib.SettingsSource;
import com.soulfiremc.server.settings.property.*;
import com.soulfiremc.server.util.TimeUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.Component;
import net.lenni0451.lambdaevents.EventHandler;

import java.util.concurrent.TimeUnit;

@InternalPluginClass
public final class AutoReconnect extends InternalPlugin {
  public AutoReconnect() {
    super(new PluginInfo(
      "auto-reconnect",
      "1.0.0",
      "Automatically reconnects bots when they time out or are kicked",
      "AlexProgrammerDE",
      "AGPL-3.0",
      "https://soulfiremc.com"
    ));
  }

  @EventHandler
  public void onSettingsRegistryInit(BotSettingsRegistryInitEvent event) {
    event.settingsPageRegistry().addPluginPage(AutoReconnectSettings.class, "Auto Reconnect", this, "refresh-ccw", AutoReconnectSettings.ENABLED);
  }

  @EventHandler
  public void onAttackBotRemove(AttackBotRemoveEvent event) {
    var instanceManager = event.instanceManager();
    var bot = event.botConnection();
    var settingsSource = bot.settingsSource();
    if (!settingsSource.get(AutoReconnectSettings.ENABLED) || bot.isStatusPing()) {
      return;
    }

    instanceManager
      .scheduler()
      .schedule(
        () -> {
          TimeUtil.waitCondition(() -> instanceManager.attackLifecycle().isPaused());

          var eventLoopGroup = bot.eventLoopGroup();
          if (eventLoopGroup.isShuttingDown()
            || eventLoopGroup.isShutdown()
            || eventLoopGroup.isTerminated()) {
            return;
          }

          if (instanceManager.attackLifecycle().isStoppedOrStopping()) {
            return;
          }

          bot.disconnect(Component.text("Reconnecting..."));
          var newConnection = bot.factory().prepareConnection(false);

          instanceManager.storeNewBot(newConnection);

          newConnection.connect();
        },
        settingsSource.getRandom(AutoReconnectSettings.DELAY).getAsLong(),
        TimeUnit.SECONDS);
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  private static class AutoReconnectSettings implements SettingsObject {
    private static final String NAMESPACE = "auto-reconnect";
    public static final BooleanProperty<SettingsSource.Bot> ENABLED =
      ImmutableBooleanProperty.<SettingsSource.Bot>builder()
        .sourceType(SettingsSource.Bot.INSTANCE)
        .namespace(NAMESPACE)
        .key("enabled")
        .uiName("Enable Auto Reconnect")
        .description("Reconnect a bot when it times out/is kicked")
        .defaultValue(true)
        .build();
    public static final MinMaxProperty<SettingsSource.Bot> DELAY =
      ImmutableMinMaxProperty.<SettingsSource.Bot>builder()
        .sourceType(SettingsSource.Bot.INSTANCE)
        .namespace(NAMESPACE)
        .key("delay")
        .minValue(0)
        .maxValue(Integer.MAX_VALUE)
        .minEntry(ImmutableMinMaxPropertyEntry.builder()
          .uiName("Min delay (seconds)")
          .description("Minimum delay between reconnects")
          .defaultValue(1)
          .build())
        .maxEntry(ImmutableMinMaxPropertyEntry.builder()
          .uiName("Max delay (seconds)")
          .description("Maximum delay between reconnects")
          .defaultValue(5)
          .build())
        .build();
  }
}
