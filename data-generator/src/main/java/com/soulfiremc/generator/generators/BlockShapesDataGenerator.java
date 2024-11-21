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
package com.soulfiremc.generator.generators;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class BlockShapesDataGenerator {
  private static final BlockShapesCache BLOCK_SHAPES_CACHE = new BlockShapesCache();

  private static boolean isAllTheSame(IntList list) {
    if (list.isEmpty()) {
      return true;
    }

    var first = list.getInt(0);
    for (var i = 1; i < list.size(); i++) {
      if (list.getInt(i) != first) {
        return false;
      }
    }

    return true;
  }

  private static String formatDouble(double d) {
    return "" + d;
  }

  private static String voxelShapeToString(VoxelShape voxelShape) {
    var list = new ArrayList<String>();
    voxelShape.forAllBoxes(
      (x1, y1, z1, x2, y2, z2) ->
        list.add(
          String.join(
            ",",
            formatDouble(x1),
            formatDouble(y1),
            formatDouble(z1),
            formatDouble(x2),
            formatDouble(y2),
            formatDouble(z2))));

    return String.join("|", String.join(";", list), Arrays.stream(Direction.values())
      .filter(direction -> Block.isFaceFull(voxelShape, direction))
      .map(Direction::name)
      .collect(Collectors.joining(",")));
  }

  private static class BlockShapesCache {
    public final Object2IntMap<VoxelShape> uniqueBlockShapes =
      new Object2IntLinkedOpenCustomHashMap<>(
        new Hash.Strategy<>() {
          @Override
          public int hashCode(VoxelShape voxelShape) {
            return voxelShapeToString(voxelShape).hashCode();
          }

          @Override
          public boolean equals(VoxelShape voxelShape, VoxelShape k1) {
            if (voxelShape == k1) {
              return true;
            } else if (voxelShape == null || k1 == null) {
              return false;
            }

            return voxelShapeToString(voxelShape).equals(voxelShapeToString(k1));
          }
        });
    public final Map<Block, ShapesData> blockShapes = new LinkedHashMap<>();
    private int lastCollisionShapeId = 0;

    {
      BuiltInRegistries.BLOCK.forEach(
        block -> {
          IntList blockCollisionShapes = new IntArrayList();
          IntList blockSupportShapes = new IntArrayList();

          for (var blockState : block.getStateDefinition().getPossibleStates()) {
            var collisionShape =
              blockState.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            var supportShape =
              blockState.getBlockSupportShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);

            // Revert block offset
            var blockShapeCenter = blockState.getOffset(BlockPos.ZERO);
            var inverseBlockShapeCenter = blockShapeCenter.reverse();
            collisionShape =
              collisionShape.move(
                inverseBlockShapeCenter.x,
                inverseBlockShapeCenter.y,
                inverseBlockShapeCenter.z);
            supportShape =
              supportShape.move(
                inverseBlockShapeCenter.x,
                inverseBlockShapeCenter.y,
                inverseBlockShapeCenter.z);

            blockCollisionShapes.add(
              uniqueBlockShapes.computeIfAbsent(collisionShape, k -> lastCollisionShapeId++));
            blockSupportShapes.add(
              uniqueBlockShapes.computeIfAbsent(supportShape, k -> lastCollisionShapeId++));
          }

          this.blockShapes.put(block, new ShapesData(
            blockCollisionShapes,
            blockSupportShapes
          ));
        });
    }

    public String dumpBlockShapeIndices() {
      var resultBuilder = new StringBuilder();

      for (var entry : blockShapes.entrySet()) {
        resultBuilder.append(BuiltInRegistries.BLOCK.getKey(entry.getKey()));
        var blockShapes = entry.getValue();
        insertShape(resultBuilder, blockShapes.collisionShapes);
        insertShape(resultBuilder, blockShapes.supportShapes);

        resultBuilder.append("\n");
      }

      return resultBuilder.toString();
    }

    private void insertShape(StringBuilder resultBuilder, IntList supportShapes) {
      if (!supportShapes.isEmpty()) {
        resultBuilder.append("|");

        if (isAllTheSame(supportShapes)) {
          resultBuilder.append(supportShapes.getInt(0));
        } else {
          resultBuilder.append(String.join(",",
            supportShapes.intStream().mapToObj(String::valueOf).toArray(String[]::new)));
        }
      }
    }

    public String dumpShapesObject() {
      var resultBuilder = new StringBuilder();

      for (var entry : uniqueBlockShapes.object2IntEntrySet()) {
        resultBuilder.append(entry.getIntValue());
        var voxelShapeString = voxelShapeToString(entry.getKey());
        if (!voxelShapeString.isEmpty()) {
          resultBuilder.append("|").append(voxelShapeString);
        }
        resultBuilder.append("\n");
      }

      return resultBuilder.toString();
    }
  }

  public static final class BlockShapesGenerator implements IDataGenerator {
    @Override
    public String getDataName() {
      return "data/block-shapes.txt";
    }

    @Override
    public String generateDataJson() {
      return BLOCK_SHAPES_CACHE.dumpShapesObject();
    }
  }

  public static final class BlockStatesGenerator implements IDataGenerator {
    @Override
    public String getDataName() {
      return "data/block-states.txt";
    }

    @Override
    public String generateDataJson() {
      return BLOCK_SHAPES_CACHE.dumpBlockShapeIndices();
    }
  }

  private record ShapesData(IntList collisionShapes, IntList supportShapes) {
  }
}
