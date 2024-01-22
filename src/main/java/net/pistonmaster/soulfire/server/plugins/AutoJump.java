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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.lenni0451.lambdaevents.EventHandler;
import net.pistonmaster.soulfire.server.api.ExecutorHelper;
import net.pistonmaster.soulfire.server.api.PluginHelper;
import net.pistonmaster.soulfire.server.api.SoulFireAPI;
import net.pistonmaster.soulfire.server.api.event.bot.BotJoinedEvent;
import net.pistonmaster.soulfire.server.api.event.lifecycle.SettingsRegistryInitEvent;
import net.pistonmaster.soulfire.server.settings.lib.SettingsObject;
import net.pistonmaster.soulfire.server.settings.lib.property.BooleanProperty;
import net.pistonmaster.soulfire.server.settings.lib.property.MinMaxPropertyLink;
import net.pistonmaster.soulfire.server.settings.lib.property.Property;

public class AutoJump implements InternalExtension {
    public static void onJoined(BotJoinedEvent event) {
        var connection = event.connection();
        var settingsHolder = connection.settingsHolder();
        if (!settingsHolder.get(AutoJumpSettings.ENABLED)) {
            return;
        }

        var executor = connection.executorManager().newScheduledExecutorService(connection, "AutoJump");
        ExecutorHelper.executeRandomDelaySeconds(executor, () -> {
            var sessionDataManager = connection.sessionDataManager();
            var level = sessionDataManager.getCurrentLevel();
            var clientEntity = sessionDataManager.clientEntity();
            if (level != null && clientEntity != null
                    && level.isChunkLoaded(clientEntity.blockPos())
                    && clientEntity.onGround()) {
                connection.logger().debug("[AutoJump] Jumping!");
                clientEntity.jump();
            }
        }, settingsHolder.get(AutoJumpSettings.DELAY.min()), settingsHolder.get(AutoJumpSettings.DELAY.max()));
    }

    @EventHandler
    public static void onSettingsManagerInit(SettingsRegistryInitEvent event) {
        event.settingsRegistry().addClass(AutoJumpSettings.class, "Auto Jump");
    }

    @Override
    public void onLoad() {
        SoulFireAPI.registerListeners(AutoJump.class);
        PluginHelper.registerBotEventConsumer(BotJoinedEvent.class, AutoJump::onJoined);
    }

    @NoArgsConstructor(access = AccessLevel.NONE)
    private static class AutoJumpSettings implements SettingsObject {
        private static final Property.Builder BUILDER = Property.builder("auto-jump");
        public static final BooleanProperty ENABLED = BUILDER.ofBoolean(
                "enabled",
                "Enable Auto Jump",
                new String[]{"--auto-jump"},
                "Attempt to jump automatically in random intervals",
                false
        );
        public static final MinMaxPropertyLink DELAY = new MinMaxPropertyLink(
                BUILDER.ofInt(
                        "min-delay",
                        "Min delay (seconds)",
                        new String[]{"--jump-min-delay"},
                        "Minimum delay between jumps",
                        2,
                        0,
                        Integer.MAX_VALUE,
                        1
                ),
                BUILDER.ofInt(
                        "max-delay",
                        "Max delay (seconds)",
                        new String[]{"--jump-max-delay"},
                        "Maximum delay between jumps",
                        5,
                        0,
                        Integer.MAX_VALUE,
                        1
                )
        );
    }
}
