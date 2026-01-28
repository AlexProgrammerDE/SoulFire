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
package com.soulfiremc.server.script.nodes.integration;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.soulfiremc.server.script.*;
import com.soulfiremc.server.util.ReactorHttpHelper;
import io.netty.buffer.Unpooled;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Integration node that sends messages to a Discord webhook.
public final class DiscordWebhookNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("integration.discord_webhook")
    .displayName("Discord Webhook")
    .category(CategoryRegistry.INTEGRATION)
    .addInputs(
      PortDefinition.execIn(),
      PortDefinition.input("webhookUrl", "Webhook URL", PortType.STRING, "Discord webhook URL"),
      PortDefinition.inputWithDefault("content", "Content", PortType.STRING, "\"\"", "Message content (max 2000 chars)"),
      PortDefinition.inputWithDefault("username", "Username", PortType.STRING, "\"\"", "Override webhook username"),
      PortDefinition.inputWithDefault("avatarUrl", "Avatar URL", PortType.STRING, "\"\"", "Override webhook avatar"),
      PortDefinition.inputWithDefault("embedJson", "Embed JSON", PortType.STRING, "\"\"", "Optional embed as JSON")
    )
    .addOutputs(
      PortDefinition.output("exec_success", "Success", PortType.EXEC, "Success execution path"),
      PortDefinition.output("exec_error", "Error", PortType.EXEC, "Error execution path"),
      PortDefinition.output("success", "Success", PortType.BOOLEAN, "Whether message was sent"),
      PortDefinition.output("errorMessage", "Error Message", PortType.STRING, "Error details if failed")
    )
    .description("Sends messages to a Discord webhook")
    .icon("message-circle")
    .color("#5865F2")
    .addKeywords("discord", "webhook", "notify", "message", "alert", "integration")
    .build();

  private static final Gson GSON = new Gson();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var webhookUrl = getStringInput(inputs, "webhookUrl", "");
    var content = getStringInput(inputs, "content", "");
    var username = getStringInput(inputs, "username", "");
    var avatarUrl = getStringInput(inputs, "avatarUrl", "");
    var embedJson = getStringInput(inputs, "embedJson", "");

    if (webhookUrl.isEmpty()) {
      return completed(results(
        "success", false,
        "errorMessage", "Webhook URL is required"
      ));
    }

    if (content.isEmpty() && embedJson.isEmpty()) {
      return completed(results(
        "success", false,
        "errorMessage", "Either content or embed is required"
      ));
    }

    var payload = new JsonObject();
    if (!content.isEmpty()) {
      // Truncate to Discord's limit
      payload.addProperty("content", content.length() > 2000 ? content.substring(0, 2000) : content);
    }
    if (!username.isEmpty()) {
      payload.addProperty("username", username);
    }
    if (!avatarUrl.isEmpty()) {
      payload.addProperty("avatar_url", avatarUrl);
    }

    if (!embedJson.isEmpty()) {
      try {
        var embed = JsonParser.parseString(embedJson);
        var embeds = new JsonArray();
        embeds.add(embed);
        payload.add("embeds", embeds);
      } catch (Exception e) {
        return completed(results(
          "success", false,
          "errorMessage", "Invalid embed JSON: " + e.getMessage()
        ));
      }
    }

    var client = ReactorHttpHelper.createReactorClient(null, true);
    var payloadBytes = GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);

    var future = client
      .post()
      .uri(webhookUrl)
      .send(Mono.just(Unpooled.wrappedBuffer(payloadBytes)))
      .responseSingle((resp, buf) -> {
        var statusCode = resp.status().code();
        if (statusCode >= 200 && statusCode < 300) {
          return Mono.just(results(
            "success", true,
            "errorMessage", ""
          ));
        } else {
          return buf.asString(StandardCharsets.UTF_8)
            .defaultIfEmpty("")
            .map(body -> results(
              "success", false,
              "errorMessage", "HTTP " + statusCode + ": " + body
            ));
        }
      })
      .onErrorResume(e -> Mono.just(results(
        "success", false,
        "errorMessage", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()
      )))
      .toFuture();

    runtime.addPendingOperation(future);
    return future;
  }
}
