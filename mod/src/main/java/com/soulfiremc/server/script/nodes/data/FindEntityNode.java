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

import com.soulfiremc.server.script.AbstractScriptNode;
import com.soulfiremc.server.script.ScriptContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Data node that finds the nearest entity of a specific type.
/// Input: entityType (string, e.g., "minecraft:zombie", or "any" for any entity)
/// Input: maxDistance (double, maximum search radius)
/// Outputs: found (boolean), x, y, z (position), entityId (int), distance (double)
public final class FindEntityNode extends AbstractScriptNode {
  public static final String TYPE = "data.find_entity";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public Map<String, Object> getDefaultInputs() {
    return Map.of("entityType", "any", "maxDistance", 32.0);
  }

  @Override
  public CompletableFuture<Map<String, Object>> execute(ScriptContext context, Map<String, Object> inputs) {
    var bot = context.requireBot();
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
      if (entity == player) continue; // Skip self

      var distSq = entity.distanceToSqr(playerPos);
      if (distSq > maxDistSq || distSq >= nearestDistSq) continue;

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
      "x", nearestEntity.getX(),
      "y", nearestEntity.getY(),
      "z", nearestEntity.getZ(),
      "entityId", nearestEntity.getId(),
      "distance", Math.sqrt(nearestDistSq)
    ));
  }

  private Map<String, Object> notFoundResult() {
    return results(
      "found", false,
      "x", 0.0,
      "y", 0.0,
      "z", 0.0,
      "entityId", -1,
      "distance", -1.0
    );
  }
}
