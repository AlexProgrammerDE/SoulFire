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
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.pathfinding.execution.PathExecutor;
import com.soulfiremc.server.pathfinding.goals.AwayFromPosGoal;
import com.soulfiremc.server.pathfinding.graph.PathConstraint;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.BooleanProperty;
import com.soulfiremc.server.settings.property.MinMaxPropertyLink;
import com.soulfiremc.server.settings.property.Property;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lenni0451.lambdaevents.EventHandler;

import java.util.concurrent.TimeUnit;

@Slf4j
public class AntiAFK implements InternalPlugin {
  public static final PluginInfo PLUGIN_INFO = new PluginInfo(
    "anti-afk",
    "1.0.0",
    "Automatically moves x amount of blocks in a random direction to prevent being kicked for being AFK",
    "AlexProgrammerDE",
    "GPL-3.0");

  public static void onJoined(BotJoinedEvent event) {
    var connection = event.connection();
    var settingsSource = connection.settingsSource();
    connection
      .scheduler()
      .scheduleWithDynamicDelay(
        () -> {
          if (!settingsSource.get(AntiAFKSettings.ENABLED)) {
            return;
          }

          log.info("Moving bot to prevent AFK");
          PathExecutor.executePathfinding(
            connection,
            new AwayFromPosGoal(
              SFVec3i.fromDouble(connection
                .dataManager()
                .clientEntity()
                .pos()),
              settingsSource.getRandom(AntiAFKSettings.DISTANCE).getAsInt()),
            new PathConstraint(connection));
        },
        settingsSource.getRandom(AntiAFKSettings.DELAY).asLongSupplier(),
        TimeUnit.SECONDS);
  }

  @EventHandler
  public static void onSettingsRegistryInit(InstanceSettingsRegistryInitEvent event) {
    event.settingsRegistry().addClass(AntiAFKSettings.class, "Anti AFK", PLUGIN_INFO, "activity");
  }

  @Override
  public PluginInfo pluginInfo() {
    return PLUGIN_INFO;
  }

  @Override
  public void onServer(SoulFireServer soulFireServer) {
    soulFireServer.registerListeners(AntiAFK.class);
    PluginHelper.registerBotEventConsumer(soulFireServer, BotJoinedEvent.class, AntiAFK::onJoined);
  }

  @NoArgsConstructor(access = AccessLevel.NONE)
  private static class AntiAFKSettings implements SettingsObject {
    private static final Property.Builder BUILDER = Property.builder("anti-afk");
    public static final BooleanProperty ENABLED =
      BUILDER.ofBoolean(
        "enabled",
        "Enable Anti AFK",
        "Enable the Anti AFK feature",
        false);
    public static final MinMaxPropertyLink DISTANCE = new MinMaxPropertyLink(
      BUILDER.ofInt(
        "min-distance",
        "Min distance (blocks)",
        "Minimum distance to walk",
        10,
        1,
        Integer.MAX_VALUE,
        1),
      BUILDER.ofInt(
        "max-distance",
        "Max distance (blocks)",
        "Maximum distance to walk",
        30,
        1,
        Integer.MAX_VALUE,
        1));
    public static final MinMaxPropertyLink DELAY = new MinMaxPropertyLink(
      BUILDER.ofInt(
        "min-delay",
        "Min delay (seconds)",
        "Minimum delay between moves",
        15,
        0,
        Integer.MAX_VALUE,
        1),
      BUILDER.ofInt(
        "max-delay",
        "Max delay (seconds)",
        "Maximum delay between moves",
        45,
        0,
        Integer.MAX_VALUE,
        1));
  }
}
