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

import com.github.steveice10.mc.protocol.packet.handshake.serverbound.ClientIntentionPacket;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.lenni0451.lambdaevents.EventHandler;
import net.pistonmaster.serverwrecker.server.api.PluginHelper;
import net.pistonmaster.serverwrecker.server.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.server.api.event.bot.SWPacketSendingEvent;
import net.pistonmaster.serverwrecker.server.api.event.lifecycle.SettingsRegistryInitEvent;
import net.pistonmaster.serverwrecker.server.settings.lib.SettingsObject;
import net.pistonmaster.serverwrecker.server.settings.lib.property.BooleanProperty;
import net.pistonmaster.serverwrecker.server.settings.lib.property.IntProperty;
import net.pistonmaster.serverwrecker.server.settings.lib.property.Property;
import net.pistonmaster.serverwrecker.server.settings.lib.property.StringProperty;

public class FakeVirtualHost implements InternalExtension {
    public static void onPacket(SWPacketSendingEvent event) {
        if (event.packet() instanceof ClientIntentionPacket intentionPacket) {
            var settingsHolder = event.connection().settingsHolder();

            if (!settingsHolder.get(FakeVirtualHostSettings.ENABLED)) {
                return;
            }

            event.packet(intentionPacket
                    .withHostname(settingsHolder.get(FakeVirtualHostSettings.HOSTNAME))
                    .withPort(settingsHolder.get(FakeVirtualHostSettings.PORT))
            );
        }
    }

    @EventHandler
    public static void onSettingsManagerInit(SettingsRegistryInitEvent event) {
        event.settingsRegistry().addClass(FakeVirtualHostSettings.class, "Fake Virtual Host");
    }

    @Override
    public void onLoad() {
        ServerWreckerAPI.registerListeners(FakeVirtualHost.class);
        PluginHelper.registerBotEventConsumer(SWPacketSendingEvent.class, FakeVirtualHost::onPacket);
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    private static class FakeVirtualHostSettings implements SettingsObject {
        private static final Property.Builder BUILDER = Property.builder("fake-virtual-host");
        public static final BooleanProperty ENABLED = BUILDER.ofBoolean(
                "enabled",
                "Fake virtual host",
                new String[]{"--fake-virtual-host"},
                "Whether to fake the virtual host or not",
                false
        );
        public static final StringProperty HOSTNAME = BUILDER.ofString(
                "hostname",
                "Hostname",
                new String[]{"--fake-virtual-host-hostname"},
                "The hostname to fake",
                "localhost"
        );
        public static final IntProperty PORT = BUILDER.ofInt(
                "port",
                "Port",
                new String[]{"--fake-virtual-host-port"},
                "The port to fake",
                25565,
                1,
                65535,
                1,
                "#"
        );
    }
}
