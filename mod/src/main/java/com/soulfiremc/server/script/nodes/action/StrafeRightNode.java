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
import reactor.core.publisher.Mono;

import java.util.Map;

/// Action node that toggles the bot's right strafe movement (D key).
/// Input: enabled (boolean) - whether to strafe right
public final class StrafeRightNode extends AbstractScriptNode {
  public static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("action.strafe_right")
    .displayName("Strafe Right")
    .category(CategoryRegistry.ACTIONS)
    .addInputs(
      PortDefinition.execIn(),
      PortDefinition.botIn(),
      PortDefinition.inputWithDefault("enabled", "Enabled", PortType.BOOLEAN, "true", "Whether to strafe right")
    )
    .addOutputs(
      PortDefinition.execOut()
    )
    .description("Sets the bot's right strafe movement state (D key)")
    .icon("arrow-right")
    .color("#FF9800")
    .addKeywords("move", "strafe", "right", "d-key", "wasd")
    .build();

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var bot = requireBot(inputs);
    var enabled = getBooleanInput(inputs, "enabled", true);

    bot.controlState().right(enabled);

    return completedEmptyMono();
  }
}
