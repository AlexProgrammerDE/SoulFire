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
import net.minecraft.world.level.GameType;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Data node that gets the bot's current gamemode.
/// Outputs: gamemode (string), isSurvival, isCreative, isAdventure, isSpectator
public final class GetGamemodeNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("data.get_gamemode")
    .displayName("Get Gamemode")
    .category(CategoryRegistry.DATA)
    .addInputs(
      PortDefinition.execIn()
    )
    .addOutputs(
      PortDefinition.execOut(),
      PortDefinition.output("gamemode", "Gamemode", PortType.STRING, "Current gamemode name"),
      PortDefinition.output("isSurvival", "Is Survival", PortType.BOOLEAN, "True if in survival mode"),
      PortDefinition.output("isCreative", "Is Creative", PortType.BOOLEAN, "True if in creative mode"),
      PortDefinition.output("isAdventure", "Is Adventure", PortType.BOOLEAN, "True if in adventure mode"),
      PortDefinition.output("isSpectator", "Is Spectator", PortType.BOOLEAN, "True if in spectator mode")
    )
    .description("Gets the bot's current gamemode")
    .icon("gamepad-2")
    .color("#9C27B0")
    .addKeywords("gamemode", "survival", "creative", "adventure", "spectator")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var bot = requireBot(inputs);
    var gameMode = bot.minecraft().gameMode;

    if (gameMode == null) {
      return completed(results(
        "gamemode", "unknown",
        "isSurvival", false,
        "isCreative", false,
        "isAdventure", false,
        "isSpectator", false
      ));
    }

    var currentMode = gameMode.getPlayerMode();
    return completed(results(
      "gamemode", currentMode.getName(),
      "isSurvival", currentMode == GameType.SURVIVAL,
      "isCreative", currentMode == GameType.CREATIVE,
      "isAdventure", currentMode == GameType.ADVENTURE,
      "isSpectator", currentMode == GameType.SPECTATOR
    ));
  }
}
