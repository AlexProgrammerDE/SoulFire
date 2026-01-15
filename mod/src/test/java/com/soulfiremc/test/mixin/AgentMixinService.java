/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.test.mixin;

import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;
import lombok.SneakyThrows;
import net.lenni0451.reflect.Agents;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.launch.platform.MixinPlatformAgentDefault;
import org.spongepowered.asm.launch.platform.container.ContainerHandleURI;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.service.*;
import org.spongepowered.asm.transformers.MixinClassReader;
import org.spongepowered.asm.util.perf.Profiler;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

public class AgentMixinService extends MixinServiceAbstract implements IClassProvider, IClassBytecodeProvider, ITransformerProvider, IClassTracker {

  @Override
  public ClassNode getClassNode(String name) throws ClassNotFoundException {
    return this.getClassNode(name, true);
  }

  @Override
  public ClassNode getClassNode(String name, boolean runTransformers) throws ClassNotFoundException {
    return this.getClassNode(name, this.getClassBytes(name, runTransformers), ClassReader.EXPAND_FRAMES);
  }

  @Override
  public ClassNode getClassNode(String name, boolean runTransformers, int readerFlags) throws ClassNotFoundException {
    return this.getClassNode(name, this.getClassBytes(name, runTransformers), readerFlags);
  }

  private ClassNode getClassNode(String className, byte[] classBytes, int flags) {
    ClassNode classNode = new ClassNode();
    ClassReader classReader = new MixinClassReader(classBytes, className);
    classReader.accept(classNode, flags);
    return classNode;
  }

  private byte[] getClassBytes(String className, boolean runTransformers) throws ClassNotFoundException {
    className = className.replace('/', '.');

    Profiler profiler = Profiler.getProfiler("mixin");
    Profiler.Section loadTime = profiler.begin(Profiler.ROOT, "class.load");
    byte[] classBytes = this.getClassBytes(className);
    loadTime.end();

    if (runTransformers) {
      Profiler.Section transformTime = profiler.begin(Profiler.ROOT, "class.transform");
      classBytes = this.applyTransformers(className, classBytes, profiler);
      transformTime.end();
    }

    if (classBytes == null) {
      throw new ClassNotFoundException("The specified class '%s' was not found".formatted(className));
    }

    return classBytes;
  }

  private byte[] getClassBytes(String name) {
    InputStream classStream = null;
    try {
      final String resourcePath = name.replace('.', '/').concat(".class");
      classStream = this.getClass().getClassLoader().getResourceAsStream(resourcePath);
      if (classStream == null) {
        return null;
      }

      return ByteStreams.toByteArray(classStream);
    } catch (Exception _) {
      return null;
    } finally {
      Closeables.closeQuietly(classStream);
    }
  }

  private byte[] applyTransformers(String name, byte[] basicClass, Profiler profiler) {
    for (ITransformer transformer : this.getTransformers()) {
      if (!(transformer instanceof ILegacyClassTransformer legacyClassTransformer)) {
        continue;
      }
      // Clear the re-entrance semaphore
      this.lock.clear();

      int pos = transformer.getName().lastIndexOf('.');
      String simpleName = transformer.getName().substring(pos + 1);
      Profiler.Section transformTime = profiler.begin(Profiler.FINE, simpleName.toLowerCase(Locale.ROOT));
      transformTime.setInfo(transformer.getName());
      basicClass = legacyClassTransformer.transformClassBytes(name, name, basicClass);
      transformTime.end();

      if (this.lock.isSet()) {
        // Also add it to the exclusion list so we can exclude it if the environment triggers a rebuild
        this.addTransformerExclusion(transformer.getName());
        this.lock.clear();
        System.out.printf("A re-entrant transformer '%s' was detected and will no longer process meta class data%n", transformer.getName());
      }
    }

    return basicClass;
  }

  @Override
  public URL[] getClassPath() {
    return new URL[0];
  }

  @Override
  public Class<?> findClass(String name) throws ClassNotFoundException {
    return this.getClass().getClassLoader().loadClass(name);
  }

  @Override
  public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
    return Class.forName(name, initialize, this.getClass().getClassLoader());
  }

  @Override
  public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
    return Class.forName(name, initialize, this.getClass().getClassLoader());
  }

  @Override
  public String getName() {
    return "AgentMixin";
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public IClassProvider getClassProvider() {
    return this;
  }

  @Override
  public IClassBytecodeProvider getBytecodeProvider() {
    return this;
  }

  @Override
  public ITransformerProvider getTransformerProvider() {
    return this;
  }

  @Override
  public IClassTracker getClassTracker() {
    return this;
  }

  @Override
  public IMixinAuditTrail getAuditTrail() {
    return null;
  }

  @Override
  public Collection<String> getPlatformAgents() {
    return Collections.singletonList(MixinPlatformAgentDefault.class.getName());
  }

  @Override
  public IContainerHandle getPrimaryContainer() {
    try {
      return new ContainerHandleURI(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public InputStream getResourceAsStream(String name) {
    return this.getClass().getClassLoader().getResourceAsStream(name);
  }

  @Override
  public MixinEnvironment.CompatibilityLevel getMinCompatibilityLevel() {
    return MixinEnvironment.CompatibilityLevel.JAVA_21;
  }

  @Override
  public void registerInvalidClass(String className) {
  }

  @SneakyThrows
  @Override
  public boolean isClassLoaded(String className) {
    for (Class<?> clazz : Agents.getInstrumentation().getAllLoadedClasses()) {
      if (clazz != null && clazz.getName().equals(className)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String getClassRestrictions(String className) {
    return "";
  }

  @Override
  public Collection<ITransformer> getTransformers() {
    return Collections.emptyList();
  }

  @Override
  public Collection<ITransformer> getDelegatedTransformers() {
    return Collections.emptyList();
  }

  @Override
  public void addTransformerExclusion(String name) {
  }

}
