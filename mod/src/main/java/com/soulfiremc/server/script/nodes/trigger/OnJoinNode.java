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

/// Trigger node that fires when the bot joins the server.
/// Outputs: bot (the bot that joined), serverAddress, username
public final class OnJoinNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("trigger.on_join")
    .displayName("On Join")
    .category(NodeCategory.TRIGGERS)
    .addInputs()
    .addOutputs(
      PortDefinition.execOut(),
      PortDefinition.output("bot", "Bot", PortType.BOT, "The bot that joined"),
      PortDefinition.output("serverAddress", "Server Address", PortType.STRING, "The server address"),
      PortDefinition.output("username", "Username", PortType.STRING, "The bot's username")
    )
    .isTrigger(true)
    .description("Fires when the bot joins the server")
    .icon("log-in")
    .color("#4CAF50")
    .addKeywords("join", "connect", "login", "server", "spawn")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var bot = getBotInput(inputs);

    var serverAddress = bot != null ? bot.serverAddress().toString() : "";
    var username = bot != null ? bot.accountName() : "";

    // Output data so it can be wired to downstream nodes
    return completed(results(
      "bot", bot,
      "serverAddress", serverAddress,
      "username", username
    ));
  }
}
