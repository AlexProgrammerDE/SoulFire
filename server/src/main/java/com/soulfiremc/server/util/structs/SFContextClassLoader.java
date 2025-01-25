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

import lombok.SneakyThrows;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class SFContextClassLoader extends URLClassLoader {
  // Prevent infinite loop when plugins are looking for classes inside this class loader
  private static final ThreadLocal<Boolean> PREVENT_LOOP = ThreadLocal.withInitial(() -> false);
  private final List<ClassLoader> childClassLoaders = new ArrayList<>();

  @SneakyThrows
  public SFContextClassLoader(Path libDir) {
    super(createLibClassLoader(libDir), ClassLoader.getSystemClassLoader());
    Thread.currentThread().setContextClassLoader(this);

    var constantsClass = loadClass("com.soulfiremc.launcher.SoulFireClassloaderConstants");
    var pluginUrlsField = constantsClass.getField("CHILD_CLASSLOADER_CONSUMER");
    pluginUrlsField.set(null, (Consumer<ClassLoader>) childClassLoaders::add);
  }

  private static ClassLoader getParentClassLoader() {
    return ClassLoader.getSystemClassLoader().getParent();
  }

  private static String nameToPath(String name) {
    return name.replace('.', '/').concat(".class");
  }

  private static URL[] createLibClassLoader(Path libDir) {
    try (var dependencyListInput = ClassLoader.getSystemClassLoader().getResourceAsStream("META-INF/dependency-list.txt")) {
      if (dependencyListInput == null) {
        return new URL[0];
      }

      Files.createDirectories(libDir);

      var urls = new ArrayList<URL>();
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

      return urls.toArray(new URL[0]);
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
          c = getPlatformClassLoader().loadClass(name);
        } catch (ClassNotFoundException ignored) {
          // Ignore
        }

        // In the next step, we pretend we own the classes
        // of either the parent or the child classloaders
        if (c == null) {
          c = loadParentClass(name);
          if (c == null) {
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
        }
      }

      // Resolve the class if requested
      if (resolve) {
        resolveClass(c);
      }

      return c;
    }
  }

  private Class<?> loadParentClass(String name) {
    try {
      var classBytes = this.getClassBytes(name);
      if (classBytes == null) {
        return null;
      }

      var connection = this.getClassConnection(name);
      var url = connection == null ? null : connection.getURL();

      CodeSigner[] codeSigner = null;
      Manifest manifest = null;
      if (connection instanceof JarURLConnection jarConnection) {
        var jarFile = jarConnection.getJarFile();
        url = jarConnection.getJarFileURL();

        if (jarFile != null && jarFile.getManifest() != null) {
          manifest = jarFile.getManifest();
          var entry = jarFile.getJarEntry(nameToPath(name));
          if (entry != null) {
            codeSigner = entry.getCodeSigners();
          }
        }
      }

      var i = name.lastIndexOf('.');
      if (i != -1) {
        var pkgName = name.substring(0, i);
        // Check if package already loaded.
        if (getAndVerifyPackage(pkgName, manifest, url) == null) {
          try {
            if (manifest != null) {
              definePackage(pkgName, manifest, url);
            } else {
              definePackage(pkgName, null, null, null, null, null, null, null);
            }
          } catch (IllegalArgumentException iae) {
            // parallel-capable class loaders: re-verify in case of a
            // race condition
            if (getAndVerifyPackage(pkgName, manifest, url) == null) {
              // Should never happen
              throw new AssertionError("Cannot find package " +
                pkgName);
            }
          }
        }
      }

      CodeSource codeSource = null;
      if (connection != null) codeSource = new CodeSource(url, codeSigner);
      return this.defineClass(name, classBytes, 0, classBytes.length, codeSource);
    } catch (Exception e) {
      return null;
    }
  }

  @SuppressWarnings("deprecation")
  private URLConnection getClassConnection(String className) throws IOException {
    var url = this.getResource(nameToPath(className));
    if (url != null) {
      if ("jar".equalsIgnoreCase(url.getProtocol()) && url.getRef() == null) {
        //Append the '#runtime' ref to make sure the opened jarfile handles multi release jars correctly
        url = new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getFile() + "#runtime");
      }
      return url.openConnection();
    }
    return null;
  }

  private Package getAndVerifyPackage(String pkgName,
                                      Manifest man, URL url) {
    var pkg = getDefinedPackage(pkgName);
    if (pkg != null) {
      // Package found, so check package sealing.
      if (pkg.isSealed()) {
        // Verify that code source URL is the same.
        if (!pkg.isSealed(url)) {
          throw new SecurityException(
            "sealing violation: package " + pkgName + " is sealed");
        }
      } else {
        // Make sure we are not attempting to seal the package
        // at this code source URL.
        if ((man != null) && isSealed(pkgName, man)) {
          throw new SecurityException(
            "sealing violation: can't seal package " + pkgName +
              ": already loaded");
        }
      }
    }
    return pkg;
  }

  private byte[] getClassBytes(String className) {
    try (var inputStream = this.getResourceAsStream(nameToPath(className))) {
      if (inputStream == null) {
        return null;
      }

      return inputStream.readAllBytes();
    } catch (IOException ignored) {
      return null;
    }
  }

  private boolean isSealed(final String path, final Manifest manifest) {
    var attributes = manifest.getAttributes(path);
    String sealed = null;
    if (attributes != null) sealed = attributes.getValue(Attributes.Name.SEALED);

    if (sealed == null) {
      attributes = manifest.getMainAttributes();
      if (attributes != null) sealed = attributes.getValue(Attributes.Name.SEALED);
    }
    return "true".equalsIgnoreCase(sealed);
  }

  @Override
  public URL findResource(String name) {
    var thisLoaderResource = super.findResource(name);
    if (thisLoaderResource != null) {
      return thisLoaderResource;
    }

    return this.getParent().getResource(name);
  }
}
