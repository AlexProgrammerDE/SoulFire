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
package net.pistonmaster.serverwrecker.plugins;

import com.github.steveice10.mc.protocol.data.ProtocolState;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.lenni0451.lambdaevents.EventHandler;
import net.pistonmaster.serverwrecker.api.PluginHelper;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.attack.PreBotConnectEvent;
import net.pistonmaster.serverwrecker.api.event.lifecycle.SettingsRegistryInitEvent;
import net.pistonmaster.serverwrecker.settings.lib.SettingsObject;
import net.pistonmaster.serverwrecker.settings.lib.property.BooleanProperty;
import net.pistonmaster.serverwrecker.settings.lib.property.MinMaxPropertyLink;
import net.pistonmaster.serverwrecker.settings.lib.property.Property;
import net.pistonmaster.serverwrecker.util.RandomUtil;
import net.pistonmaster.serverwrecker.util.TimeUtil;

import java.util.concurrent.TimeUnit;

public class ServerListBypass implements InternalExtension {
    public static void onPreConnect(PreBotConnectEvent event) {
        var connection = event.connection();
        if (connection.meta().getTargetState() == ProtocolState.STATUS) {
            return;
        }

        var factory = connection.factory();
        var settingsHolder = connection.settingsHolder();
        if (!settingsHolder.get(ServerListBypassSettings.SERVER_LIST_BYPASS)) {
            return;
        }

        factory.prepareConnectionInternal(ProtocolState.STATUS).connect().join();
        TimeUtil.waitTime(RandomUtil.getRandomInt(settingsHolder.get(ServerListBypassSettings.DELAY.min()),
                settingsHolder.get(ServerListBypassSettings.DELAY.max())), TimeUnit.SECONDS);
    }

    @EventHandler
    public static void onSettingsManagerInit(SettingsRegistryInitEvent event) {
        event.settingsRegistry().addClass(ServerListBypassSettings.class, "Server List Bypass");
    }

    @Override
    public void onLoad() {
        ServerWreckerAPI.registerListeners(ServerListBypass.class);
        PluginHelper.registerAttackEventConsumer(PreBotConnectEvent.class, ServerListBypass::onPreConnect);
    }

    @NoArgsConstructor(access = AccessLevel.NONE)
    private static class ServerListBypassSettings implements SettingsObject {
        private static final Property.Builder BUILDER = Property.builder("server-list-bypass");
        public static final BooleanProperty SERVER_LIST_BYPASS = BUILDER.ofBoolean("server-list-bypass",
                "Do Server List Bypass?",
                "Do Server List Bypass?",
                new String[]{"--server-list-bypass"},
                false
        );
        public static final MinMaxPropertyLink DELAY = new MinMaxPropertyLink(
                BUILDER.ofInt(
                        "server-list-bypass-min-delay",
                        "Min delay (seconds)",
                        "Minimum delay between joining the server",
                        new String[]{"--server-list-bypass-min-delay"},
                        1
                ),
                BUILDER.ofInt(
                        "server-list-bypass-max-delay",
                        "Max delay (seconds)",
                        "Maximum delay between joining the server",
                        new String[]{"--server-list-bypass-max-delay"},
                        3
                )
        );
    }
}
