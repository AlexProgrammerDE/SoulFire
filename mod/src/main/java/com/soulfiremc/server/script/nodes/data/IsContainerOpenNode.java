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
import reactor.core.publisher.Mono;

import java.util.Map;

/// Data node that checks if a container or inventory screen is currently open.
public final class IsContainerOpenNode extends AbstractScriptNode {
  public static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("data.is_container_open")
    .displayName("Is Container Open")
    .category(CategoryRegistry.DATA)
    .addInputs(
      PortDefinition.execIn()
    )
    .addOutputs(
      PortDefinition.execOut(),
      PortDefinition.output("isOpen", "Is Open", PortType.BOOLEAN, "Whether a container or inventory screen is open")
    )
    .description("Checks if a container or inventory screen is open")
    .icon("package-search")
    .color("#9C27B0")
    .addKeywords("container", "inventory", "open", "screen", "check")
    .build();

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var bot = requireBot(inputs);
    var player = bot.minecraft().player;

    if (player == null) {
      return completedMono(result("isOpen", false));
    }

    return completedMono(result("isOpen", player.hasContainerOpen()));
  }
}
