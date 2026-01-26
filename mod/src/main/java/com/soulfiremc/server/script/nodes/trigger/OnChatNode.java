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
package com.soulfiremc.server.script.nodes.trigger;

import com.soulfiremc.server.script.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Trigger node that fires when a chat message is received.
/// Outputs: bot (the bot that received the message), message (Component), messagePlainText, timestamp
public final class OnChatNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("trigger.on_chat")
    .displayName("On Chat")
    .category(NodeCategory.TRIGGERS)
    .addInputs()
    .addOutputs(
      PortDefinition.execOut(),
      PortDefinition.output("bot", "Bot", PortType.BOT, "The bot that received the message"),
      PortDefinition.output("message", "Message", PortType.ANY, "The chat message component"),
      PortDefinition.output("messagePlainText", "Message (Plain Text)", PortType.STRING, "The chat message as plain text"),
      PortDefinition.output("timestamp", "Timestamp", PortType.NUMBER, "Timestamp when the message was received")
    )
    .isTrigger(true)
    .description("Fires when a chat message is received")
    .icon("message-square")
    .color("#4CAF50")
    .addKeywords("chat", "message", "text", "say", "talk", "receive")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    // Event data is passed through inputs from the trigger system
    var bot = getBotInput(inputs);
    var message = inputs.get("message");
    var messagePlainText = getStringInput(inputs, "messagePlainText", "");
    var timestamp = getLongInput(inputs, "timestamp", System.currentTimeMillis());

    // Output data so it can be wired to downstream nodes
    return completed(results(
      "bot", bot,
      "message", message,
      "messagePlainText", messagePlainText,
      "timestamp", timestamp
    ));
  }
}
