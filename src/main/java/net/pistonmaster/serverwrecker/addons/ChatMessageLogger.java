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
package net.pistonmaster.serverwrecker.addons;

import net.kyori.event.EventSubscriber;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.UnregisterCleanup;
import net.pistonmaster.serverwrecker.api.event.bot.ChatMessageReceiveEvent;
import net.pistonmaster.serverwrecker.api.event.bot.PreBotConnectEvent;
import net.pistonmaster.serverwrecker.protocol.BotConnection;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChatMessageLogger implements InternalAddon, EventSubscriber<PreBotConnectEvent> {
    @Override
    public void onLoad() {
        ServerWreckerAPI.registerListener(PreBotConnectEvent.class, this);
    }

    @Override
    public void on(@NonNull PreBotConnectEvent event) throws Throwable {
        ServerWreckerAPI.registerListener(ChatMessageReceiveEvent.class,
                event.connection().cleanup(new BotChatListener(event.connection(),
                        Executors.newScheduledThreadPool(1), new LinkedHashSet<>())));
    }

    private record BotChatListener(BotConnection connection, ScheduledExecutorService executor,
                                   Set<String> messageQueue)
            implements EventSubscriber<ChatMessageReceiveEvent>, UnregisterCleanup {
        public BotChatListener {
            executor.scheduleAtFixedRate(() -> {
                Iterator<String> iter = messageQueue.iterator();
                while (!messageQueue.isEmpty()) {
                    String message = iter.next();
                    iter.remove();
                    if (!Objects.nonNull(message)) {
                        continue;
                    }

                    connection.logger().info("[Chat] Received Message: {}", message);
                }
            }, 0, 2, TimeUnit.SECONDS);
        }

        @Override
        public void on(@NonNull ChatMessageReceiveEvent event) {
            messageQueue.add(event.parseToText().replace("\n", "\\n"));
        }

        @Override
        public void cleanup() {
            executor.shutdown();
        }
    }
}
