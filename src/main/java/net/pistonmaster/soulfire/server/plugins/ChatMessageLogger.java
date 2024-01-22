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
import net.pistonmaster.soulfire.server.api.PluginHelper;
import net.pistonmaster.soulfire.server.api.SoulFireAPI;
import net.pistonmaster.soulfire.server.api.event.attack.BotConnectionInitEvent;
import net.pistonmaster.soulfire.server.api.event.bot.ChatMessageReceiveEvent;
import net.pistonmaster.soulfire.server.api.event.lifecycle.SettingsRegistryInitEvent;
import net.pistonmaster.soulfire.server.settings.lib.SettingsHolder;
import net.pistonmaster.soulfire.server.settings.lib.SettingsObject;
import net.pistonmaster.soulfire.server.settings.lib.property.BooleanProperty;
import net.pistonmaster.soulfire.server.settings.lib.property.IntProperty;
import net.pistonmaster.soulfire.server.settings.lib.property.Property;
import org.slf4j.Logger;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ChatMessageLogger implements InternalExtension {
    public static void onConnectionInit(BotConnectionInitEvent event) {
        var connection = event.connection();
        var settingsHolder = connection.settingsHolder();
        if (!settingsHolder.get(ChatMessageSettings.ENABLED)) {
            return;
        }

        connection.eventBus().register(ChatMessageReceiveEvent.class,
                new BotChatListener(connection.connectionId(), connection.logger(),
                        connection.executorManager().newScheduledExecutorService(connection, "Chat"),
                        new LinkedHashSet<>(), settingsHolder));
    }

    @EventHandler
    public static void onSettingsManagerInit(SettingsRegistryInitEvent event) {
        event.settingsRegistry().addClass(ChatMessageSettings.class, "Chat Message Logger");
    }

    @Override
    public void onLoad() {
        SoulFireAPI.registerListeners(ChatMessageLogger.class);
        PluginHelper.registerAttackEventConsumer(BotConnectionInitEvent.class, ChatMessageLogger::onConnectionInit);
    }

    private record BotChatListener(UUID connectionId, Logger logger, ScheduledExecutorService executor,
                                   Set<String> messageQueue, SettingsHolder settingsHolder)
            implements Consumer<ChatMessageReceiveEvent> {
        public BotChatListener {
            executor.scheduleWithFixedDelay(() -> {
                var iter = messageQueue.iterator();
                while (!messageQueue.isEmpty()) {
                    var message = iter.next();
                    iter.remove();
                    if (!Objects.nonNull(message)) {
                        continue;
                    }

                    logger.info("[Chat] Received Message: {}", message);
                }
            }, 0, settingsHolder.get(ChatMessageSettings.INTERVAL), TimeUnit.SECONDS);
        }

        @Override
        public void accept(ChatMessageReceiveEvent event) {
            if (!event.connection().connectionId().equals(connectionId)) {
                return;
            }

            messageQueue.add(event.parseToText().replace("\n", "\\n"));
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    private static class ChatMessageSettings implements SettingsObject {
        private static final Property.Builder BUILDER = Property.builder("chat-message-logger");
        public static final BooleanProperty ENABLED = BUILDER.ofBoolean(
                "enabled",
                "Log chat to terminal",
                new String[]{"--log-chat"},
                "Log all received chat messages to the terminal",
                true
        );
        public static final IntProperty INTERVAL = BUILDER.ofInt(
                "interval",
                "Interval between logging chat messages",
                new String[]{"--chat-interval"},
                "This is the minimum delay between logging chat messages",
                2,
                0,
                Integer.MAX_VALUE,
                1
        );
    }
}
