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
import net.lenni0451.classtransform.TransformerManager;
import net.lenni0451.classtransform.mixinstranslator.MixinsTranslator;
import net.lenni0451.reflect.Agents;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarFile;

public abstract class SoulFireAbstractLauncher {
  private static final String JAR_NAME = "minecraft-client-1.21.5.jar";
  private static final String JAR_URL = "https://piston-data.mojang.com/v1/objects/b88808bbb3da8d9f453694b5d8f74a3396f1a533/client.jar";

  @SneakyThrows
  private static void loadAndInjectMinecraftJar(Path baseDir) {
    var mcJarsPath = baseDir.resolve("mc-jars");
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

  protected abstract String getBootstrapClassName();

  @SneakyThrows
  private static void loadLibs(Path baseDir) {
    var librariesPath = baseDir.resolve("libraries");
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

  private static void injectEarlyMixins() {
    var classProvider = new CustomClassProvider(List.of(SoulFireAbstractLauncher.class.getClassLoader()));
    var transformerManager = new TransformerManager(classProvider);
    transformerManager.addTransformerPreprocessor(new MixinsTranslator());
    transformerManager.addTransformer("com.soulfiremc.launcher.mixin.*");

    try {
      transformerManager.hookInstrumentation(Agents.getInstrumentation());
      System.out.println("Used Runtime Agent to inject mixins");
    } catch (IOException t) {
      throw new IllegalStateException("Failed to inject mixins", t);
    }
  }

  public void run(Path basePath, String[] args) {
    System.setProperty("sf.baseDir", basePath.toAbsolutePath().toString());
    System.setProperty("java.awt.headless", "true");
    System.setProperty("joml.nounsafe", "true");
    System.setProperty(SystemProperties.SKIP_MC_PROVIDER, "true");
    System.setProperty("sf.bootstrap.class", getBootstrapClassName());

    loadLibs(basePath);
    injectEarlyMixins();
    loadAndInjectMinecraftJar(basePath);

    KnotClient.main(args);
  }
}
