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
import com.soulfiremc.server.api.event.lifecycle.BotSettingsRegistryInitEvent;
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
public final class AutoJump extends InternalPlugin {
  public AutoJump() {
    super(new PluginInfo(
      "auto-jump",
      "1.0.0",
      "Automatically jumps randomly",
      "AlexProgrammerDE",
      "AGPL-3.0",
      "https://soulfiremc.com"
    ));
  }

  @EventHandler
  public static void onJoined(BotConnectionInitEvent event) {
    var connection = event.connection();
    var settingsSource = connection.settingsSource();
    connection.scheduler().scheduleWithDynamicDelay(
      () -> {
        if (!settingsSource.get(AutoJumpSettings.ENABLED)) {
          return;
        }

        var player = connection.minecraft().player;
        if (player == null) {
          return;
        }

        log.debug("[AutoJump] Jumping!");
        player.jumpFromGround();
      },
      settingsSource.getRandom(AutoJumpSettings.DELAY).asLongSupplier(),
      TimeUnit.SECONDS);
  }

  @EventHandler
  public void onSettingsRegistryInit(BotSettingsRegistryInitEvent event) {
    event.settingsPageRegistry().addPluginPage(AutoJumpSettings.class, "Auto Jump", this, "footprints", AutoJumpSettings.ENABLED);
  }

  @NoArgsConstructor(access = AccessLevel.NONE)
  private static class AutoJumpSettings implements SettingsObject {
    private static final String NAMESPACE = "auto-jump";
    public static final BooleanProperty<SettingsSource.Bot> ENABLED =
      ImmutableBooleanProperty.<SettingsSource.Bot>builder()
        .namespace(NAMESPACE)
        .key("enabled")
        .uiName("Enable Auto Jump")
        .description("Attempt to jump automatically in random intervals")
        .defaultValue(false)
        .build();
    public static final MinMaxProperty<SettingsSource.Bot> DELAY =
      ImmutableMinMaxProperty.<SettingsSource.Bot>builder()
        .namespace(NAMESPACE)
        .key("delay")
        .minValue(0)
        .maxValue(Integer.MAX_VALUE)
        .minEntry(ImmutableMinMaxPropertyEntry.builder()
          .uiName("Min delay (seconds)")
          .description("Minimum delay between jumps")
          .defaultValue(2)
          .build())
        .maxEntry(ImmutableMinMaxPropertyEntry.builder()
          .uiName("Max delay (seconds)")
          .description("Maximum delay between jumps")
          .defaultValue(5)
          .build())
        .build();
  }
}
