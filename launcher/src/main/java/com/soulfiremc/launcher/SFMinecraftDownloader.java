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
package com.soulfiremc.launcher;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.SneakyThrows;
import net.fabricmc.loader.impl.util.SystemProperties;
import net.fabricmc.mappingio.format.proguard.ProGuardFileReader;
import net.fabricmc.mappingio.format.tiny.Tiny2FileWriter;
import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

public class SFMinecraftDownloader {
  private static final String MINECRAFT_VERSION = "1.21.7";
  private static final String MINECRAFT_CLIENT_JAR_NAME = "minecraft-%s-client.jar".formatted(MINECRAFT_VERSION);
  private static final String MINECRAFT_CLIENT_MAPPINGS_PROGUARD_NAME = "minecraft-%s-client-mappings.txt".formatted(MINECRAFT_VERSION);
  private static final String MINECRAFT_CLIENT_MAPPINGS_TINY_NAME = "minecraft-%s-client-mappings.tiny".formatted(MINECRAFT_VERSION);
  private static final String MINECRAFT_CLIENT_JAR_REMAPPED_NAME = "minecraft-%s-client-remapped.jar".formatted(MINECRAFT_VERSION);
  private static final String MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

  private static Path getAndCreateDownloadDirectory(Path basePath) {
    var downloadDir = basePath.resolve("mc-downloads");
    if (!Files.exists(downloadDir)) {
      try {
        Files.createDirectories(downloadDir);
      } catch (Exception e) {
        throw new RuntimeException("Failed to create Minecraft download directory: " + downloadDir, e);
      }
    }
    return downloadDir;
  }

  private static Path getMinecraftClientJarPath(Path basePath) {
    return getAndCreateDownloadDirectory(basePath).resolve(MINECRAFT_CLIENT_JAR_NAME);
  }

  private static Path getMinecraftClientMappingsProguardPath(Path basePath) {
    return getAndCreateDownloadDirectory(basePath).resolve(MINECRAFT_CLIENT_MAPPINGS_PROGUARD_NAME);
  }

  private static Path getMinecraftClientMappingsTinyPath(Path basePath) {
    return getAndCreateDownloadDirectory(basePath).resolve(MINECRAFT_CLIENT_MAPPINGS_TINY_NAME);
  }

  private static Path getMinecraftClientJarRemappedPath(Path basePath) {
    return getAndCreateDownloadDirectory(basePath).resolve(MINECRAFT_CLIENT_JAR_REMAPPED_NAME);
  }

  private static JsonObject getUrl(String url) {
    try (var client = HttpClient.newHttpClient()) {
      var response = client.send(HttpRequest.newBuilder(URI.create(url)).build(),
        HttpResponse.BodyHandlers.ofString());
      return JsonParser.parseString(response.body()).getAsJsonObject();
    } catch (Exception e) {
      throw new RuntimeException("Failed to fetch URL: " + url, e);
    }
  }

  @SuppressWarnings("unused")
  @SneakyThrows
  public static void loadAndInjectMinecraftJar(Path basePath) {
    var minecraftJarPath = getMinecraftClientJarPath(basePath);
    var minecraftMappingsProguardPath = getMinecraftClientMappingsProguardPath(basePath);
    if (Files.exists(minecraftJarPath)
      && Files.exists(minecraftMappingsProguardPath)) {
      System.out.println("Minecraft already downloaded, continuing");
    } else {
      System.out.println("Downloading Minecraft...");
      var versionUrl = getUrl(MANIFEST_URL)
        .getAsJsonArray("versions")
        .asList()
        .stream()
        .map(JsonElement::getAsJsonObject)
        .filter(v -> v.get("id").getAsString().equals(MINECRAFT_VERSION))
        .map(v -> v.get("url").getAsString())
        .findFirst()
        .orElseThrow(() -> new RuntimeException("Minecraft version " + MINECRAFT_VERSION + " not found in manifest"));

      var versionInfo = getUrl(versionUrl);

      if (!Files.exists(minecraftJarPath)) {
        var clientUrl = versionInfo
          .getAsJsonObject("downloads")
          .getAsJsonObject("client")
          .get("url")
          .getAsString();

        System.out.println("Downloading Minecraft client jar from: " + clientUrl);
        var tempJarPath = Files.createTempFile("sf-mc-jar-download-", "-" + MINECRAFT_CLIENT_JAR_NAME);
        try (var in = URI.create(clientUrl).toURL().openStream()) {
          Files.copy(in, tempJarPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
          Files.deleteIfExists(tempJarPath);
          throw new RuntimeException("Failed to download Minecraft client jar from " + clientUrl, e);
        }

        Files.copy(tempJarPath, minecraftJarPath);
        Files.deleteIfExists(tempJarPath);
        System.out.println("Minecraft client jar downloaded and saved to: " + minecraftJarPath);
      }

      if (!Files.exists(minecraftMappingsProguardPath)) {
        var clientMappingsUrl = versionInfo
          .getAsJsonObject("downloads")
          .getAsJsonObject("client_mappings")
          .get("url")
          .getAsString();

        System.out.println("Downloading Minecraft client mappings from: " + clientMappingsUrl);
        var tempMappingsPath = Files.createTempFile("sf-mc-mappings-download-", ".txt");
        try (var in = URI.create(clientMappingsUrl).toURL().openStream()) {
          Files.copy(in, tempMappingsPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
          Files.deleteIfExists(tempMappingsPath);
          throw new RuntimeException("Failed to download Minecraft client mappings from " + clientMappingsUrl, e);
        }

        Files.copy(tempMappingsPath, minecraftMappingsProguardPath);
        Files.deleteIfExists(tempMappingsPath);
        System.out.println("Minecraft client mappings downloaded and saved to: " + minecraftMappingsProguardPath);
      }
    }

    var minecraftMappingsTinyPath = getMinecraftClientMappingsTinyPath(basePath);
    if (!Files.exists(minecraftMappingsTinyPath)) {
      try (var reader = Files.newBufferedReader(minecraftMappingsProguardPath);
           var writer = Files.newBufferedWriter(minecraftMappingsTinyPath)) {
        ProGuardFileReader.read(reader, new Tiny2FileWriter(writer, false));
      } catch (Exception e) {
        throw new RuntimeException("Failed to read or write ProGuard mappings", e);
      }
    }

    var minecraftJarRemappedPath = getMinecraftClientJarRemappedPath(basePath);
    if (!Files.exists(minecraftJarRemappedPath)) {
      TinyRemapper remapper = TinyRemapper.newRemapper()
        .withMappings(TinyUtils.createTinyMappingProvider(minecraftMappingsTinyPath, "target", "source"))
        .rebuildSourceFilenames(true)
        .build();

      try (OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(minecraftJarRemappedPath).build()) {
        outputConsumer.addNonClassFiles(minecraftJarPath, NonClassCopyMode.FIX_META_INF, remapper);

        remapper.readInputs(minecraftJarPath);
        remapper.readClassPath(Arrays.stream(System.getProperty("java.class.path")
            .split(File.pathSeparator))
          .map(Paths::get)
          .toArray(Path[]::new));

        remapper.apply(outputConsumer);
      } catch (IOException e) {
        throw new RuntimeException(e);
      } finally {
        remapper.finish();
      }
    }

    System.setProperty(SystemProperties.GAME_JAR_PATH_CLIENT, minecraftJarRemappedPath.toString());
  }
}
