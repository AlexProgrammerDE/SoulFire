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
import com.soulfiremc.generator.Main;
import com.soulfiremc.generator.util.GsonInstance;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Objects;
import java.util.zip.GZIPOutputStream;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

@Slf4j
public class WorldExporterGenerator implements IDataGenerator {
  private static final int CHUNK_X = 8;
  private static final int CHUNK_Z = 8;
  private static final int CHUNK_SIZE = 16;

  @Override
  public String getDataName() {
    return "world_data.json.zip";
  }

  @Override
  public byte[] generateDataJson() {
    var byteOutputStream = new ByteArrayOutputStream();
    try (var gzipOutputStream = new GZIPOutputStream(byteOutputStream);
         var outputStreamWriter = new OutputStreamWriter(gzipOutputStream);
         var jsonWriter = new JsonWriter(outputStreamWriter)) {
      var level = Objects.requireNonNull(Main.SERVER.getLevel(Level.OVERWORLD));
      var jsonObject = new JsonObject();
      var minBuildHeight = level.getMinBuildHeight();
      var definitionArray = new String[BuiltInRegistries.BLOCK.size()];
      for (var blockState : BuiltInRegistries.BLOCK) {
        definitionArray[BuiltInRegistries.BLOCK.getId(blockState)] =
          BuiltInRegistries.BLOCK.getKey(blockState).toString();
      }
      jsonObject.add("definitions", GsonInstance.GSON.toJsonTree(definitionArray));

      var data = new int[CHUNK_X * CHUNK_SIZE][level.getHeight()][CHUNK_Z * CHUNK_SIZE];
      for (var x = 0; x < CHUNK_X * CHUNK_SIZE; x++) {
        for (var y = 0; y < level.getHeight(); y++) {
          for (var z = 0; z < CHUNK_Z * CHUNK_SIZE; z++) {
            var pos = new BlockPos(x, y + minBuildHeight, z);
            var blockState = level.getBlockState(pos);

            if (!blockState.isCollisionShapeFullBlock(level, pos)) {
              data[x][y][z] = BuiltInRegistries.BLOCK.getId(Blocks.AIR);
              continue;
            }

            data[x][y][z] = BuiltInRegistries.BLOCK.getId(blockState.getBlock());
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
