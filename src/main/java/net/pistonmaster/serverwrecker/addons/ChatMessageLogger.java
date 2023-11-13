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

import net.lenni0451.lambdaevents.EventHandler;
import net.pistonmaster.serverwrecker.ServerWreckerServer;
import net.pistonmaster.serverwrecker.api.AddonCLIHelper;
import net.pistonmaster.serverwrecker.api.AddonHelper;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.attack.BotConnectionInitEvent;
import net.pistonmaster.serverwrecker.api.event.bot.ChatMessageReceiveEvent;
import net.pistonmaster.serverwrecker.api.event.lifecycle.AddonPanelInitEvent;
import net.pistonmaster.serverwrecker.api.event.lifecycle.CommandManagerInitEvent;
import net.pistonmaster.serverwrecker.gui.libs.PresetJCheckBox;
import net.pistonmaster.serverwrecker.gui.navigation.NavigationItem;
import net.pistonmaster.serverwrecker.settings.lib.SettingsDuplex;
import net.pistonmaster.serverwrecker.settings.lib.SettingsObject;
import net.pistonmaster.serverwrecker.settings.lib.SettingsProvider;
import org.slf4j.Logger;
import picocli.CommandLine;

import javax.swing.*;
import java.awt.*;
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
        AddonHelper.registerAttackEventConsumer(BotConnectionInitEvent.class, ChatMessageLogger::onConnectionInit);
    }

    public static void onConnectionInit(BotConnectionInitEvent event) {
        var chatMessageSettings = event.connection().settingsHolder().get(ChatMessageSettings.class);
        if (!chatMessageSettings.logChat()) {
            return;
        }

        event.connection().eventBus().register(ChatMessageReceiveEvent.class,
                new BotChatListener(event.connection().connectionId(), event.connection().logger(),
                        event.connection().executorManager().newScheduledExecutorService("Chat"),
                        new LinkedHashSet<>(), chatMessageSettings));
    }

    @EventHandler
    public static void onAddonPanel(AddonPanelInitEvent event) {
        event.navigationItems().add(new ChatMessagePanel(ServerWreckerAPI.getServerWrecker()));
    }

    @EventHandler
    public static void onCommandLine(CommandManagerInitEvent event) {
        AddonCLIHelper.registerCommands(event.commandLine(), ChatMessageSettings.class, new ChatMessageCommand());
    }

    private record BotChatListener(UUID connectionId, Logger logger, ScheduledExecutorService executor,
                                   Set<String> messageQueue, ChatMessageSettings chatMessageSettings)
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
            }, 0, chatMessageSettings.interval(), TimeUnit.SECONDS);
        }

        @Override
        public void accept(ChatMessageReceiveEvent event) {
            if (!event.connection().connectionId().equals(connectionId)) {
                return;
            }

            messageQueue.add(event.parseToText().replace("\n", "\\n"));
        }
    }

    private static class ChatMessagePanel extends NavigationItem implements SettingsDuplex<ChatMessageSettings> {
        private final JCheckBox logChat;
        private final JSpinner interval;

        ChatMessagePanel(ServerWreckerServer serverWreckerServer) {
            super();
            serverWreckerServer.getSettingsManager().registerDuplex(ChatMessageSettings.class, this);

            setLayout(new GridLayout(0, 2));

            add(new JLabel("Log chat to terminal?"));
            logChat = new PresetJCheckBox(ChatMessageSettings.DEFAULT_LOG_CHAT);
            add(logChat);

            add(new JLabel("Interval (Seconds)"));
            interval = new JSpinner(new SpinnerNumberModel(ChatMessageSettings.DEFAULT_INTERVAL, 1, 1000, 1));
            add(interval);
        }

        @Override
        public String getNavigationName() {
            return "Chat Logger";
        }

        @Override
        public String getNavigationId() {
            return "chat-logger";
        }

        @Override
        public void onSettingsChange(ChatMessageSettings settings) {
            logChat.setSelected(settings.logChat());
            interval.setValue(settings.interval());
        }

        @Override
        public ChatMessageSettings collectSettings() {
            return new ChatMessageSettings(
                    logChat.isSelected(),
                    (int) interval.getValue()
            );
        }
    }

    private static class ChatMessageCommand implements SettingsProvider<ChatMessageSettings> {
        @CommandLine.Option(names = {"--log-chat"}, description = "Log chat to terminal")
        private boolean logChat = ChatMessageSettings.DEFAULT_LOG_CHAT;
        @CommandLine.Option(names = {"--chat-interval"}, description = "Minimum delay between logging chat")
        private int interval = ChatMessageSettings.DEFAULT_INTERVAL;

        @Override
        public ChatMessageSettings collectSettings() {
            return new ChatMessageSettings(
                    logChat,
                    interval
            );
        }
    }

    private record ChatMessageSettings(
            boolean logChat,
            int interval
    ) implements SettingsObject {
        public static final boolean DEFAULT_LOG_CHAT = true;
        public static final int DEFAULT_INTERVAL = 2;
    }
}
