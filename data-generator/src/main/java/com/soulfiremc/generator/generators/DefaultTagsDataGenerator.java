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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import com.soulfiremc.generator.util.MCHelper;
import it.unimi.dsi.fastutil.ints.IntList;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagNetworkSerialization;

@Slf4j
public class DefaultTagsDataGenerator implements IDataGenerator {
  @Override
  public String getDataName() {
    return "default_tags.json.zip";
  }

  @SuppressWarnings("unchecked")
  @SneakyThrows
  @Override
  public byte[] generateDataJson() {
    var byteOutputStream = new ByteArrayOutputStream();
    try (var gzipOutputStream = new GZIPOutputStream(byteOutputStream);
         var outputStreamWriter = new OutputStreamWriter(gzipOutputStream);
         var jsonWriter = new JsonWriter(outputStreamWriter)) {

      var rootObj = new JsonObject();
      var serialized =
        TagNetworkSerialization.serializeTagsToNetwork(MCHelper.getServer().registries());

      for (var entry : serialized.entrySet()) {
        var registry = entry.getKey();
        var payload = entry.getValue();

        var registryObj = new JsonObject();

        var tagsField = TagNetworkSerialization.NetworkPayload.class.getDeclaredField("tags");
        tagsField.setAccessible(true);
        var tags = (Map<ResourceLocation, IntList>) tagsField.get(payload);
        for (var tag : tags.entrySet()) {
          var tagObj = new JsonArray();

          for (var id : tag.getValue()) {
            tagObj.add(id);
          }

          registryObj.add(tag.getKey().toString(), tagObj);
        }

        rootObj.add(registry.location().toString(), registryObj);
      }

      Streams.write(rootObj, jsonWriter);

      jsonWriter.flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return byteOutputStream.toByteArray();
  }
}
