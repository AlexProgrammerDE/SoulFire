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
package com.soulfiremc.server.plugins;

import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.api.PluginHelper;
import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.api.event.bot.ChatMessageReceiveEvent;
import com.soulfiremc.server.api.event.lifecycle.SettingsRegistryInitEvent;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.BooleanProperty;
import com.soulfiremc.server.settings.property.Property;
import com.soulfiremc.server.util.ExpiringSet;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer;
import net.kyori.ansi.ColorLevel;
import net.lenni0451.lambdaevents.EventHandler;
import org.fusesource.jansi.AnsiConsole;

@Slf4j
public class ChatMessageLogger implements InternalPlugin {
  public static final ANSIComponentSerializer ANSI_MESSAGE_SERIALIZER =
    ANSIComponentSerializer.builder()
      .flattener(SoulFireServer.FLATTENER)
      .colorLevel(
        switch (AnsiConsole.out().getColors()) {
          case Colors16 -> ColorLevel.INDEXED_16;
          case Colors256 -> ColorLevel.INDEXED_256;
          case TrueColor -> ColorLevel.TRUE_COLOR;
        })
      .build();
  private static final ExpiringSet<String> CHAT_MESSAGES = new ExpiringSet<>(5, TimeUnit.SECONDS);

  public static void onMessage(ChatMessageReceiveEvent event) {
    if (!event.connection().settingsHolder().get(ChatMessageSettings.ENABLED)) {
      return;
    }

    var sender =
      Optional.ofNullable(event.sender())
        .map(ChatMessageReceiveEvent.ChatMessageSender::senderName)
        .orElse("Server");
    var message = Component.text("<%s> ".formatted(sender)).append(event.message());

    var ansiMessage = ANSI_MESSAGE_SERIALIZER.serialize(message);

    // usage of synchronized method so that the chatMessages set is not modified while being
    // iterated
    logChatMessage(ansiMessage);
  }

  private static synchronized void logChatMessage(String message) {
    if (CHAT_MESSAGES.contains(message)) {
      return;
    }

    CHAT_MESSAGES.add(message);
    log.info(message);
  }

  @EventHandler
  public static void onSettingsRegistryInit(SettingsRegistryInitEvent event) {
    event.settingsRegistry().addClass(ChatMessageSettings.class, "Chat Message Logger");
  }

  @Override
  public void onLoad() {
    SoulFireAPI.registerListeners(ChatMessageLogger.class);
    PluginHelper.registerBotEventConsumer(
      ChatMessageReceiveEvent.class, ChatMessageLogger::onMessage);
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  private static class ChatMessageSettings implements SettingsObject {
    private static final Property.Builder BUILDER = Property.builder("chat-message-logger");
    public static final BooleanProperty ENABLED =
      BUILDER.ofBoolean(
        "enabled",
        "Log chat to terminal",
        new String[] {"--log-chat"},
        "Log all received chat messages to the terminal",
        true);
  }
}
