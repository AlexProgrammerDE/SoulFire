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
      vector.y + blockShapeGroup.max(Direction.Axis.Y),
      vector.z + 0.5
    );
  }

  public static BlockPos fromVector3i(Vector3i vector) {
    return new BlockPos(vector.getX(), vector.getY(), vector.getZ());
  }

  public static Vec3 fromVector3d(Vector3d vector) {
    return new Vec3(vector.getX(), vector.getY(), vector.getZ());
  }
}
