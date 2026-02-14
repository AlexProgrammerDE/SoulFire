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

/// Data node that gets the bot's current rotation.
/// Outputs: yaw, pitch (float, in degrees)
public final class GetRotationNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("data.get_rotation")
    .displayName("Get Rotation")
    .category(CategoryRegistry.DATA)
    .addInputs(
      PortDefinition.execIn()
    )
    .addOutputs(
      PortDefinition.execOut(),
      PortDefinition.output("yaw", "Yaw", PortType.NUMBER, "Horizontal rotation (-180 to 180)"),
      PortDefinition.output("pitch", "Pitch", PortType.NUMBER, "Vertical rotation (-90 to 90)")
    )
    .description("Gets the bot's current rotation (yaw and pitch)")
    .icon("compass")
    .color("#9C27B0")
    .addKeywords("rotation", "yaw", "pitch", "angle", "direction")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var bot = requireBot(inputs);
    var player = bot.minecraft().player;

    if (player == null) {
      return completedMono(results("yaw", 0.0f, "pitch", 0.0f));
    }

    return completedMono(results(
      "yaw", player.getYRot(),
      "pitch", player.getXRot()
    ));
  }
}
