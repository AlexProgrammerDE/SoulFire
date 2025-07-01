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

import lombok.SneakyThrows;
import net.fabricmc.loader.impl.util.SystemProperties;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class SFMinecraftDownloader {
  private static final String JAR_NAME = "minecraft-client-1.21.7.jar";
  private static final String JAR_URL = "https://piston-data.mojang.com/v1/objects/a2db1ea98c37b2d00c83f6867fb8bb581a593e07/client.jar";

  @SuppressWarnings("unused")
  @SneakyThrows
  public static void loadAndInjectMinecraftJar(Path basePath) {
    var mcJarsPath = basePath.resolve("mc-jars");
    if (!Files.exists(mcJarsPath)) {
      Files.createDirectories(mcJarsPath);
    }

    var minecraftJarPath = mcJarsPath.resolve(JAR_NAME);
    if (!Files.exists(minecraftJarPath)) {
      System.out.println("Downloading Minecraft jar...");
      var tempJarPath = Files.createTempFile("sf-mc-jar-download-", "-" + JAR_NAME);
      try (var in = URI.create(JAR_URL).toURL().openStream()) {
        Files.copy(in, tempJarPath, StandardCopyOption.REPLACE_EXISTING);
      } catch (Exception e) {
        Files.deleteIfExists(tempJarPath);
        throw new RuntimeException("Failed to download Minecraft jar from " + JAR_URL, e);
      }

      Files.copy(tempJarPath, minecraftJarPath);
      Files.deleteIfExists(tempJarPath);
      System.out.println("Minecraft jar downloaded and saved to: " + minecraftJarPath);
    } else {
      System.out.println("Minecraft jar already exists, skipping download.");
    }

    System.setProperty(SystemProperties.GAME_JAR_PATH_CLIENT, minecraftJarPath.toString());
  }
}
