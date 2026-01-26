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

import com.soulfiremc.server.script.AbstractScriptNode;
import com.soulfiremc.server.script.ScriptContext;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Trigger node that fires when a chat message is received.
/// Outputs: bot (the bot that received the message), message (Component), messagePlainText, timestamp
public final class OnChatNode extends AbstractScriptNode {
  public static final String TYPE = "trigger.on_chat";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean isTrigger() {
    return true;
  }

  @Override
  public CompletableFuture<Map<String, Object>> execute(ScriptContext context, Map<String, Object> inputs) {
    // Event data is passed through inputs from the trigger system
    var bot = getBotInput(inputs);
    var message = inputs.get("message");
    var messagePlainText = getStringInput(inputs, "messagePlainText", "");
    var timestamp = getLongInput(inputs, "timestamp", System.currentTimeMillis());

    // Set current bot in context for downstream nodes
    context.setCurrentBot(bot);

    return completed(results(
      "bot", bot,
      "message", message,
      "messagePlainText", messagePlainText,
      "timestamp", timestamp
    ));
  }
}
