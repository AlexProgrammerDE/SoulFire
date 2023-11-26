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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.lenni0451.lambdaevents.EventHandler;
import net.pistonmaster.serverwrecker.api.ExecutorHelper;
import net.pistonmaster.serverwrecker.api.PluginHelper;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.bot.BotJoinedEvent;
import net.pistonmaster.serverwrecker.api.event.lifecycle.SettingsRegistryInitEvent;
import net.pistonmaster.serverwrecker.settings.lib.SettingsObject;
import net.pistonmaster.serverwrecker.settings.lib.property.BooleanProperty;
import net.pistonmaster.serverwrecker.settings.lib.property.MinMaxPropertyLink;
import net.pistonmaster.serverwrecker.settings.lib.property.Property;

public class AutoJump implements InternalExtension {
    public static void onJoined(BotJoinedEvent event) {
        var connection = event.connection();
        var settingsHolder = connection.settingsHolder();
        if (!settingsHolder.get(AutoJumpSettings.AUTO_JUMP)) {
            return;
        }

        var executor = connection.executorManager().newScheduledExecutorService(connection, "AutoJump");
        ExecutorHelper.executeRandomDelaySeconds(executor, () -> {
            var sessionDataManager = connection.sessionDataManager();
            var level = sessionDataManager.getCurrentLevel();
            var movementManager = sessionDataManager.getBotMovementManager();
            if (level != null && movementManager != null
                    && level.isChunkLoaded(movementManager.getBlockPos())
                    && movementManager.getEntity().isOnGround()) {
                connection.logger().debug("[AutoJump] Jumping!");
                movementManager.jump();
            }
        }, settingsHolder.get(AutoJumpSettings.DELAY.min()), settingsHolder.get(AutoJumpSettings.DELAY.max()));
    }

    @EventHandler
    public static void onSettingsManagerInit(SettingsRegistryInitEvent event) {
        event.settingsRegistry().addClass(AutoJumpSettings.class, "Auto Jump");
    }

    @Override
    public void onLoad() {
        ServerWreckerAPI.registerListeners(AutoJump.class);
        PluginHelper.registerBotEventConsumer(BotJoinedEvent.class, AutoJump::onJoined);
    }

    @NoArgsConstructor(access = AccessLevel.NONE)
    private static class AutoJumpSettings implements SettingsObject {
        private static final Property.Builder BUILDER = Property.builder("auto-jump");
        public static final BooleanProperty AUTO_JUMP = BUILDER.ofBoolean("auto-jump",
                "Do Auto Jump?",
                "Do Auto Jump?",
                new String[]{"--auto-jump"},
                true
        );
        public static final MinMaxPropertyLink DELAY = new MinMaxPropertyLink(
                BUILDER.ofInt("jump-min-delay",
                        "Min delay (seconds)",
                        "Minimum delay between jumps",
                        new String[]{"--jump-min-delay"},
                        2,
                        0,
                        Integer.MAX_VALUE,
                        1
                ),
                BUILDER.ofInt("jump-max-delay",
                        "Max delay (seconds)",
                        "Maximum delay between jumps",
                        new String[]{"--jump-max-delay"},
                        5,
                        0,
                        Integer.MAX_VALUE,
                        1
                )
        );
    }
}
