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
import com.soulfiremc.server.api.event.bot.BotConnectionInitEvent;
import com.soulfiremc.server.api.event.lifecycle.InstanceSettingsRegistryInitEvent;
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.pathfinding.execution.PathExecutor;
import com.soulfiremc.server.pathfinding.goals.AwayFromPosGoal;
import com.soulfiremc.server.pathfinding.graph.constraint.PathConstraintImpl;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.lib.SettingsSource;
import com.soulfiremc.server.settings.property.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lenni0451.lambdaevents.EventHandler;

import java.util.concurrent.TimeUnit;

@Slf4j
@InternalPluginClass
public final class AntiAFK extends InternalPlugin {
  public AntiAFK() {
    super(new PluginInfo(
      "anti-afk",
      "1.0.0",
      "Automatically moves x amount of blocks in a random direction to prevent being kicked for being AFK",
      "AlexProgrammerDE",
      "AGPL-3.0",
      "https://soulfiremc.com"));
  }

  @EventHandler
  public static void onJoined(BotConnectionInitEvent event) {
    var connection = event.connection();
    var settingsSource = connection.settingsSource();
    connection
      .scheduler()
      .scheduleWithDynamicDelay(
        () -> {
          if (!settingsSource.get(AntiAFKSettings.ENABLED)) {
            return;
          }

          var player = connection.minecraft().player;
          if (player == null) {
            return;
          }

          log.info("Moving bot to prevent AFK");
          PathExecutor.executePathfinding(
            connection,
            new AwayFromPosGoal(
              SFVec3i.fromDouble(connection
                .minecraft()
                .player
                .position()),
              settingsSource.getRandom(AntiAFKSettings.DISTANCE).getAsInt()),
            new PathConstraintImpl(connection));
        },
        settingsSource.getRandom(AntiAFKSettings.DELAY).asLongSupplier(),
        TimeUnit.SECONDS);
  }

  @EventHandler
  public void onSettingsRegistryInit(InstanceSettingsRegistryInitEvent event) {
    event.settingsPageRegistry().addPluginPage(AntiAFKSettings.class, "anti-afk", "Anti AFK", this, "activity", AntiAFKSettings.ENABLED);
  }

  @NoArgsConstructor(access = AccessLevel.NONE)
  private static class AntiAFKSettings implements SettingsObject {
    private static final String NAMESPACE = "anti-afk";
    public static final BooleanProperty<SettingsSource.Bot> ENABLED =
      ImmutableBooleanProperty.<SettingsSource.Bot>builder()
        .sourceType(SettingsSource.Bot.INSTANCE)
        .namespace(NAMESPACE)
        .key("enabled")
        .uiName("Enable Anti AFK")
        .description("Enable the Anti AFK feature")
        .defaultValue(false)
        .build();
    public static final MinMaxProperty<SettingsSource.Bot> DISTANCE = ImmutableMinMaxProperty.<SettingsSource.Bot>builder()
      .sourceType(SettingsSource.Bot.INSTANCE)
      .namespace(NAMESPACE)
      .key("distance")
      .minValue(1)
      .maxValue(Integer.MAX_VALUE)
      .minEntry(ImmutableMinMaxPropertyEntry.builder()
        .uiName("Min distance (blocks)")
        .description("Minimum distance to walk")
        .defaultValue(10)
        .build())
      .maxEntry(ImmutableMinMaxPropertyEntry.builder()
        .uiName("Max distance (blocks)")
        .description("Maximum distance to walk")
        .defaultValue(30)
        .build())
      .build();
    public static final MinMaxProperty<SettingsSource.Bot> DELAY = ImmutableMinMaxProperty.<SettingsSource.Bot>builder()
      .sourceType(SettingsSource.Bot.INSTANCE)
      .namespace(NAMESPACE)
      .key("delay")
      .minValue(0)
      .maxValue(Integer.MAX_VALUE)
      .minEntry(ImmutableMinMaxPropertyEntry.builder()
        .uiName("Min delay (seconds)")
        .description("Minimum delay between moves")
        .defaultValue(15)
        .build())
      .maxEntry(ImmutableMinMaxPropertyEntry.builder()
        .uiName("Max delay (seconds)")
        .description("Maximum delay between moves")
        .defaultValue(30)
        .build())
      .build();
  }
}
