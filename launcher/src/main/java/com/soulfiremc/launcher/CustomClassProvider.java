package com.soulfiremc.launcher;

import com.google.common.reflect.ClassPath;
import net.lenni0451.classtransform.utils.tree.IClassProvider;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import static net.lenni0451.classtransform.utils.ASMUtils.slash;
import static net.lenni0451.classtransform.utils.Sneaky.sneakySupply;

public final class CustomClassProvider implements IClassProvider {
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
  public byte @NonNull [] getClass(@NonNull String name) throws ClassNotFoundException {
    for (var classLoader : this.classLoaders) {
      var bytes = this.getClassFromLoader(classLoader, name);
      if (bytes != null) {
        return bytes;
      }
    }

    throw new ClassNotFoundException("Class not found: " + name);
  }

  private byte @Nullable [] getClassFromLoader(ClassLoader classLoader, String name) {
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
