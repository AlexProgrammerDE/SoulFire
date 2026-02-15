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
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import reactor.core.publisher.Mono;

import java.util.Map;

/// Data node that queries the state of an entity by its ID.
/// Returns health, alive status, player info, and other combat-relevant state.
/// Input: entityId (number)
/// Outputs: found, health, maxHealth, isAlive, isPlayer, name, gamemode, isSprinting
public final class GetEntityStateNode extends AbstractScriptNode {
  public static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("data.get_entity_state")
    .displayName("Get Entity State")
    .category(CategoryRegistry.DATA)
    .addInputs(
      PortDefinition.execIn(),
      PortDefinition.input("entityId", "Entity ID", PortType.NUMBER, "The entity's ID")
    )
    .addOutputs(
      PortDefinition.execOut(),
      PortDefinition.output("found", "Found", PortType.BOOLEAN, "Whether the entity was found"),
      PortDefinition.output("health", "Health", PortType.NUMBER, "Current health"),
      PortDefinition.output("maxHealth", "Max Health", PortType.NUMBER, "Maximum health"),
      PortDefinition.output("isAlive", "Is Alive", PortType.BOOLEAN, "Whether the entity is alive"),
      PortDefinition.output("isPlayer", "Is Player", PortType.BOOLEAN, "Whether the entity is a player"),
      PortDefinition.output("name", "Name", PortType.STRING, "Entity name or player username"),
      PortDefinition.output("gamemode", "Gamemode", PortType.STRING, "Player gamemode (survival, creative, etc.)"),
      PortDefinition.output("isSprinting", "Is Sprinting", PortType.BOOLEAN, "Whether the entity is sprinting")
    )
    .description("Gets the state of an entity by ID (health, gamemode, etc.)")
    .icon("heart-pulse")
    .color("#9C27B0")
    .addKeywords("entity", "state", "health", "alive", "player", "gamemode", "sprint")
    .build();

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var bot = requireBot(inputs);
    var entityId = getIntInput(inputs, "entityId", -1);

    var level = bot.minecraft().level;
    var player = bot.minecraft().player;

    if (level == null || player == null || entityId < 0) {
      return completedMono(notFoundResult());
    }

    // Find entity by ID
    for (var entity : level.entitiesForRendering()) {
      if (entity.getId() != entityId) {
        continue;
      }

      var health = 0.0;
      var maxHealth = 0.0;
      var isAlive = entity.isAlive();
      var isPlayer = entity instanceof AbstractClientPlayer;
      var name = entity.getName().getString();
      var gamemode = "unknown";
      var isSprinting = entity.isSprinting();

      if (entity instanceof LivingEntity le) {
        health = le.getHealth();
        maxHealth = le.getMaxHealth();
      }

      if (entity instanceof AbstractClientPlayer acp) {
        if (acp.isCreative()) {
          gamemode = "creative";
        } else if (acp.isSpectator()) {
          gamemode = "spectator";
        } else {
          gamemode = "survival";
        }

        var playerInfo = player.connection.getPlayerInfo(acp.getUUID());
        if (playerInfo != null) {
          name = playerInfo.getProfile().name();
        }
      }

      return completedMono(results(
        "found", true,
        "health", health,
        "maxHealth", maxHealth,
        "isAlive", isAlive,
        "isPlayer", isPlayer,
        "name", name,
        "gamemode", gamemode,
        "isSprinting", isSprinting
      ));
    }

    return completedMono(notFoundResult());
  }

  private Map<String, NodeValue> notFoundResult() {
    return results(
      "found", false,
      "health", 0.0,
      "maxHealth", 0.0,
      "isAlive", false,
      "isPlayer", false,
      "name", "",
      "gamemode", "unknown",
      "isSprinting", false
    );
  }
}
