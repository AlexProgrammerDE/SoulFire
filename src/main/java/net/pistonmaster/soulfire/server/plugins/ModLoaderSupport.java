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
package net.pistonmaster.soulfire.server.plugins;

import com.github.steveice10.mc.protocol.packet.common.clientbound.ClientboundCustomPayloadPacket;
import com.github.steveice10.mc.protocol.packet.handshake.serverbound.ClientIntentionPacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundCustomQueryPacket;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lenni0451.lambdaevents.EventHandler;
import net.pistonmaster.soulfire.server.api.PluginHelper;
import net.pistonmaster.soulfire.server.api.SoulFireAPI;
import net.pistonmaster.soulfire.server.api.event.bot.SWPacketReceiveEvent;
import net.pistonmaster.soulfire.server.api.event.bot.SWPacketSendingEvent;
import net.pistonmaster.soulfire.server.api.event.lifecycle.SettingsRegistryInitEvent;
import net.pistonmaster.soulfire.server.settings.lib.SettingsObject;
import net.pistonmaster.soulfire.server.settings.lib.property.ComboProperty;
import net.pistonmaster.soulfire.server.settings.lib.property.Property;

import javax.inject.Inject;
import java.util.Arrays;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ModLoaderSupport implements InternalExtension {
    private static final char HOSTNAME_SEPARATOR = '\0';

    @EventHandler
    public static void onSettingsManagerInit(SettingsRegistryInitEvent event) {
        event.settingsRegistry().addClass(ModLoaderSettings.class, "Mod Loader Support");
    }

    @Override
    public void onLoad() {
        SoulFireAPI.registerListeners(ModLoaderSupport.class);
        PluginHelper.registerBotEventConsumer(SWPacketSendingEvent.class, this::onPacket);
        PluginHelper.registerBotEventConsumer(SWPacketReceiveEvent.class, this::onPacketReceive);
    }

    public void onPacket(SWPacketSendingEvent event) {
        if (!(event.packet() instanceof ClientIntentionPacket handshake)) {
            return;
        }

        var connection = event.connection();
        var settingsHolder = connection.settingsHolder();
        var hostname = handshake.getHostname();

        switch (settingsHolder.get(ModLoaderSettings.FORGE_MODE, ModLoaderSettings.ModLoaderMode.class)) {
            case FML -> event.packet(handshake
                    .withHostname(createFMLAddress(hostname)));
            case FML2 -> event.packet(handshake
                    .withHostname(createFML2Address(hostname)));
        }
    }

    public void onPacketReceive(SWPacketReceiveEvent event) {
        var connection = event.connection();
        var settingsHolder = connection.settingsHolder();

        if (event.packet() instanceof ClientboundCustomPayloadPacket pluginMessage) {
            if (settingsHolder.get(ModLoaderSettings.FORGE_MODE, ModLoaderSettings.ModLoaderMode.class)
            == ModLoaderSettings.ModLoaderMode.FML) {
                handleFMLPluginMessage(pluginMessage);
            }
        } else if (event.packet() instanceof ClientboundCustomQueryPacket loginPluginMessage) {
            if (settingsHolder.get(ModLoaderSettings.FORGE_MODE, ModLoaderSettings.ModLoaderMode.class)
                    == ModLoaderSettings.ModLoaderMode.FML2) {
                handleFML2PluginMessage(loginPluginMessage);
            }
        }
    }

    private void handleFMLPluginMessage(ClientboundCustomPayloadPacket pluginMessage) {

    }

    private void handleFML2PluginMessage(ClientboundCustomQueryPacket loginPluginMessage) {

    }

    private static String createFMLAddress(String initialHostname) {
        return initialHostname + HOSTNAME_SEPARATOR + "FML" + HOSTNAME_SEPARATOR;
    }

    private static String createFML2Address(String initialHostname) {
        return initialHostname + HOSTNAME_SEPARATOR + "FML2" + HOSTNAME_SEPARATOR;
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    private static class ModLoaderSettings implements SettingsObject {
        private static final Property.Builder BUILDER = Property.builder("mod-loader");
        public static final ComboProperty FORGE_MODE = BUILDER.ofEnum(
                "mod-loader-mode",
                "Mod Loader mode",
                new String[]{"--mod-loader-mode"},
                "What mod loader to use",
                ModLoaderMode.values(),
                ModLoaderMode.NONE
        );

        @RequiredArgsConstructor
        enum ModLoaderMode {
            NONE("None"),
            FML("FML (Forge 1.7-1.12)"),
            FML2("FML2 (Forge 1.13+)");

            private final String displayName;

            @Override
            public String toString() {
                return displayName;
            }
        }
    }
}
