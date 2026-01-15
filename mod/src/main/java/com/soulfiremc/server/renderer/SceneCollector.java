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
package com.soulfiremc.server.renderer;

import lombok.experimental.UtilityClass;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.decoration.ItemFrame;

import java.util.ArrayList;

/// Collects scene data (entities and map frames) from the level for rendering.
@UtilityClass
public class SceneCollector {

  /// Collects all renderable scene data within the specified range.
  ///
  /// @param level       The client level to collect from
  /// @param localPlayer The local player to exclude from rendering
  /// @param eyeX        Camera eye X position
  /// @param eyeY        Camera eye Y position
  /// @param eyeZ        Camera eye Z position
  /// @param maxDistance Maximum render distance in blocks
  /// @return Collected scene data containing map frames and entities
  public static SceneData collect(
    ClientLevel level,
    LocalPlayer localPlayer,
    double eyeX,
    double eyeY,
    double eyeZ,
    int maxDistance) {

    var maxDistSq = (double) maxDistance * maxDistance;
    var mapFrames = new ArrayList<SceneData.MapFrameData>();
    var entities = new ArrayList<SceneData.EntityData>();

    for (var entity : level.entitiesForRendering()) {
      var dx = entity.getX() - eyeX;
      var dy = entity.getY() - eyeY;
      var dz = entity.getZ() - eyeZ;

      if (dx * dx + dy * dy + dz * dz > maxDistSq) {
        continue;
      }

      if (entity instanceof ItemFrame frame) {
        var mapData = getMapData(level, frame);
        if (mapData != null) {
          mapFrames.add(new SceneData.MapFrameData(
            frame.getBoundingBox(),
            frame.getDirection(),
            frame.getX(),
            frame.getY(),
            frame.getZ(),
            frame.getRotation(),
            mapData
          ));
        }
      } else if (entity != localPlayer) {
        entities.add(new SceneData.EntityData(entity.getBoundingBox()));
      }
    }

    return new SceneData(
      mapFrames.toArray(new SceneData.MapFrameData[0]),
      entities.toArray(new SceneData.EntityData[0])
    );
  }

  private static byte[] getMapData(ClientLevel level, ItemFrame frame) {
    var item = frame.getItem();
    var mapId = item.get(DataComponents.MAP_ID);
    if (mapId == null) {
      return null;
    }

    var mapData = level.getMapData(mapId);
    if (mapData == null) {
      return null;
    }

    return mapData.colors;
  }
}
