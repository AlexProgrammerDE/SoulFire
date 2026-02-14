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
package com.soulfiremc.server.script.nodes.action;

import com.soulfiremc.server.script.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Action node that toggles or sets the bot's sprinting state.
/// Input: enabled (boolean) - whether to sprint
public final class SprintNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("action.sprint")
    .displayName("Sprint")
    .category(CategoryRegistry.ACTIONS)
    .addInputs(
      PortDefinition.execIn(),
      PortDefinition.inputWithDefault("enabled", "Enabled", PortType.BOOLEAN, "true", "Whether to sprint")
    )
    .addOutputs(
      PortDefinition.execOut()
    )
    .description("Sets the bot's sprinting state")
    .icon("fast-forward")
    .color("#FF9800")
    .addKeywords("sprint", "run", "fast", "speed")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var bot = requireBot(inputs);
    var enabled = getBooleanInput(inputs, "enabled", true);

    bot.controlState().sprint(enabled);

    return completedEmpty();
  }
}
