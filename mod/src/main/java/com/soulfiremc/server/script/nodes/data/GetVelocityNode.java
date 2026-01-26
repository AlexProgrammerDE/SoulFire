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
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Data node that gets the bot's current velocity.
/// Outputs: velocity (Vec3), speed (magnitude)
public final class GetVelocityNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("data.get_velocity")
    .displayName("Get Velocity")
    .category(CategoryRegistry.DATA)
    .addInputs(
      PortDefinition.input("bot", "Bot", PortType.BOT, "The bot to get velocity from")
    )
    .addOutputs(
      PortDefinition.output("velocity", "Velocity", PortType.VECTOR3, "Current velocity vector"),
      PortDefinition.output("speed", "Speed", PortType.NUMBER, "Movement speed (magnitude)")
    )
    .description("Gets the bot's current velocity and speed")
    .icon("activity")
    .color("#9C27B0")
    .addKeywords("velocity", "speed", "movement", "motion")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var bot = requireBot(inputs);
    var player = bot.minecraft().player;

    if (player == null) {
      return completed(results("velocity", Vec3.ZERO, "speed", 0.0));
    }

    var velocity = player.getDeltaMovement();
    var speed = velocity.length();

    return completed(results(
      "velocity", velocity,
      "speed", speed
    ));
  }
}
