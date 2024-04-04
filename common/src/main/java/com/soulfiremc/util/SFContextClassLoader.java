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
package com.soulfiremc.util;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.Getter;

public class SFContextClassLoader extends ClassLoader {
  @Getter
  private final List<ClassLoader> childClassLoaders = new ArrayList<>();
  private final Method findLoadedClassMethod;
  private final ClassLoader platformClassLoader = ClassLoader.getSystemClassLoader().getParent();
  // Prevent infinite loop when plugins are looking for classes inside this class loader
  private boolean lookingThroughPlugins = false;

  public SFContextClassLoader() {
    super(createLibClassLoader());
    try {
      findLoadedClassMethod =
        ClassLoader.class.getDeclaredMethod("loadClass", String.class, boolean.class);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  private static URLClassLoader createLibClassLoader() {
    var urls = new ArrayList<URL>();
    try {
      var tempDir = Files.createTempDirectory("soulfire-lib-");
      Runtime.getRuntime().addShutdownHook(new Thread(() -> deleteDirRecursively(tempDir)));

      for (var entry : FileSystemUtil.getFilesInDirectory("/META-INF/lib").entrySet()) {
        var fileName = entry.getKey().getFileName().toString();

        var tempFile = tempDir.resolve(fileName);
        Files.write(tempFile, entry.getValue());
        urls.add(tempFile.toUri().toURL());
      }
    } catch (IOException | URISyntaxException e) {
      throw new RuntimeException(e);
    }
    return new URLClassLoader(urls.toArray(new URL[0]), ClassLoader.getSystemClassLoader());
  }

  private static void deleteDirRecursively(Path dir) {
    try (var stream = Files.walk(dir)) {
      stream
        .sorted(Comparator.reverseOrder())
        .forEach(
          file -> {
            try {
              Files.delete(file);
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    synchronized (getClassLoadingLock(name)) {
      // First, check if the class has already been loaded
      var c = findLoadedClass(name);
      if (c == null) {
        try {
          return loadClassFromClassLoader(platformClassLoader, name, resolve);
        } catch (ClassNotFoundException ignored) {
          // Ignore
        }

        // In the next step, we pretend we own the classes of either the parent or the child class
        // loaders
        var parentClassData = getClassBytes(this.getParent(), name);
        if (parentClassData == null) {
          if (lookingThroughPlugins) {
            throw new ClassNotFoundException(name);
          }

          lookingThroughPlugins = true;

          // Check if child class loaders can load the class
          for (var childClassLoader : childClassLoaders) {
            try {
              var pluginClass = loadClassFromClassLoader(childClassLoader, name, resolve);
              if (pluginClass != null) {
                lookingThroughPlugins = false;
                return pluginClass;
              }
            } catch (ClassNotFoundException ignored) {
              // Ignore
            }
          }

          lookingThroughPlugins = false;
          throw new ClassNotFoundException(name);
        }

        c = defineClass(name, parentClassData, 0, parentClassData.length);
      }

      // Resolve the class if requested
      if (resolve) {
        resolveClass(c);
      }

      return c;
    }
  }

  private Class<?> loadClassFromClassLoader(ClassLoader classLoader, String name, boolean resolve)
    throws ClassNotFoundException {
    try {
      return (Class<?>)
        getMethodsClass()
          .getDeclaredMethod("invoke", Object.class, Method.class, Object[].class)
          .invoke(null, classLoader, findLoadedClassMethod, new Object[] {name, resolve});
    } catch (ReflectiveOperationException e) {
      if (e.getCause() != null
        && e.getCause().getCause() != null
        && e.getCause().getCause() instanceof ClassNotFoundException cnfe) {
        throw cnfe;
      } else {
        throw new RuntimeException(e);
      }
    }
  }

  private Class<?> getMethodsClass() throws ClassNotFoundException {
    try {
      return getParent().loadClass("net.lenni0451.reflect.Methods");
    } catch (ClassNotFoundException e) {
      return getClass().getClassLoader().loadClass("net.lenni0451.reflect.Methods");
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
