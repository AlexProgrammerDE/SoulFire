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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.lenni0451.lambdaevents.EventHandler;
import net.pistonmaster.serverwrecker.server.api.PluginHelper;
import net.pistonmaster.serverwrecker.server.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.server.api.event.bot.BotDisconnectedEvent;
import net.pistonmaster.serverwrecker.server.api.event.lifecycle.SettingsRegistryInitEvent;
import net.pistonmaster.serverwrecker.server.settings.lib.SettingsObject;
import net.pistonmaster.serverwrecker.server.settings.lib.property.BooleanProperty;
import net.pistonmaster.serverwrecker.server.settings.lib.property.MinMaxPropertyLink;
import net.pistonmaster.serverwrecker.server.settings.lib.property.Property;
import net.pistonmaster.serverwrecker.server.util.RandomUtil;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AutoReconnect implements InternalExtension {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @EventHandler
    public static void onSettingsManagerInit(SettingsRegistryInitEvent event) {
        event.settingsRegistry().addClass(AutoReconnectSettings.class, "Auto Reconnect");
    }

    @Override
    public void onLoad() {
        ServerWreckerAPI.registerListeners(AutoReconnect.class);
        PluginHelper.registerBotEventConsumer(BotDisconnectedEvent.class, this::onDisconnect);
    }

    public void onDisconnect(BotDisconnectedEvent event) {
        var connection = event.connection();
        var settingsHolder = connection.settingsHolder();
        if (!settingsHolder.get(AutoReconnectSettings.ENABLED) || connection.attackManager().attackState().isInactive()) {
            return;
        }

        scheduler.schedule(() -> {
            var eventLoopGroup = connection.session().eventLoopGroup();
            if (eventLoopGroup.isShuttingDown() || eventLoopGroup.isShutdown() || eventLoopGroup.isTerminated()) {
                return;
            }

            connection.gracefulDisconnect().join();
            var newConnection = connection.factory().prepareConnection();

            connection.attackManager().botConnections()
                    .replaceAll(connectionEntry -> connectionEntry == connection ? newConnection : connectionEntry);

            newConnection.connect();
        }, RandomUtil.getRandomInt(settingsHolder.get(AutoReconnectSettings.DELAY.min()), settingsHolder.get(AutoReconnectSettings.DELAY.max())), TimeUnit.SECONDS);
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    private static class AutoReconnectSettings implements SettingsObject {
        private static final Property.Builder BUILDER = Property.builder("auto-reconnect");
        public static final BooleanProperty ENABLED = BUILDER.ofBoolean(
                "enabled",
                "Enable Auto Reconnect",
                new String[]{"--auto-reconnect"},
                "Reconnect a bot when it times out/is kicked",
                true
        );
        public static final MinMaxPropertyLink DELAY = new MinMaxPropertyLink(
                BUILDER.ofInt(
                        "min-delay",
                        "Min delay (seconds)",
                        new String[]{"--reconnect-min-delay"},
                        "Minimum delay between reconnects",
                        1,
                        0,
                        Integer.MAX_VALUE,
                        1
                ),
                BUILDER.ofInt(
                        "max-delay",
                        "Max delay (seconds)",
                        new String[]{"--reconnect-max-delay"},
                        "Maximum delay between reconnects",
                        5,
                        0,
                        Integer.MAX_VALUE,
                        1
                )
        );
    }
}
