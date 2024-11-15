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

import com.soulfiremc.server.api.InternalPlugin;
import com.soulfiremc.server.api.PluginInfo;
import com.soulfiremc.server.api.event.bot.ChatMessageReceiveEvent;
import com.soulfiremc.server.api.event.lifecycle.InstanceSettingsRegistryInitEvent;
import com.soulfiremc.server.api.metadata.MetadataKey;
import com.soulfiremc.server.settings.AISettings;
import com.soulfiremc.server.settings.lib.SettingsObject;
import com.soulfiremc.server.settings.property.BooleanProperty;
import com.soulfiremc.server.settings.property.Property;
import com.soulfiremc.server.settings.property.StringProperty;
import io.github.ollama4j.models.chat.OllamaChatMessageRole;
import io.github.ollama4j.models.chat.OllamaChatRequestBuilder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lenni0451.lambdaevents.EventHandler;
import org.pf4j.Extension;

@Slf4j
@Extension
public class AIChatBot extends InternalPlugin {
  private static final MetadataKey<OllamaChatRequestBuilder> PLAYER_CONVERSATIONS = MetadataKey.of("ai_chat_bot", "conversations", OllamaChatRequestBuilder.class);

  public AIChatBot() {
    super(new PluginInfo(
      "ai-chat-bot",
      "1.0.0",
      "Allow players to chat with an AI",
      "AlexProgrammerDE",
      "GPL-3.0"
    ));
  }

  @EventHandler
  public static void onMessage(ChatMessageReceiveEvent event) {
    var settingsSource = event.connection().settingsSource();
    if (!settingsSource.get(AIChatBotSettings.ENABLED)) {
      return;
    }

    var message = event.parseToPlainText();
    if (!message.contains(settingsSource.get(AIChatBotSettings.KEYWORD))) {
      return;
    }

    try {
      var api = AISettings.create(settingsSource);
      var model = settingsSource.get(AIChatBotSettings.MODEL);
      AISettings.pullIfNecessary(api, model, settingsSource);

      var builder = event.connection().metadata().getOrSet(PLAYER_CONVERSATIONS, () -> {
        var newBuilder = OllamaChatRequestBuilder.getInstance(model);
        newBuilder.withMessage(OllamaChatMessageRole.SYSTEM, settingsSource.get(AIChatBotSettings.PROMPT));
        return newBuilder;
      });
      var requestModel = builder.withMessage(OllamaChatMessageRole.USER, message)
        .build();

      log.debug("Chatting with AI: {}", requestModel);
      var chatResult = api.chat(requestModel);

      event.connection().metadata().set(PLAYER_CONVERSATIONS, builder.withMessages(chatResult.getChatHistory()));

      log.debug("AI response: {}", chatResult.getResponse());
      event.connection().botControl().sendMessage(chatResult.getResponse());
    } catch (Exception e) {
      log.error("Failed to chat with AI", e);
    }
  }

  @EventHandler
  public void onSettingsRegistryInit(InstanceSettingsRegistryInitEvent event) {
    event.settingsRegistry().addClass(AIChatBotSettings.class, "AI Chat Bot", this, "bot-message-square");
  }

  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  private static class AIChatBotSettings implements SettingsObject {
    private static final Property.Builder BUILDER = Property.builder("ai-chat-bot");
    public static final BooleanProperty ENABLED =
      BUILDER.ofBoolean(
        "enabled",
        "Enable AI Chat Bot",
        "Enable the AI Chat Bot",
        false);
    public static final StringProperty PROMPT =
      BUILDER.ofString(
        "prompt",
        "AI System prompt",
        "What the bot is instructed to say",
        """
          You are a Minecraft chat bot, you chat with players.
          You must not say more than 128 characters or more than 2 sentences per response.
          Keep responses short, but conversational.
          You must not say anything that is not safe for work.
          You will take any roleplay seriously and follow the player's lead.
          You cannot interact with the Minecraft world except by chatting.
          Use prefixes to express emotions, do not put names as prefixes.
          """.replace("\n", " "));
    public static final StringProperty MODEL =
      BUILDER.ofString(
        "model",
        "AI Model",
        "What AI model should be used for inference",
        "nemotron-mini");
    public static final StringProperty KEYWORD =
      BUILDER.ofString(
        "keyword",
        "Keyword",
        "Only respond to messages containing this keyword",
        "!ai");
  }
}
