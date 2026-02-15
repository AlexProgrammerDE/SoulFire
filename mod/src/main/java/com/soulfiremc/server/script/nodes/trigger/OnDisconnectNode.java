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
import reactor.core.publisher.Mono;

import java.util.Map;

/// Trigger node that fires when the bot is disconnected from the server.
/// Outputs: bot (the disconnected bot), reason (disconnect message as string)
public final class OnDisconnectNode extends AbstractScriptNode {
  public static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("trigger.on_disconnect")
    .displayName("On Disconnect")
    .category(CategoryRegistry.TRIGGERS)
    .addInputs()
    .addOutputs(
      PortDefinition.execOut(),
      PortDefinition.output("bot", "Bot", PortType.BOT, "The bot that was disconnected"),
      PortDefinition.output("reason", "Reason", PortType.STRING, "The disconnect reason message")
    )
    .isTrigger(true)
    .description("Fires when the bot is disconnected from the server")
    .icon("unplug")
    .color("#4CAF50")
    .addKeywords("disconnect", "leave", "kick", "timeout", "connection")
    .build();

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var bot = getBotInput(inputs);
    var reason = getStringInput(inputs, "reason", "");

    return completedMono(results(
      "bot", bot,
      "reason", reason
    ));
  }
}
