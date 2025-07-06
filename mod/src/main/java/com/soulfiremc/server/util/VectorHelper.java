/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.util;

import com.soulfiremc.server.pathfinding.SFVec3i;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.cloudburstmc.math.vector.Vector2d;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;

public class VectorHelper {
  private VectorHelper() {
  }

  public static Vector2d toVector2dXZ(Vector3d vector) {
    return Vector2d.from(vector.getX(), vector.getZ());
  }

  public static Vector3d topMiddleOfBlock(SFVec3i vector, BlockState blockState) {
    return topMiddleOfBlock(vector, SFBlockHelpers.RAW_COLLISION_SHAPES.get(blockState));
  }

  public static Vector3d topMiddleOfBlock(SFVec3i vector, VoxelShape blockShapeGroup) {
    return Vector3d.from(
      vector.x + 0.5,
      vector.y + Math.max(blockShapeGroup.max(Direction.Axis.Y), 0),
      vector.z + 0.5
    );
  }

  public static BlockPos fromVector3i(Vector3i vector) {
    return new BlockPos(vector.getX(), vector.getY(), vector.getZ());
  }

  public static Vector3i fromBlockPos(BlockPos vector) {
    return Vector3i.from(vector.getX(), vector.getY(), vector.getZ());
  }

  public static Vec3 fromVector3d(Vector3d vector) {
    return new Vec3(vector.getX(), vector.getY(), vector.getZ());
  }

  public static Vector3d fromVec3(Vec3 vector) {
    return Vector3d.from(vector.x, vector.y, vector.z);
  }
}
