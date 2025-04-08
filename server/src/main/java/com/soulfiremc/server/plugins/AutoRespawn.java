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

import com.soulfiremc.server.adventure.SoulFireAdventure;
import com.soulfiremc.server.api.InternalPlugin;
import com.soulfiremc.server.api.PluginInfo;
import com.soulfiremc.server.api.event.bot.BotPreTickEvent;
import com.soulfiremc.server.api.event.bot.SFPacketReceiveEvent;
import com.soulfiremc.server.api.event.lifecycle.InstanceSettingsRegistryInitEvent;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lenni0451.lambdaevents.EventHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerCombatKillPacket;
import org.pf4j.Extension;

import java.util.concurrent.TimeUnit;

@Slf4j
@Extension
public final class AutoRespawn extends InternalPlugin {
  public AutoRespawn() {
    super(new PluginInfo(
      "auto-respawn",
      "1.0.0",
      "Automatically respawns after death",
      "AlexProgrammerDE",
      "GPL-3.0",
      "https://soulfiremc.com"
    ));
  }

  @EventHandler
  public static void onPacket(SFPacketReceiveEvent event) {
    if (event.packet() instanceof ClientboundPlayerCombatKillPacket combatKillPacket) {
      var connection = event.connection();
      var settingsSource = connection.settingsSource();
      if (!settingsSource.get(AutoRespawnSettings.ENABLED)) {
        return;
      }

      var localPlayer = connection.dataManager().localPlayer();
      if (combatKillPacket.getPlayerId() != localPlayer.entityId()) {
        return;
      }

      if (!localPlayer.shouldShowDeathScreen()) {
        return;
      }

      var message =
        SoulFireAdventure.PLAIN_MESSAGE_SERIALIZER.serialize(combatKillPacket.getMessage());
      log.info("[AutoRespawn] Died with message: '{}'", message);

      connection
        .scheduler()
        .schedule(
          localPlayer::respawn,
          settingsSource.getRandom(AutoRespawnSettings.DELAY).getAsLong(),
          TimeUnit.SECONDS);
    }
  }

  @EventHandler
  public static void onTick(BotPreTickEvent event) {
    var connection = event.connection();
    var settingsSource = connection.settingsSource();
    if (!settingsSource.get(AutoRespawnSettings.ENABLED)) {
      return;
    }

    var dataManager = connection.dataManager();
    if (!dataManager.joinedWorld()) {
      return;
    }

    var localPlayer = connection.dataManager().localPlayer();
    if (localPlayer.isDeadOrDying()) {
      localPlayer.respawn();
    }
  }

  @EventHandler
  public void onSettingsRegistryInit(InstanceSettingsRegistryInitEvent event) {
    event.settingsRegistry().addPluginPage(AutoRespawnSettings.class, "Auto Respawn", this, "repeat", AutoRespawnSettings.ENABLED);
  }

  @NoArgsConstructor(access = AccessLevel.NONE)
  private static class AutoRespawnSettings implements SettingsObject {
    private static final String NAMESPACE = "auto-respawn";
    public static final BooleanProperty ENABLED =
      ImmutableBooleanProperty.builder()
        .namespace(NAMESPACE)
        .key("enabled")
        .uiName("Enable Auto Respawn")
        .description("Respawn automatically after death")
        .defaultValue(true)
        .build();
    public static final MinMaxProperty DELAY =
      ImmutableMinMaxProperty.builder()
        .namespace(NAMESPACE)
        .key("delay")
        .minValue(0)
        .maxValue(Integer.MAX_VALUE)
        .minEntry(ImmutableMinMaxPropertyEntry.builder()
          .uiName("Min delay (seconds)")
          .description("Minimum delay between respawns")
          .defaultValue(1)
          .build())
        .maxEntry(ImmutableMinMaxPropertyEntry.builder()
          .uiName("Max delay (seconds)")
          .description("Maximum delay between respawns")
          .defaultValue(3)
          .build())
        .build();
  }
}
