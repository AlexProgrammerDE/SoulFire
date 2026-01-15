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

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

/// Pre-collected scene data for rendering.
/// Contains cached information about entities and map frames to avoid
/// repeated method calls during ray casting.
public record SceneData(MapFrameData[] mapFrames, EntityData[] entities) {
  /// Pre-computed data for an item frame containing a map.
  public record MapFrameData(
    AABB bbox,
    Direction direction,
    double posX,
    double posY,
    double posZ,
    int rotation,
    byte[] colors
  ) {}

  /// Pre-computed data for a renderable entity.
  public record EntityData(AABB bbox) {}
}
