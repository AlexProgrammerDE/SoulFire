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

/// Trigger node that fires when the bot joins the server.
/// Outputs: bot (the bot that joined), serverAddress, username
public final class OnJoinNode extends AbstractScriptNode {
  public static final String TYPE = "trigger.on_join";

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
    var bot = getBotInput(inputs);

    // Set current bot in context for downstream nodes
    context.setCurrentBot(bot);

    var serverAddress = bot != null ? bot.serverAddress().toString() : "";
    var username = bot != null ? bot.accountName() : "";

    return completed(results(
      "bot", bot,
      "serverAddress", serverAddress,
      "username", username
    ));
  }
}
