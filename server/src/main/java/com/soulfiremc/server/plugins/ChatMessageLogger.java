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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.api.InternalPlugin;
import com.soulfiremc.server.api.PluginInfo;
import com.soulfiremc.server.api.event.bot.ChatMessageReceiveEvent;
import com.soulfiremc.server.api.event.lifecycle.InstanceSettingsRegistryInitEvent;
import com.soulfiremc.server.settings.lib.InstanceSettingsSource;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.BooleanProperty;
import com.soulfiremc.server.settings.property.ImmutableBooleanProperty;
import com.soulfiremc.server.settings.property.ImmutableIntProperty;
import com.soulfiremc.server.settings.property.IntProperty;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer;
import net.kyori.ansi.ColorLevel;
import net.lenni0451.lambdaevents.EventHandler;
import org.fusesource.jansi.AnsiConsole;
import org.pf4j.Extension;

import java.util.concurrent.TimeUnit;

@Slf4j
@Extension
public class ChatMessageLogger extends InternalPlugin {
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
  private static final Cache<String, Integer> CHAT_MESSAGES = Caffeine.newBuilder()
    .expireAfterWrite(5, TimeUnit.SECONDS)
    .build();

  public ChatMessageLogger() {
    super(new PluginInfo(
      "chat-message-logger",
      "1.0.0",
      "Logs all received chat messages to the terminal\nIncludes deduplication to prevent spamming the same message too often",
      "AlexProgrammerDE",
      "GPL-3.0",
      "https://soulfiremc.com"
    ));
  }

  @EventHandler
  public static void onMessage(ChatMessageReceiveEvent event) {
    var settingsSource = event.connection().settingsSource();
    if (!settingsSource.get(ChatMessageSettings.ENABLED)) {
      return;
    }

    var message = event.message();

    var ansiMessage = ANSI_MESSAGE_SERIALIZER.serialize(message);

    // usage of synchronized method so that the chatMessages set is not modified while being
    // iterated
    logChatMessage(settingsSource, ansiMessage);
  }

  private static synchronized void logChatMessage(InstanceSettingsSource settingsSource, String message) {
    var deduplicateAmount = settingsSource.get(ChatMessageSettings.DEDUPLICATE_AMOUNT);
    var messageCount = CHAT_MESSAGES.get(message, (key) -> 0);

    if (messageCount < deduplicateAmount) {
      log.info(message);
      CHAT_MESSAGES.put(message, messageCount + 1);
    }
  }

  @EventHandler
  public void onSettingsRegistryInit(InstanceSettingsRegistryInitEvent event) {
    event.settingsRegistry().addClass(ChatMessageSettings.class, "Chat Message Logger", this, "logs");
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  private static class ChatMessageSettings implements SettingsObject {
    private static final String NAMESPACE = "chat-message-logger";
    public static final BooleanProperty ENABLED =
      ImmutableBooleanProperty.builder()
        .namespace(NAMESPACE)
        .key("enabled")
        .uiName("Log chat to terminal")
        .description("Log all received chat messages to the terminal")
        .defaultValue(true)
        .build();
    public static final IntProperty DEDUPLICATE_AMOUNT =
      ImmutableIntProperty.builder()
        .namespace(NAMESPACE)
        .key("deduplicate-amount")
        .uiName("Deduplicate amount")
        .description("How often should the same message be logged before it will not be logged again? (within 5 seconds)")
        .defaultValue(1)
        .minValue(1)
        .maxValue(Integer.MAX_VALUE)
        .stepValue(1)
        .build();
  }
}
