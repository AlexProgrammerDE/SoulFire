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
import net.minecraft.world.level.LightLayer;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Data node that gets the light level at the bot's position.
/// Outputs: blockLight, skyLight, combinedLight
public final class GetLightLevelNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("data.get_light_level")
    .displayName("Get Light Level")
    .category(CategoryRegistry.DATA)
    .addInputs(
      PortDefinition.input("bot", "Bot", PortType.BOT, "The bot to get light level from")
    )
    .addOutputs(
      PortDefinition.output("blockLight", "Block Light", PortType.NUMBER, "Light from blocks (0-15)"),
      PortDefinition.output("skyLight", "Sky Light", PortType.NUMBER, "Light from sky (0-15)"),
      PortDefinition.output("combinedLight", "Combined", PortType.NUMBER, "Maximum of block and sky light")
    )
    .description("Gets the light level at the bot's current position")
    .icon("sun")
    .color("#9C27B0")
    .addKeywords("light", "brightness", "dark", "spawn", "mob")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var bot = requireBot(inputs);
    var level = bot.minecraft().level;
    var player = bot.minecraft().player;

    if (level == null || player == null) {
      return completed(results("blockLight", 0, "skyLight", 0, "combinedLight", 0));
    }

    var pos = player.blockPosition();
    var blockLight = level.getBrightness(LightLayer.BLOCK, pos);
    var skyLight = level.getBrightness(LightLayer.SKY, pos);
    var combinedLight = Math.max(blockLight, skyLight);

    return completed(results(
      "blockLight", blockLight,
      "skyLight", skyLight,
      "combinedLight", combinedLight
    ));
  }
}
