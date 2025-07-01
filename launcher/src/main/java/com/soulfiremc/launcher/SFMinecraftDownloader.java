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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class SFMinecraftDownloader {
  private static final String MINECRAFT_VERSION = "1.21.7";
  private static final String MINECRAFT_CLIENT_JAR_NAME = "minecraft-%s-client.jar".formatted(MINECRAFT_VERSION);
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
    if (Files.exists(getMinecraftClientJarPath(basePath))) {
      System.out.println("Minecraft already downloaded, continuing");
    } else {
      System.out.println("Downloading Minecraft jar...");
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
      var clientUrl = versionInfo
        .getAsJsonObject("downloads")
        .getAsJsonObject("client")
        .get("url")
        .getAsString();

      var tempJarPath = Files.createTempFile("sf-mc-jar-download-", "-" + MINECRAFT_CLIENT_JAR_NAME);
      try (var in = URI.create(clientUrl).toURL().openStream()) {
        Files.copy(in, tempJarPath, StandardCopyOption.REPLACE_EXISTING);
      } catch (Exception e) {
        Files.deleteIfExists(tempJarPath);
        throw new RuntimeException("Failed to download Minecraft jar from " + clientUrl, e);
      }

      Files.copy(tempJarPath, minecraftJarPath);
      Files.deleteIfExists(tempJarPath);
      System.out.println("Minecraft jar downloaded and saved to: " + minecraftJarPath);
    }

    System.setProperty(SystemProperties.GAME_JAR_PATH_CLIENT, minecraftJarPath.toString());
  }
}
