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
package com.soulfiremc.server.script.nodes.data;

import com.soulfiremc.server.script.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Data node that returns all connected bots in the instance.
/// Output: bots (List of BotConnection)
public final class GetBotsNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("data.get_bots")
    .displayName("Get Bots")
    .category(NodeCategory.DATA)
    .addInputs()
    .addOutputs(
      PortDefinition.listOutput("bots", "Bots", PortType.BOT, "List of all connected bots")
    )
    .description("Returns all connected bots in the instance")
    .icon("users")
    .color("#9C27B0")
    .addKeywords("bots", "all", "list", "connections")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var bots = new ArrayList<>(runtime.instance().botConnections().values());
    return completed(result("bots", bots));
  }
}
