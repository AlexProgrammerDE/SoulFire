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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Data node that finds the nearest entity of a specific type.
/// Input: entityType (string, e.g., "minecraft:zombie", or "any" for any entity)
/// Input: maxDistance (double, maximum search radius)
/// Outputs: found (boolean), position (Vec3), entityId (int), distance (double)
public final class FindEntityNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("data.find_entity")
    .displayName("Find Entity")
    .category(CategoryRegistry.DATA)
    .addInputs(
      PortDefinition.execIn(),
      PortDefinition.inputWithDefault("entityType", "Entity Type", PortType.STRING, "\"any\"", "Entity ID or 'any' for any entity"),
      PortDefinition.inputWithDefault("maxDistance", "Max Distance", PortType.NUMBER, "32", "Maximum search radius")
    )
    .addOutputs(
      PortDefinition.execOut(),
      PortDefinition.output("found", "Found", PortType.BOOLEAN, "Whether an entity was found"),
      PortDefinition.output("position", "Position", PortType.VECTOR3, "Entity position"),
      PortDefinition.output("entityId", "Entity ID", PortType.NUMBER, "The entity's ID"),
      PortDefinition.output("distance", "Distance", PortType.NUMBER, "Distance to the entity")
    )
    .description("Finds the nearest entity of a specific type within range")
    .icon("target")
    .color("#9C27B0")
    .addKeywords("find", "entity", "search", "nearest", "mob", "player", "locate")
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
    var entityTypeInput = getStringInput(inputs, "entityType", "any");
    var maxDistance = getDoubleInput(inputs, "maxDistance", 32.0);

    if (level == null || player == null) {
      return completed(notFoundResult());
    }

    var playerPos = player.position();
    var maxDistSq = maxDistance * maxDistance;

    // Find the nearest entity matching the criteria
    Entity nearestEntity = null;
    double nearestDistSq = Double.MAX_VALUE;

    for (var entity : level.entitiesForRendering()) {
      if (entity == player) {
        continue; // Skip self

      }
      var distSq = entity.distanceToSqr(playerPos);
      if (distSq > maxDistSq || distSq >= nearestDistSq) {
        continue;
      }

      if (!"any".equalsIgnoreCase(entityTypeInput)) {
        var entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        if (!entityTypeId.equals(entityTypeInput) && !entityTypeId.equals("minecraft:" + entityTypeInput)) {
          continue;
        }
      }

      nearestEntity = entity;
      nearestDistSq = distSq;
    }

    if (nearestEntity == null) {
      return completed(notFoundResult());
    }

    return completed(results(
      "found", true,
      "position", nearestEntity.position(),
      "entityId", nearestEntity.getId(),
      "distance", Math.sqrt(nearestDistSq)
    ));
  }

  private Map<String, NodeValue> notFoundResult() {
    return results(
      "found", false,
      "position", Vec3.ZERO,
      "entityId", -1,
      "distance", -1.0
    );
  }
}
