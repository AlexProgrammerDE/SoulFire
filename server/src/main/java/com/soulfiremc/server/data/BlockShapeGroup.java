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
package com.soulfiremc.server.data;

import com.soulfiremc.server.util.SFHelpers;
import com.soulfiremc.server.util.mcstructs.AABB;
import com.soulfiremc.server.util.mcstructs.Direction;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public record BlockShapeGroup(int id, List<AABB> blockShapes, List<Direction> fullFaceDirections) implements IDValue {
  public static final Int2ObjectMap<BlockShapeGroup> FROM_ID = new Int2ObjectOpenHashMap<>();
  public static final BlockShapeGroup EMPTY;

  static {
    SFHelpers.getResourceAsString("minecraft/block-shapes.txt")
      .lines()
      .forEach(
        line -> {
          var parts = line.split("\\|", -1);

          var id = Integer.parseInt(parts[0]);
          var blockShapes = new ArrayList<AABB>();
          var fullFaceDirections = new ArrayList<Direction>();

          var shapeString = parts[1];
          if (!shapeString.isEmpty()) {
            var shapeParts = shapeString.split(";");
            for (var part : shapeParts) {
              var subParts = part.split(",");
              var shape =
                new AABB(
                  Double.parseDouble(subParts[0]),
                  Double.parseDouble(subParts[1]),
                  Double.parseDouble(subParts[2]),
                  Double.parseDouble(subParts[3]),
                  Double.parseDouble(subParts[4]),
                  Double.parseDouble(subParts[5]));
              blockShapes.add(shape);
            }
          }

          var faceString = parts[2];
          if (!faceString.isEmpty()) {
            var fullFaces = faceString.split(",");
            for (var face : fullFaces) {
              fullFaceDirections.add(Direction.valueOf(face));
            }
          }

          FROM_ID.put(
            id,
            new BlockShapeGroup(
              id,
              blockShapes,
              fullFaceDirections));
        });

    EMPTY = getById(0);
  }

  public static BlockShapeGroup getById(int id) {
    return FROM_ID.get(id);
  }

  public List<AABB> getCollisionBoxes(Vector3i block, BlockType blockType) {
    var collisionBoxes = new ArrayList<AABB>(blockShapes.size());
    for (var shape : blockShapes) {
      var shapeBB =
        new AABB(
          shape.minX, shape.minY, shape.minZ, shape.maxX, shape.maxY, shape.maxZ);

      // Apply random offset if needed
      shapeBB = shapeBB.move(OffsetHelper.getOffsetForBlock(blockType, block));

      // Apply block offset
      shapeBB = shapeBB.move(block.getX(), block.getY(), block.getZ());

      collisionBoxes.add(shapeBB);
    }

    return collisionBoxes;
  }

  public boolean isFullBlock() {
    if (blockShapes.size() != 1) {
      return false;
    }

    return blockShapes.getFirst().fullBlock();
  }

  public boolean hasCollisions() {
    return !blockShapes.isEmpty();
  }

  public boolean hasNoCollisions() {
    return blockShapes.isEmpty();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof BlockShapeGroup blockShapeGroup)) {
      return false;
    }
    return id == blockShapeGroup.id;
  }

  @Override
  public int hashCode() {
    return id;
  }
}
