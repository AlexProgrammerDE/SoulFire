/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.script.nodes.ai;

import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import com.openai.models.completions.CompletionUsage;
import com.soulfiremc.server.script.*;
import com.soulfiremc.server.settings.instance.AISettings;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// AI node that sends a prompt to an LLM and returns the response.
/// Uses the bot's configured AI settings.
public final class LLMChatNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("ai.llm_chat")
    .displayName("LLM Chat")
    .category(CategoryRegistry.AI)
    .addInputs(
      PortDefinition.execIn(),
      PortDefinition.input("bot", "Bot", PortType.BOT, "Bot whose AI settings to use"),
      PortDefinition.input("prompt", "Prompt", PortType.STRING, "The prompt/message to send"),
      PortDefinition.inputWithDefault("systemPrompt", "System Prompt", PortType.STRING, "\"\"", "Optional system prompt for context"),
      PortDefinition.inputWithDefault("model", "Model", PortType.STRING, "\"\"", "Model override (empty = use bot default)"),
      PortDefinition.inputWithDefault("temperature", "Temperature", PortType.NUMBER, "0.7", "Creativity (0.0-2.0)"),
      PortDefinition.inputWithDefault("maxTokens", "Max Tokens", PortType.NUMBER, "1024", "Maximum response tokens")
    )
    .addOutputs(
      PortDefinition.output("exec_success", "Success", PortType.EXEC, "Executes on successful response"),
      PortDefinition.output("exec_error", "Error", PortType.EXEC, "Executes on failed request"),
      PortDefinition.output("response", "Response", PortType.STRING, "LLM response text"),
      PortDefinition.output("success", "Success", PortType.BOOLEAN, "Whether the request succeeded"),
      PortDefinition.output("errorMessage", "Error Message", PortType.STRING, "Error message if failed"),
      PortDefinition.output("tokensUsed", "Tokens Used", PortType.NUMBER, "Total tokens used")
    )
    .description("Sends a prompt to an LLM and returns the response using the bot's AI settings")
    .icon("brain")
    .color("#A855F7")
    .addKeywords("ai", "llm", "gpt", "chat", "openai", "prompt", "completion", "generate")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var bot = requireBot(inputs);
    var prompt = getStringInput(inputs, "prompt", "");
    var systemPrompt = getStringInput(inputs, "systemPrompt", "");
    var model = getStringInput(inputs, "model", "");
    var temperature = getDoubleInput(inputs, "temperature", 0.7);
    var maxTokens = getIntInput(inputs, "maxTokens", 1024);

    if (prompt.isEmpty()) {
      return completed(results(
        "response", "",
        "success", false,
        "errorMessage", "Prompt is required",
        "tokensUsed", 0
      ));
    }

    var future = CompletableFuture.supplyAsync(() -> {
      try {
        var settingsSource = bot.settingsSource();
        var openAiClient = AISettings.create(settingsSource);

        // Build messages
        var messages = new ArrayList<ChatCompletionMessageParam>();
        if (!systemPrompt.isEmpty()) {
          messages.add(ChatCompletionMessageParam.ofSystem(
            ChatCompletionSystemMessageParam.builder()
              .content(ChatCompletionSystemMessageParam.Content.ofText(systemPrompt))
              .build()
          ));
        }
        messages.add(ChatCompletionMessageParam.ofUser(
          ChatCompletionUserMessageParam.builder()
            .content(ChatCompletionUserMessageParam.Content.ofText(prompt))
            .build()
        ));

        // Build request
        var requestBuilder = ChatCompletionCreateParams.builder()
          .messages(messages)
          .temperature(temperature)
          .maxCompletionTokens(maxTokens);

        // Use model override or a default model
        if (!model.isEmpty()) {
          requestBuilder.model(model);
        } else {
          requestBuilder.model("gpt-4o-mini");
        }

        // Execute
        var completion = openAiClient.chat().completions().create(requestBuilder.build());
        var choice = completion.choices().getFirst();
        var responseText = choice.message().content().orElse("");
        var tokensUsed = completion.usage().map(CompletionUsage::totalTokens).orElse(0L);

        return results(
          "response", responseText,
          "success", true,
          "errorMessage", "",
          "tokensUsed", tokensUsed
        );
      } catch (Exception e) {
        return results(
          "response", "",
          "success", false,
          "errorMessage", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(),
          "tokensUsed", 0
        );
      }
    });

    runtime.addPendingOperation(future);
    return future;
  }
}
