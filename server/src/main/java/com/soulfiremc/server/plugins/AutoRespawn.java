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
import com.soulfiremc.server.api.event.bot.SFPacketReceiveEvent;
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
import org.geysermc.mcprotocollib.protocol.data.game.ClientCommand;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerCombatKillPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket;

public class AutoRespawn implements InternalPlugin {
  public static final PluginInfo PLUGIN_INFO = new PluginInfo(
    "auto-respawn",
    "1.0.0",
    "Automatically respawns after death",
    "AlexProgrammerDE",
    "GPL-3.0"
  );

  public static void onPacket(SFPacketReceiveEvent event) {
    if (event.packet() instanceof ClientboundPlayerCombatKillPacket combatKillPacket) {
      var connection = event.connection();
      var settingsHolder = connection.settingsHolder();
      if (!settingsHolder.get(AutoRespawnSettings.ENABLED)) {
        return;
      }

      var message =
        SoulFireServer.PLAIN_MESSAGE_SERIALIZER.serialize(combatKillPacket.getMessage());
      connection
        .logger()
        .info(
          "[AutoRespawn] Died with killer: {} and message: '{}'",
          combatKillPacket.getPlayerId(),
          message);

      connection
        .scheduler()
        .schedule(
          () ->
            connection
              .session()
              .send(new ServerboundClientCommandPacket(ClientCommand.RESPAWN)),
          RandomUtil.getRandomInt(
            settingsHolder.get(AutoRespawnSettings.DELAY.min()),
            settingsHolder.get(AutoRespawnSettings.DELAY.max())),
          TimeUnit.SECONDS);
    }
  }

  @EventHandler
  public static void onSettingsRegistryInit(InstanceSettingsRegistryInitEvent event) {
    event.settingsRegistry().addClass(AutoRespawnSettings.class, "Auto Respawn", PLUGIN_INFO);
  }

  @Override
  public PluginInfo pluginInfo() {
    return PLUGIN_INFO;
  }

  @Override
  public void onServer(SoulFireServer soulFireServer) {
    soulFireServer.registerListeners(AutoRespawn.class);
    PluginHelper.registerBotEventConsumer(soulFireServer, SFPacketReceiveEvent.class, AutoRespawn::onPacket);
  }

  @NoArgsConstructor(access = AccessLevel.NONE)
  private static class AutoRespawnSettings implements SettingsObject {
    private static final Property.Builder BUILDER = Property.builder("auto-respawn");
    public static final BooleanProperty ENABLED =
      BUILDER.ofBoolean(
        "enabled",
        "Enable Auto Respawn",
        new String[] {"--auto-respawn"},
        "Respawn automatically after death",
        true);
    public static final MinMaxPropertyLink DELAY =
      new MinMaxPropertyLink(
        BUILDER.ofInt(
          "min-delay",
          "Min delay (seconds)",
          new String[] {"--respawn-min-delay"},
          "Minimum delay between respawns",
          1,
          0,
          Integer.MAX_VALUE,
          1),
        BUILDER.ofInt(
          "max-delay",
          "Max delay (seconds)",
          new String[] {"--respawn-max-delay"},
          "Maximum delay between respawns",
          3,
          0,
          Integer.MAX_VALUE,
          1));
  }
}
