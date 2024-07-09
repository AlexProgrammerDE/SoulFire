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

import com.soulfiremc.server.protocol.bot.movement.AABB;
import com.soulfiremc.util.ResourceHelper;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import org.cloudburstmc.math.vector.Vector3i;

@SuppressWarnings("unused")
public record BlockShapeGroup(int id, List<BlockShape> blockShapes, double highestY) {
  public static final Int2ObjectMap<BlockShapeGroup> FROM_ID = new Int2ObjectOpenHashMap<>();
  public static final BlockShapeGroup EMPTY;

  static {
    ResourceHelper.getResourceAsString("minecraft/blockshapes.txt")
      .lines()
      .forEach(
        line -> {
          var parts = line.split("\\|");

          var id = Integer.parseInt(parts[0]);
          var blockShapes = new ArrayList<BlockShape>();

          if (parts.length > 1) {
            for (var i = 1; i < parts.length; i++) {
              var part = parts[i];
              var subParts = part.split(",");
              var shape =
                new BlockShape(
                  Double.parseDouble(subParts[0]),
                  Double.parseDouble(subParts[1]),
                  Double.parseDouble(subParts[2]),
                  Double.parseDouble(subParts[3]),
                  Double.parseDouble(subParts[4]),
                  Double.parseDouble(subParts[5]));
              blockShapes.add(shape);
            }
          }

          FROM_ID.put(
            id,
            new BlockShapeGroup(
              id,
              blockShapes,
              blockShapes.stream().mapToDouble(BlockShape::maxY).max().orElse(0)));
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
          shape.minX(), shape.minY(), shape.minZ(), shape.maxX(), shape.maxY(), shape.maxZ());

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

    return blockShapes.getFirst().isFullBlock();
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
