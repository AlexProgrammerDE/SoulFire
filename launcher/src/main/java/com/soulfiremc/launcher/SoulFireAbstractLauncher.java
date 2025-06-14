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

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.jar.JarFile;

public abstract class SoulFireAbstractLauncher {
  private static final String JAR_NAME = "minecraft-client-1.21.5.jar";
  private static final String JAR_URL = "https://piston-data.mojang.com/v1/objects/b88808bbb3da8d9f453694b5d8f74a3396f1a533/client.jar";

  @SneakyThrows
  private static void loadAndInjectMinecraftJar() {
    var librariesPath = Path.of("libraries");
    if (!Files.exists(librariesPath)) {
      Files.createDirectories(librariesPath);
    }

    var minecraftJarPath = librariesPath.resolve(JAR_NAME);
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

  protected abstract String getBootstrapClassName();

  @SneakyThrows
  private static void setupManagedMods() {
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

  @SneakyThrows
  private static void loadLibs() {
    var librariesPath = Path.of("libraries");
    var extractedLibs = createLibClassLoader(librariesPath);

    var reflectLibPath = Arrays.stream(extractedLibs).filter(path -> path.getFileName().toString().startsWith("Reflect-"))
      .findFirst()
      .orElseThrow(() -> new RuntimeException("Reflect library not found in extracted libs"));
    try (var reflectLib = new URLClassLoader(new URL[]{reflectLibPath.toUri().toURL()})) {
      var instrumentationClass = (Instrumentation) reflectLib.loadClass("net.lenni0451.reflect.Agents")
        .getDeclaredMethod("getInstrumentation")
        .invoke(null);

      for (var lib : extractedLibs) {
        instrumentationClass.appendToSystemClassLoaderSearch(new JarFile(lib.toFile()));
      }
    }
  }

  private static Path[] createLibClassLoader(Path libDir) {
    try (var dependencyListInput = ClassLoader.getSystemClassLoader().getResourceAsStream("META-INF/dependency-list.txt")) {
      if (dependencyListInput == null) {
        return new Path[0];
      }

      Files.createDirectories(libDir);

      var urls = new ArrayList<Path>();
      for (var fileName : new String(dependencyListInput.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
        var libFile = libDir.resolve(fileName);
        try (var libInput = Objects.requireNonNull(
          ClassLoader.getSystemClassLoader().getResourceAsStream(
            "META-INF/lib/" + fileName
          ),
          "File not found: " + fileName
        )) {
          Files.write(libFile, libInput.readAllBytes());
          urls.add(libFile);
        }
      }

      return urls.toArray(new Path[0]);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void run(String[] args) {
    System.setProperty("joml.nounsafe", "true");
    System.setProperty(SystemProperties.DEBUG_DISABLE_CLASS_PATH_ISOLATION, "true");
    System.setProperty(SystemProperties.DEBUG_DEOBFUSCATE_WITH_CLASSPATH, "true");
    System.setProperty("sf.boostrap.class", getBootstrapClassName());

    loadLibs();
    loadAndInjectMinecraftJar();
    setupManagedMods();

    KnotClient.main(args);
  }
}
