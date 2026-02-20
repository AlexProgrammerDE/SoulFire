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

/// Action node that respawns the bot after death.
public final class RespawnNode extends AbstractScriptNode {
  public static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("action.respawn")
    .displayName("Respawn")
    .category(CategoryRegistry.ACTIONS)
    .addInputs(
      PortDefinition.execIn(),
      PortDefinition.botIn()
    )
    .addOutputs(
      PortDefinition.execOut()
    )
    .description("Respawns the bot after death")
    .icon("repeat")
    .color("#FF9800")
    .addKeywords("respawn", "death", "revive", "alive")
    .build();

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var bot = requireBot(inputs);

    runOnTickThread(runtime, bot, () -> {
      var player = bot.minecraft().player;
      if (player != null) {
        player.respawn();
      }
    });

    return completedEmptyMono();
  }
}
