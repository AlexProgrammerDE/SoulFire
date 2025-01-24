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
package com.soulfiremc.generator;

import com.google.gson.JsonElement;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import com.soulfiremc.generator.generators.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

@Slf4j
public class DataGenerators {
  private static final List<IDataGenerator> GENERATORS =
    List.of(
      new RegistryKeysJavaGenerator(),
      new BlockShapesDataGenerator.BlockShapesGenerator(),
      new BlockShapesDataGenerator.BlockStatesGenerator(),
      new BlocksJsonGenerator(),
      new BlocksJavaGenerator(),
      new BlockPropertiesJavaGenerator(),
      new FluidsJsonGenerator(),
      new FluidsJavaGenerator(),
      new EffectsJsonGenerator(),
      new EffectsJavaGenerator(),
      new EntitiesJsonGenerator(),
      new EntitiesJavaGenerator(),
      new NamedEntityDataJavaGenerator(),
      new ItemsJsonGenerator(),
      new ItemsJavaGenerator(),
      new AttributesJsonGenerator(),
      new AttributesJavaGenerator(),
      new LanguageJsonGenerator(),
      new MapColorJavaGenerator(),
      new TagsDataGenerator.BlockTagsDataGenerator(),
      new TagsDataGenerator.ItemTagsDataGenerator(),
      new TagsDataGenerator.EntityTypeTagsDataGenerator(),
      new TagsDataGenerator.FluidTagsDataGenerator(),
      new DefaultTagsDataGenerator(),
      new DefaultPacksDataGenerator(),
      new WorldExporterGenerator());

  private DataGenerators() {}

  public static boolean runDataGenerators(Path outputDirectory) {
    try {
      Files.createDirectories(outputDirectory);
    } catch (IOException exception) {
      log.error(
        "Failed to create data generator output directory at {}", outputDirectory, exception);
      return false;
    }

    var generatorsFailed = 0;
    log.info("Running minecraft data generators, output at {}", outputDirectory);

    for (var dataGenerator : GENERATORS) {
      log.info("Running generator {}", dataGenerator.getDataName());
      try {
        var outputFileName = dataGenerator.getDataName();
        var outputFilePath = outputDirectory.resolve(outputFileName);
        var outputFolder = outputFilePath.getParent();
        Files.createDirectories(outputFolder);

        var outputElement = dataGenerator.generateDataJson();

        switch (outputElement) {
          case JsonElement jsonElement -> {
            try (var writer =
                   Files.newBufferedWriter(
                     outputFilePath,
                     StandardOpenOption.CREATE,
                     StandardOpenOption.TRUNCATE_EXISTING)) {
              var jsonWriter = new JsonWriter(writer);
              jsonWriter.setIndent("  ");
              Streams.write(jsonElement, jsonWriter);
            }
          }
          case String string -> {
            try (var writer =
                   Files.newBufferedWriter(
                     outputFilePath,
                     StandardOpenOption.CREATE,
                     StandardOpenOption.TRUNCATE_EXISTING)) {
              writer.write(string);
            }
          }
          case byte[] bytes -> {
            try (var outputStream =
                   Files.newOutputStream(
                     outputFilePath,
                     StandardOpenOption.CREATE,
                     StandardOpenOption.TRUNCATE_EXISTING)) {
              outputStream.write(bytes);
            }
          }
          default -> {
            log.error("Unknown output type for data generator {}", dataGenerator.getDataName());
            generatorsFailed++;
            continue;
          }
        }

        log.info("Generator: {} -> {}", dataGenerator.getClass().getSimpleName(), outputFileName);
      } catch (Throwable exception) {
        log.error("Failed to run data generator {}", dataGenerator.getDataName(), exception);
        generatorsFailed++;
      }
    }

    PacketsGenerator.generatePackets(outputDirectory.resolve("packets"));

    log.info("Finishing running data generators");
    return generatorsFailed == 0;
  }
}
