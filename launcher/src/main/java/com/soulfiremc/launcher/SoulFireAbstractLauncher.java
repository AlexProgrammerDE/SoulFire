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
import net.fabricmc.loader.impl.launch.knot.KnotClient;
import net.fabricmc.loader.impl.util.SystemProperties;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class SoulFireAbstractLauncher {
  private static final String JAR_NAME = "minecraft-client-1.21.5.jar";
  private static final String JAR_URL = "https://piston-data.mojang.com/v1/objects/b88808bbb3da8d9f453694b5d8f74a3396f1a533/client.jar";

  public void run(String[] args) {
    System.setProperty("joml.nounsafe", "true");
    System.setProperty(SystemProperties.DEBUG_DISABLE_CLASS_PATH_ISOLATION, "true");
    System.setProperty(SystemProperties.DEBUG_DEOBFUSCATE_WITH_CLASSPATH, "true");
    System.setProperty("sf.boostrap.class", getBootstrapClassName());

    loadAndInjectMinecraftJar();
    setupManagedMods();

    KnotClient.main(args);
  }

  protected abstract String getBootstrapClassName();

  @SneakyThrows
  private void loadAndInjectMinecraftJar() {
    var jarsPath = Path.of("jars");
    if (!Files.exists(jarsPath)) {
      Files.createDirectories(jarsPath);
    }

    var minecraftJarPath = jarsPath.resolve(JAR_NAME);
    if (!Files.exists(minecraftJarPath)) {
      System.out.println("Downloading Minecraft jar...");
      var tempJarPath = Files.createTempFile("sf-mc-jar-download-", "-" + JAR_NAME);
      try (var in = URI.create(JAR_URL).toURL().openStream()) {
        Files.copy(in, tempJarPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
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

  @SneakyThrows
  private void setupManagedMods() {
    var modsPath = Path.of("mods");
    if (!Files.exists(modsPath)) {
      Files.createDirectories(modsPath);
    }

    try (var stream = Files.list(modsPath)) {
      stream.filter(path -> path.getFileName().toString().startsWith("managed-"))
        .forEach(path -> {
          try {
            Files.deleteIfExists(path);
          } catch (Exception e) {
            System.err.println("Failed to delete managed mod: " + path + " - " + e.getMessage());
          }
        });
    }

    FileSystemUtil.getFilesInDirectory("META-INF/jars")
      .forEach((filePath, fileBytes) -> {
        try {
          var targetPath = modsPath.resolve("managed-" + filePath.getFileName());
          Files.write(targetPath, fileBytes);
          System.out.println("Copied managed mod: " + targetPath);
        } catch (Exception e) {
          System.err.println("Failed to copy managed mod: " + filePath + " - " + e.getMessage());
        }
      });
  }
}
