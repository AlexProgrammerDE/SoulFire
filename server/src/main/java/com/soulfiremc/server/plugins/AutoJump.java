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
import com.soulfiremc.server.api.event.bot.BotJoinedEvent;
import com.soulfiremc.server.api.event.lifecycle.InstanceSettingsRegistryInitEvent;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.BooleanProperty;
import com.soulfiremc.server.settings.property.MinMaxPropertyLink;
import com.soulfiremc.server.settings.property.Property;
import java.util.concurrent.TimeUnit;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.lenni0451.lambdaevents.EventHandler;

public class AutoJump implements InternalPlugin {
  public static final PluginInfo PLUGIN_INFO = new PluginInfo(
    "auto-jump",
    "1.0.0",
    "Automatically jumps randomly",
    "AlexProgrammerDE",
    "GPL-3.0"
  );

  public static void onJoined(BotJoinedEvent event) {
    var connection = event.connection();
    var settingsHolder = connection.settingsHolder();
    if (!settingsHolder.get(AutoJumpSettings.ENABLED)) {
      return;
    }

    connection.scheduler().scheduleWithRandomDelay(
      () -> {
        var dataManager = connection.dataManager();
        var clientEntity = dataManager.clientEntity();
        if (clientEntity != null
          && clientEntity.level().isChunkLoaded(clientEntity.blockPos())
          && clientEntity.onGround()) {
          connection.logger().debug("[AutoJump] Jumping!");
          clientEntity.jump();
        }
      },
      settingsHolder.get(AutoJumpSettings.DELAY.min()),
      settingsHolder.get(AutoJumpSettings.DELAY.max()),
      TimeUnit.SECONDS);
  }

  @EventHandler
  public static void onSettingsRegistryInit(InstanceSettingsRegistryInitEvent event) {
    event.settingsRegistry().addClass(AutoJumpSettings.class, "Auto Jump", PLUGIN_INFO);
  }

  @Override
  public PluginInfo pluginInfo() {
    return PLUGIN_INFO;
  }

  @Override
  public void onServer(SoulFireServer soulFireServer) {
    soulFireServer.registerListeners(AutoJump.class);
    PluginHelper.registerBotEventConsumer(soulFireServer, BotJoinedEvent.class, AutoJump::onJoined);
  }

  @NoArgsConstructor(access = AccessLevel.NONE)
  private static class AutoJumpSettings implements SettingsObject {
    private static final Property.Builder BUILDER = Property.builder("auto-jump");
    public static final BooleanProperty ENABLED =
      BUILDER.ofBoolean(
        "enabled",
        "Enable Auto Jump",
        new String[] {"--auto-jump"},
        "Attempt to jump automatically in random intervals",
        false);
    public static final MinMaxPropertyLink DELAY =
      new MinMaxPropertyLink(
        BUILDER.ofInt(
          "min-delay",
          "Min delay (seconds)",
          new String[] {"--jump-min-delay"},
          "Minimum delay between jumps",
          2,
          0,
          Integer.MAX_VALUE,
          1),
        BUILDER.ofInt(
          "max-delay",
          "Max delay (seconds)",
          new String[] {"--jump-max-delay"},
          "Maximum delay between jumps",
          5,
          0,
          Integer.MAX_VALUE,
          1));
  }
}
