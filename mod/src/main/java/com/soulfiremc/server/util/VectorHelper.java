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
package com.soulfiremc.server.util;

import com.soulfiremc.server.pathfinding.SFVec3i;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector2d;

public final class VectorHelper {
  private VectorHelper() {
  }

  public static Vector2d toVector2dXZ(Vec3 vector) {
    return new Vector2d(vector.x, vector.z);
  }

  public static Vec3 topMiddleOfBlock(SFVec3i vector, BlockState blockState) {
    return topMiddleOfBlock(vector, SFBlockHelpers.RAW_COLLISION_SHAPES.get(blockState));
  }

  public static Vec3 topMiddleOfBlock(SFVec3i vector, VoxelShape blockShapeGroup) {
    return new Vec3(
      vector.x + 0.5,
      vector.y + Math.max(blockShapeGroup.max(Direction.Axis.Y), 0),
      vector.z + 0.5
    );
  }
}
