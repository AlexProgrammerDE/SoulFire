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

import com.soulfiremc.server.bot.ControllingTask;
import com.soulfiremc.server.script.*;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Action node that makes the bot look at a specific position.
/// Inputs: x, y, z (coordinates to look at)
public final class LookAtNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("action.look_at")
    .displayName("Look At")
    .category(NodeCategory.ACTIONS)
    .addInputs(
      PortDefinition.execIn(),
      PortDefinition.input("bot", "Bot", PortType.BOT, "The bot to control"),
      PortDefinition.inputWithDefault("x", "X", PortType.NUMBER, "0", "X coordinate to look at"),
      PortDefinition.inputWithDefault("y", "Y", PortType.NUMBER, "0", "Y coordinate to look at"),
      PortDefinition.inputWithDefault("z", "Z", PortType.NUMBER, "0", "Z coordinate to look at")
    )
    .addOutputs(
      PortDefinition.execOut()
    )
    .description("Makes the bot look at a specific position in the world")
    .icon("eye")
    .color("#FF9800")
    .addKeywords("look", "face", "turn", "aim", "view")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var bot = requireBot(inputs);
    var x = getDoubleInput(inputs, "x", 0.0);
    var y = getDoubleInput(inputs, "y", 0.0);
    var z = getDoubleInput(inputs, "z", 0.0);

    bot.botControl().registerControllingTask(ControllingTask.singleTick(() -> {
      var player = bot.minecraft().player;
      if (player != null) {
        player.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(x, y, z));
      }
    }));

    return completedEmpty();
  }
}
