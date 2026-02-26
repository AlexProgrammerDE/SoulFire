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

/// Trigger node that fires when a container is opened.
/// Outputs: bot, ID of container, Name of container, Type of container.
public class OnContainerOpenNode extends AbstractScriptNode {
  public static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("trigger.on_container_open")
    .displayName("On Container Open")
    .category(CategoryRegistry.TRIGGERS)
    .addInputs()
    .addOutputs(
      PortDefinition.execOut(),
      PortDefinition.output("bot", "Bot", PortType.BOT, "The bot that opened the container"),
      PortDefinition.output("containerId", "Container ID", PortType.NUMBER, "The ID of opened container"),
      PortDefinition.output("containerName", "Container Name", PortType.STRING, "The name of opened container"),
      PortDefinition.output("containerType", "Container Type", PortType.STRING, "The type of opened container")
    )
    .isTrigger(true)
    .description("Fires when a any container has been opened")
    .icon("package-open")
    .color("#4CAF50")
    .addKeywords("inventory", "container", "menu", "open")
    .build();

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var bot = getBotInput(inputs);
    var containerId = getIntInput(inputs, "containerId", -1);
    var containerName = getStringInput(inputs, "containerName", "");
    var containerType = getStringInput(inputs, "containerType", "");

    return completedMono(results(
      "bot", bot,
      "containerId", containerId,
      "containerName", containerName,
      "containerType", containerType
    ));
  }
}
