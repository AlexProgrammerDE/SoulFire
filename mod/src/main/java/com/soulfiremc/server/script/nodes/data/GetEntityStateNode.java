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
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/// Data node that queries the state of an entity by its integer ID or UUID.
/// Returns position, health, alive status, player info, and other combat-relevant state.
/// Provide either entityId or entityUuid (UUID takes priority if both are set).
public final class GetEntityStateNode extends AbstractScriptNode {
  public static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("data.get_entity_state")
    .displayName("Get Entity State")
    .category(CategoryRegistry.DATA)
    .addInputs(
      PortDefinition.execIn(),
      PortDefinition.botIn(),
      PortDefinition.inputWithDefault("entityId", "Entity ID", PortType.NUMBER, "-1", "The entity's integer ID"),
      PortDefinition.inputWithDefault("entityUuid", "Entity UUID", PortType.STRING, "\"\"", "The entity's UUID (takes priority over ID)")
    )
    .addOutputs(
      PortDefinition.execOut(),
      PortDefinition.output("found", "Found", PortType.BOOLEAN, "Whether the entity was found"),
      PortDefinition.output("position", "Position", PortType.VECTOR3, "Entity position"),
      PortDefinition.output("health", "Health", PortType.NUMBER, "Current health"),
      PortDefinition.output("maxHealth", "Max Health", PortType.NUMBER, "Maximum health"),
      PortDefinition.output("isAlive", "Is Alive", PortType.BOOLEAN, "Whether the entity is alive"),
      PortDefinition.output("isPlayer", "Is Player", PortType.BOOLEAN, "Whether the entity is a player"),
      PortDefinition.output("name", "Name", PortType.STRING, "Entity name or player username"),
      PortDefinition.output("gamemode", "Gamemode", PortType.STRING, "Player gamemode (survival, creative, etc.)"),
      PortDefinition.output("isSprinting", "Is Sprinting", PortType.BOOLEAN, "Whether the entity is sprinting"),
      PortDefinition.output("yaw", "Yaw", PortType.NUMBER, "Entity yaw rotation (horizontal)"),
      PortDefinition.output("pitch", "Pitch", PortType.NUMBER, "Entity pitch rotation (vertical)"),
      PortDefinition.output("motion", "Motion", PortType.VECTOR3, "Entity motion/velocity vector")
    )
    .description("Gets the state of an entity by integer ID or UUID (position, health, gamemode, etc.)")
    .icon("heart-pulse")
    .color("#9C27B0")
    .addKeywords("entity", "state", "health", "alive", "player", "gamemode", "sprint", "position", "uuid", "rotation", "motion")
    .build();

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var bot = requireBot(inputs);
    var entityId = getIntInput(inputs, "entityId", -1);
    var entityUuidStr = getStringInput(inputs, "entityUuid", "");

    var level = bot.minecraft().level;
    var player = bot.minecraft().player;

    if (level == null || player == null) {
      return completedMono(notFoundResult());
    }

    // Parse UUID if provided
    UUID entityUuid = null;
    if (!entityUuidStr.isEmpty()) {
      try {
        entityUuid = UUID.fromString(entityUuidStr);
      } catch (IllegalArgumentException _) {
        return completedMono(notFoundResult());
      }
    }

    if (entityUuid == null && entityId < 0) {
      return completedMono(notFoundResult());
    }

    // Find entity by UUID or integer ID
    Entity found = null;
    for (var entity : level.entitiesForRendering()) {
      if (entityUuid != null) {
        if (entity.getUUID().equals(entityUuid)) {
          found = entity;
          break;
        }
      } else if (entity.getId() == entityId) {
        found = entity;
        break;
      }
    }

    if (found == null) {
      return completedMono(notFoundResult());
    }

    return completedMono(buildResult(found, player));
  }

  private Map<String, NodeValue> buildResult(Entity entity, LocalPlayer player) {
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

    return results(
      "found", true,
      "position", entity.position(),
      "health", health,
      "maxHealth", maxHealth,
      "isAlive", isAlive,
      "isPlayer", isPlayer,
      "name", name,
      "gamemode", gamemode,
      "isSprinting", isSprinting,
      "yaw", (double) entity.getYRot(),
      "pitch", (double) entity.getXRot(),
      "motion", entity.getDeltaMovement()
    );
  }

  private Map<String, NodeValue> notFoundResult() {
    return results(
      "found", false,
      "position", Vec3.ZERO,
      "health", 0.0,
      "maxHealth", 0.0,
      "isAlive", false,
      "isPlayer", false,
      "name", "",
      "gamemode", "unknown",
      "isSprinting", false,
      "yaw", 0.0,
      "pitch", 0.0,
      "motion", Vec3.ZERO
    );
  }
}
