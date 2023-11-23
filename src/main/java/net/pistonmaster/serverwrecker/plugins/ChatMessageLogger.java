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
import net.pistonmaster.serverwrecker.api.PluginHelper;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.attack.BotConnectionInitEvent;
import net.pistonmaster.serverwrecker.api.event.bot.ChatMessageReceiveEvent;
import net.pistonmaster.serverwrecker.api.event.lifecycle.SettingsManagerInitEvent;
import net.pistonmaster.serverwrecker.settings.lib.SettingsHolder;
import net.pistonmaster.serverwrecker.settings.lib.SettingsObject;
import net.pistonmaster.serverwrecker.settings.lib.property.BooleanProperty;
import net.pistonmaster.serverwrecker.settings.lib.property.IntProperty;
import net.pistonmaster.serverwrecker.settings.lib.property.Property;
import org.slf4j.Logger;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ChatMessageLogger implements InternalExtension {
    @Override
    public void onLoad() {
        ServerWreckerAPI.registerListeners(ChatMessageLogger.class);
        PluginHelper.registerAttackEventConsumer(BotConnectionInitEvent.class, ChatMessageLogger::onConnectionInit);
    }

    public static void onConnectionInit(BotConnectionInitEvent event) {
        var connection = event.connection();
        var settingsHolder = connection.settingsHolder();
        if (!settingsHolder.get(ChatMessageSettings.LOG_CHAT)) {
            return;
        }

        connection.eventBus().register(ChatMessageReceiveEvent.class,
                new BotChatListener(connection.connectionId(), connection.logger(),
                        connection.executorManager().newScheduledExecutorService("Chat"),
                        new LinkedHashSet<>(), settingsHolder));
    }

    @EventHandler
    public static void onSettingsManagerInit(SettingsManagerInitEvent event) {
        event.settingsManager().addClass(ChatMessageSettings.class);
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
        public static final BooleanProperty LOG_CHAT = BUILDER.ofBoolean(
                "log-chat",
                "Log chat to terminal",
                "If this is enabled, all chat messages will be logged to the terminal",
                new String[] {"--log-chat"},
                true
        );
        public static IntProperty INTERVAL = BUILDER.ofInt(
                "chat-interval",
                "Minimum delay between logging chat",
                "This is the minimum delay between logging chat messages",
                new String[] {"--chat-interval"},
                2
        );
    }
}
