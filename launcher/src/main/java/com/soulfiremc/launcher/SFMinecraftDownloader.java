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
import com.soulfiremc.shared.LazyObject;
import lombok.SneakyThrows;
import net.fabricmc.loader.impl.game.GameProviderHelper;
import net.fabricmc.loader.impl.launch.MappingConfiguration;
import net.fabricmc.loader.impl.util.SystemProperties;
import net.fabricmc.mappingio.format.proguard.ProGuardFileReader;
import net.fabricmc.mappingio.format.tiny.Tiny2FileReader;
import net.fabricmc.mappingio.format.tiny.Tiny2FileWriter;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

import java.io.File;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class SFMinecraftDownloader {
  public static final boolean IS_OBFUSCATED_RELEASE = Boolean.parseBoolean(System.getProperty("sf.obfuscatedRelease", "false"));
  public static final String CLIENT_URL_OVERRIDE = System.getProperty("sf.clientUrlOverride", "https://piston-data.mojang.com/v1/objects/4509ee9b65f226be61142d37bf05f8d28b03417b/client.jar");
  private static final String MINECRAFT_VERSION = System.getProperty("sf.mcVersionOverride", "1.21.11_unobfuscated");
  private static final String MINECRAFT_CLIENT_JAR_NAME = "minecraft-%s-client.jar".formatted(MINECRAFT_VERSION);
  private static final String MINECRAFT_CLIENT_MAPPINGS_PROGUARD_NAME = "minecraft-%s-client-mappings.txt".formatted(MINECRAFT_VERSION);
  private static final String MINECRAFT_CLIENT_MAPPINGS_TINY_NAME = "minecraft-%s-client-mappings.tiny".formatted(MINECRAFT_VERSION);
  private static final String MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

  private SFMinecraftDownloader() {
  }

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

  private static JsonObject getUrl(String url) {
    try (var client = HttpClient.newHttpClient()) {
      var response = client.send(HttpRequest.newBuilder(URI.create(url)).build(),
        HttpResponse.BodyHandlers.ofString());
      return JsonParser.parseString(response.body()).getAsJsonObject();
    } catch (Exception e) {
      throw new RuntimeException("Failed to fetch URL: " + url, e);
    }
  }

  @SneakyThrows
  private static void setRemapClasspath(Path basePath) {
    var remapPathFile = Files.createTempFile("soulfire-mc-remap-", ".txt");
    remapPathFile.toFile().deleteOnExit();

    var getDeobfJarDir = GameProviderHelper.class.getDeclaredMethod("getDeobfJarDir", Path.class, String.class, String.class);
    getDeobfJarDir.setAccessible(true);
    var deobfJarDir = (Path) getDeobfJarDir.invoke(null, basePath.resolve("minecraft"), "minecraft", MINECRAFT_VERSION);

    Files.writeString(remapPathFile, Stream.concat(Arrays.stream(System.getProperty("java.class.path")
          .split(File.pathSeparator)),
        Stream.of(
          deobfJarDir
            .resolve("client-intermediary.jar")
            .toAbsolutePath()
            .toString()
        )
      )
      .collect(Collectors.joining(File.pathSeparator)));

    System.setProperty(SystemProperties.REMAP_CLASSPATH_FILE, remapPathFile.toString());
  }

  @SuppressWarnings("unused")
  @SneakyThrows
  public static void loadAndInjectMinecraftJar(Path basePath) {
    var minecraftJarPath = getMinecraftClientJarPath(basePath);
    var minecraftMappingsProguardPath = getMinecraftClientMappingsProguardPath(basePath);
    if (Files.exists(minecraftJarPath)
      && (!IS_OBFUSCATED_RELEASE || Files.exists(minecraftMappingsProguardPath))) {
      IO.println("Minecraft already downloaded, continuing");
    } else {
      IO.println("Downloading Minecraft...");
      var versionInfo = new LazyObject<>(() -> {
        var versionUrl = getUrl(MANIFEST_URL)
          .getAsJsonArray("versions")
          .asList()
          .stream()
          .map(JsonElement::getAsJsonObject)
          .filter(v -> MINECRAFT_VERSION.equals(v.get("id").getAsString()))
          .map(v -> v.get("url").getAsString())
          .findFirst()
          .orElseThrow(() -> new RuntimeException("Minecraft version " + MINECRAFT_VERSION + " not found in manifest"));
        return getUrl(versionUrl);
      });

      if (!Files.exists(minecraftJarPath)) {
        var clientUrl = !CLIENT_URL_OVERRIDE.isBlank() ? CLIENT_URL_OVERRIDE : versionInfo.get()
          .getAsJsonObject("downloads")
          .getAsJsonObject("client")
          .get("url")
          .getAsString();

        IO.println("Downloading Minecraft client jar from: " + clientUrl);
        var tempJarPath = Files.createTempFile("sf-mc-jar-download-", "-" + MINECRAFT_CLIENT_JAR_NAME);
        try (var in = URI.create(clientUrl).toURL().openStream()) {
          Files.copy(in, tempJarPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
          Files.deleteIfExists(tempJarPath);
          throw new RuntimeException("Failed to download Minecraft client jar from " + clientUrl, e);
        }

        Files.copy(tempJarPath, minecraftJarPath);
        Files.deleteIfExists(tempJarPath);
        IO.println("Minecraft client jar downloaded and saved to: " + minecraftJarPath);
      }

      if (IS_OBFUSCATED_RELEASE && !Files.exists(minecraftMappingsProguardPath)) {
        var clientMappingsUrl = versionInfo.get()
          .getAsJsonObject("downloads")
          .getAsJsonObject("client_mappings")
          .get("url")
          .getAsString();

        IO.println("Downloading Minecraft client mappings from: " + clientMappingsUrl);
        var tempMappingsPath = Files.createTempFile("sf-mc-mappings-download-", ".txt");
        try (var in = URI.create(clientMappingsUrl).toURL().openStream()) {
          Files.copy(in, tempMappingsPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
          Files.deleteIfExists(tempMappingsPath);
          throw new RuntimeException("Failed to download Minecraft client mappings from " + clientMappingsUrl, e);
        }

        Files.copy(tempMappingsPath, minecraftMappingsProguardPath);
        Files.deleteIfExists(tempMappingsPath);
        IO.println("Minecraft client mappings downloaded and saved to: " + minecraftMappingsProguardPath);
      }
    }

    var minecraftMappingsTinyPath = getMinecraftClientMappingsTinyPath(basePath);
    if (IS_OBFUSCATED_RELEASE && !Files.exists(minecraftMappingsTinyPath)) {
      try (var proguardReader = Files.newBufferedReader(minecraftMappingsProguardPath);
           var intermediaryReader = new InputStreamReader(
             Objects.requireNonNull(
               MappingConfiguration.class.getClassLoader().getResourceAsStream(
                 "mappings/mappings.tiny"
               )
             )
           );
           var writer = Files.newBufferedWriter(minecraftMappingsTinyPath)) {
        var mappingTree = new MemoryMappingTree();
        ProGuardFileReader.read(proguardReader, "named", "official", mappingTree);
        Tiny2FileReader.read(intermediaryReader, mappingTree);

        var tiny2Writer = new Tiny2FileWriter(writer, false);
        mappingTree.accept(tiny2Writer);
      } catch (Exception e) {
        throw new RuntimeException("Failed to read/write mappings", e);
      }
    }

    System.setProperty(SystemProperties.GAME_MAPPING_NAMESPACE, "official");
    System.setProperty(SystemProperties.GAME_JAR_PATH_CLIENT, minecraftJarPath.toString());
    if (IS_OBFUSCATED_RELEASE) {
      System.setProperty(SystemProperties.MAPPING_PATH, minecraftMappingsTinyPath.toAbsolutePath().toString());
      if (Boolean.getBoolean("sf.remapToNamed")) {
        System.setProperty(SystemProperties.RUNTIME_MAPPING_NAMESPACE, "named");
        setRemapClasspath(basePath);
      } else {
        System.setProperty(SystemProperties.RUNTIME_MAPPING_NAMESPACE, "intermediary");
      }
    } else {
      System.setProperty(SystemProperties.RUNTIME_MAPPING_NAMESPACE, "official");
    }
  }
}
