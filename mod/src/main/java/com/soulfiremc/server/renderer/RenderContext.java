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

import net.minecraft.client.multiplayer.ClientLevel;

/// Pre-computed render context containing all data needed for ray casting.
/// Groups related parameters together to avoid passing many individual arguments.
///
/// @param level          The client level to render
/// @param camera         Pre-computed camera with position and direction vectors
/// @param sceneData      Pre-collected scene data (entities and map frames)
/// @param maxDistance    Maximum render distance in blocks
/// @param minY           World minimum Y coordinate
/// @param maxY           World maximum Y coordinate
/// @param invMaxDistance Pre-computed 1.0 / maxDistance for fog calculations
public record RenderContext(
  ClientLevel level,
  Camera camera,
  SceneData sceneData,
  int maxDistance,
  int minY,
  int maxY,
  double invMaxDistance
) {

  /// Creates a render context with computed inverse max distance.
  public static RenderContext create(
    ClientLevel level,
    Camera camera,
    SceneData sceneData,
    int maxDistance) {

    return new RenderContext(
      level,
      camera,
      sceneData,
      maxDistance,
      level.getMinY(),
      level.getMaxY(),
      1.0 / maxDistance
    );
  }
}
