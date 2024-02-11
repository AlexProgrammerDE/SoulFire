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
import net.pistonmaster.soulfire.server.api.event.bot.ChatMessageReceiveEvent;
import net.pistonmaster.soulfire.server.api.event.lifecycle.SettingsRegistryInitEvent;
import net.pistonmaster.soulfire.server.settings.lib.SettingsObject;
import net.pistonmaster.soulfire.server.settings.lib.property.BooleanProperty;
import net.pistonmaster.soulfire.server.settings.lib.property.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Set;

public class ChatMessageLogger implements InternalExtension {
    private static final Logger logger = LoggerFactory.getLogger(ChatMessageLogger.class);
    private static final Set<Long> chatMessages = new LinkedHashSet<>(50); // in case of huge lag
    public static void onMessage(ChatMessageReceiveEvent event) {
        if (event.connection().settingsHolder().get(ChatMessageSettings.ENABLED)) {
            StringBuilder content = new StringBuilder();
            // if it's a player message, add username
            if (event.isFromPlayer())
                content.append("<").append(event.sender().senderName()).append("> ");
            else content.append("<Server> ");

            content.append(event.parseToText());

            // usage of synchronized method so that the chatMessages set is not modified while being iterated
            logChatMessage(content.toString(), event.timestamp());
        }
    }

    private static synchronized void logChatMessage(String message, Long timestamp) {
        if (chatMessages.contains(timestamp)) return;
        if (chatMessages.size() == 5)
            chatMessages.remove(chatMessages.iterator().next());

        chatMessages.add(timestamp);
        logger.info(message);
    }

    @EventHandler
    public static void onSettingsManagerInit(SettingsRegistryInitEvent event) {
        event.settingsRegistry().addClass(ChatMessageSettings.class, "Chat Message Logger");
    }

    @Override
    public void onLoad() {
        SoulFireAPI.registerListeners(ChatMessageLogger.class);
        PluginHelper.registerBotEventConsumer(ChatMessageReceiveEvent.class, ChatMessageLogger::onMessage);
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
    }
}
