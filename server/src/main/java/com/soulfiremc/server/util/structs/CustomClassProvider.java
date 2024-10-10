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

import com.google.common.reflect.ClassPath;
import net.lenni0451.classtransform.utils.tree.IClassProvider;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import static net.lenni0451.classtransform.utils.ASMUtils.slash;
import static net.lenni0451.classtransform.utils.Sneaky.sneakySupply;

public class CustomClassProvider implements IClassProvider {
  private final ClassPath[] classPaths;
  private final ClassLoader[] classLoaders;

  public CustomClassProvider(List<ClassLoader> classLoaders) {
    this.classLoaders = classLoaders.toArray(new ClassLoader[0]);

    try {
      this.classPaths = new ClassPath[classLoaders.size()];
      for (var i = 0; i < classLoaders.size(); i++) {
        this.classPaths[i] = ClassPath.from(classLoaders.get(i));
      }
    } catch (Throwable t) {
      throw new RuntimeException("Failed to initialize ClassPath", t);
    }
  }

  @Override
  public byte @NotNull [] getClass(@NotNull String name) throws ClassNotFoundException {
    for (var classLoader : this.classLoaders) {
      var bytes = this.getClassFromLoader(classLoader, name);
      if (bytes != null) {
        return bytes;
      }
    }

    throw new ClassNotFoundException("Class not found: " + name);
  }

  private byte[] getClassFromLoader(ClassLoader classLoader, String name) {
    try (var is = classLoader.getResourceAsStream(slash(name) + ".class")) {
      Objects.requireNonNull(is, "Class input stream is null");
      var baos = new ByteArrayOutputStream();
      var buf = new byte[1024];
      int len;
      while ((len = is.read(buf)) > 0) {
        baos.write(buf, 0, len);
      }
      return baos.toByteArray();
    } catch (Throwable t) {
      return null;
    }
  }

  @Override
  @Nonnull
  public Map<String, Supplier<byte[]>> getAllClasses() {
    var map = new HashMap<String, Supplier<byte[]>>();
    for (var classPath : this.classPaths) {
      for (var classInfo : classPath.getAllClasses()) {
        map.put(classInfo.getName(), sneakySupply(() -> this.getClass(classInfo.getName())));
      }
    }
    return map;
  }
}
