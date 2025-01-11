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
package com.soulfiremc.server.util.structs;

import lombok.Getter;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SFContextClassLoader extends ClassLoader {
  // Prevent infinite loop when plugins are looking for classes inside this class loader
  private static final ThreadLocal<Boolean> PREVENT_LOOP = ThreadLocal.withInitial(() -> false);
  @Getter
  private final List<ClassLoader> childClassLoaders = new ArrayList<>();
  private final ClassLoader platformClassLoader = ClassLoader.getSystemClassLoader().getParent();

  public SFContextClassLoader(Path libDir) {
    super(createLibClassLoader(libDir));
  }

  private static ClassLoader createLibClassLoader(Path libDir) {
    var urls = new ArrayList<URL>();
    var dependencyListInput = ClassLoader.getSystemClassLoader().getResourceAsStream("META-INF/dependency-list.txt");
    if (dependencyListInput != null) {
      try {
        Files.createDirectories(libDir);
        for (var fileName : new String(dependencyListInput.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
          var libFile = libDir.resolve(fileName);
          try (var libInput = Objects.requireNonNull(
            ClassLoader.getSystemClassLoader().getResourceAsStream(
              "META-INF/lib/" + fileName
            ),
            "File not found: " + fileName
          )) {
            Files.write(libFile, libInput.readAllBytes());
            urls.add(libFile.toUri().toURL());
          }
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    return new URLClassLoader(urls.toArray(new URL[0]), ClassLoader.getSystemClassLoader());
  }

  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    synchronized (getClassLoadingLock(name)) {
      // First, check if the class has already been loaded
      var c = findLoadedClass(name);
      if (c == null) {
        try {
          c = platformClassLoader.loadClass(name);
        } catch (ClassNotFoundException ignored) {
          // Ignore
        }

        // In the next step, we pretend we own the classes
        // of either the parent or the child classloaders
        if (c == null) {
          var parentClassData = getClassBytes(this.getParent(), name);
          if (parentClassData == null) {
            if (PREVENT_LOOP.get()) {
              // This classloader -> plugin classloader -> delegates back to this classloader -> tries to get it from the plugin classloader again
              // We don't want to loop infinitely
              throw new ClassNotFoundException(name);
            }

            PREVENT_LOOP.set(true);
            try {
              // Check if child class loaders can load the class
              for (var childClassLoader : childClassLoaders) {
                try {
                  var pluginClass = childClassLoader.loadClass(name);
                  if (pluginClass != null) {
                    return pluginClass;
                  }
                } catch (ClassNotFoundException ignored) {
                  // Ignore
                }
              }

              throw new ClassNotFoundException(name);
            } finally {
              PREVENT_LOOP.set(false);
            }
          }

          c = defineClass(name, parentClassData, 0, parentClassData.length);
        }
      }

      // Resolve the class if requested
      if (resolve) {
        resolveClass(c);
      }

      return c;
    }
  }

  private byte[] getClassBytes(ClassLoader classLoader, String className) {
    var classPath = className.replace('.', '/') + ".class";

    try (var inputStream = classLoader.getResourceAsStream(classPath)) {
      if (inputStream == null) {
        return null;
      }

      return inputStream.readAllBytes();
    } catch (IOException ignored) {
      return null;
    }
  }
}
