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

import com.google.gson.JsonObject;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import com.soulfiremc.generator.util.GsonInstance;
import com.soulfiremc.generator.util.MCHelper;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Objects;
import java.util.zip.GZIPOutputStream;

@Slf4j
public class WorldExporterGenerator implements IDataGenerator {
  private static final int CHUNK_X_MIN = -4;
  private static final int CHUNK_X_MAX = 4;
  private static final int CHUNK_Z_MIN = -4;
  private static final int CHUNK_Z_MAX = 4;
  private static final int CHUNK_SIZE = 16;

  @Override
  public String getDataName() {
    return "jmh/world_data.json.zip";
  }

  @Override
  public byte[] generateDataJson() {
    var byteOutputStream = new ByteArrayOutputStream();
    try (var gzipOutputStream = new GZIPOutputStream(byteOutputStream);
         var outputStreamWriter = new OutputStreamWriter(gzipOutputStream);
         var jsonWriter = new JsonWriter(outputStreamWriter)) {
      var level = Objects.requireNonNull(MCHelper.getServer().getLevel(Level.OVERWORLD));
      var jsonObject = new JsonObject();
      var minBuildHeight = level.getMinY();
      var definitionArray = new String[BuiltInRegistries.BLOCK.size()];
      for (var blockState : BuiltInRegistries.BLOCK) {
        definitionArray[BuiltInRegistries.BLOCK.getId(blockState)] =
          BuiltInRegistries.BLOCK.getKey(blockState).toString();
      }
      jsonObject.add("definitions", GsonInstance.GSON.toJsonTree(definitionArray));

      var data =
        new int[CHUNK_SIZE * (CHUNK_X_MAX - CHUNK_X_MIN)][level.getHeight()][CHUNK_SIZE * (CHUNK_Z_MAX - CHUNK_Z_MIN)];
      for (var x = CHUNK_X_MIN * CHUNK_SIZE; x < CHUNK_X_MAX * CHUNK_SIZE; x++) {
        for (var y = 0; y < level.getHeight(); y++) {
          for (var z = CHUNK_Z_MIN * CHUNK_SIZE; z < CHUNK_Z_MAX * CHUNK_SIZE; z++) {
            var pos = new BlockPos(x, y + minBuildHeight, z);
            var blockState = level.getBlockState(pos);

            int block;
            if (!blockState.isCollisionShapeFullBlock(level, pos)) {
              block = BuiltInRegistries.BLOCK.getId(Blocks.AIR);
            } else {
              block = BuiltInRegistries.BLOCK.getId(blockState.getBlock());
            }

            data[x - CHUNK_X_MIN * CHUNK_SIZE][y][z - CHUNK_Z_MIN * CHUNK_SIZE] = block;
          }
        }
      }

      jsonObject.add("data", GsonInstance.GSON.toJsonTree(data));

      Streams.write(jsonObject, jsonWriter);

      jsonWriter.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return byteOutputStream.toByteArray();
  }
}
