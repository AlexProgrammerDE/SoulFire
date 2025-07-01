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

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class SoulFireAbstractLauncher {
  @SneakyThrows
  private static void loadLibs(Path basePath) {
    var librariesPath = basePath.resolve("libraries");
    var extractedLibs = createLibClassLoader(librariesPath);
    if (extractedLibs.length == 0) {
      System.out.println("No libraries found in META-INF/dependency-list.txt, skipping library loading.");
      return;
    }

    var reflectLibPath = Arrays.stream(extractedLibs).filter(path -> path.getFileName().toString().startsWith("Reflect-"))
      .findFirst()
      .orElseThrow(() -> new RuntimeException("Reflect library not found in extracted libs"));
    try (var reflectLib = new URLClassLoader(new URL[]{reflectLibPath.toUri().toURL()})) {
      var instrumentationClass = (Instrumentation) reflectLib.loadClass("net.lenni0451.reflect.Agents")
        .getDeclaredMethod("getInstrumentation")
        .invoke(null);

      for (var lib : extractedLibs) {
        System.setProperty("java.class.path",
          Stream.concat(Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator)),
              Stream.of(lib.toAbsolutePath().toString()))
            .collect(Collectors.joining(File.pathSeparator)));

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
      var bytes = dependencyListInput.readAllBytes();
      for (var fileName : new String(bytes, StandardCharsets.UTF_8).lines().toList()) {
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

  @SneakyThrows
  public static void run(Path basePath, String bootstrapClassName, String[] args) {
    loadLibs(basePath);
    Class.forName(SoulFirePostLibLauncher.class.getName())
      .getMethod("runPostLib", Path.class, String.class, String[].class)
      .invoke(null, basePath, bootstrapClassName, args);
  }
}
