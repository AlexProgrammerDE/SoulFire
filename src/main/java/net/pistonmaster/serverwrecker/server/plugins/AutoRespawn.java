/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.server.plugins;

import com.github.steveice10.mc.protocol.data.game.ClientCommand;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerCombatKillPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.lenni0451.lambdaevents.EventHandler;
import net.pistonmaster.serverwrecker.server.ServerWreckerServer;
import net.pistonmaster.serverwrecker.server.api.PluginHelper;
import net.pistonmaster.serverwrecker.server.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.server.api.event.bot.SWPacketReceiveEvent;
import net.pistonmaster.serverwrecker.server.api.event.lifecycle.SettingsRegistryInitEvent;
import net.pistonmaster.serverwrecker.server.settings.lib.SettingsObject;
import net.pistonmaster.serverwrecker.server.settings.lib.property.BooleanProperty;
import net.pistonmaster.serverwrecker.server.settings.lib.property.MinMaxPropertyLink;
import net.pistonmaster.serverwrecker.server.settings.lib.property.Property;
import net.pistonmaster.serverwrecker.server.util.RandomUtil;

import java.util.concurrent.TimeUnit;

public class AutoRespawn implements InternalExtension {
    public static void onPacket(SWPacketReceiveEvent event) {
        if (event.packet() instanceof ClientboundPlayerCombatKillPacket combatKillPacket) {
            var connection = event.connection();
            var settingsHolder = connection.settingsHolder();
            if (!settingsHolder.get(AutoRespawnSettings.AUTO_RESPAWN)) {
                return;
            }

            var message = ServerWreckerServer.PLAIN_MESSAGE_SERIALIZER.serialize(combatKillPacket.getMessage());
            connection.logger().info("[AutoRespawn] Died with killer: {} and message: '{}'",
                    combatKillPacket.getPlayerId(), message);

            connection.executorManager().newScheduledExecutorService(connection, "Respawn").schedule(() ->
                            connection.session().send(new ServerboundClientCommandPacket(ClientCommand.RESPAWN)),
                    RandomUtil.getRandomInt(settingsHolder.get(AutoRespawnSettings.DELAY.min()), settingsHolder.get(AutoRespawnSettings.DELAY.max())), TimeUnit.SECONDS);
        }
    }

    @EventHandler
    public static void onSettingsManagerInit(SettingsRegistryInitEvent event) {
        event.settingsRegistry().addClass(AutoRespawnSettings.class, "Auto Respawn");
    }

    @Override
    public void onLoad() {
        ServerWreckerAPI.registerListeners(AutoRespawn.class);
        PluginHelper.registerBotEventConsumer(SWPacketReceiveEvent.class, AutoRespawn::onPacket);
    }

    @NoArgsConstructor(access = AccessLevel.NONE)
    private static class AutoRespawnSettings implements SettingsObject {
        private static final Property.Builder BUILDER = Property.builder("auto-respawn");
        public static final BooleanProperty AUTO_RESPAWN = BUILDER.ofBoolean("auto-respawn",
                "Do Auto Respawn?",
                "Do Auto Respawn?",
                new String[]{"--auto-respawn"},
                true
        );
        public static final MinMaxPropertyLink DELAY = new MinMaxPropertyLink(
                BUILDER.ofInt("respawn-min-delay",
                        "Min delay (seconds)",
                        "Minimum delay between respawns",
                        new String[]{"--respawn-min-delay"},
                        1,
                        0,
                        Integer.MAX_VALUE,
                        1
                ),
                BUILDER.ofInt("respawn-max-delay",
                        "Max delay (seconds)",
                        "Maximum delay between respawns",
                        new String[]{"--respawn-max-delay"},
                        3,
                        0,
                        Integer.MAX_VALUE,
                        1
                )
        );
    }
}
