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
package com.soulfiremc.server.script;

import com.soulfiremc.server.InstanceManager;
import com.soulfiremc.server.script.api.ScriptAPI;
import com.soulfiremc.server.util.SFHelpers;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.io.IoBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.graalvm.polyglot.*;
import org.graalvm.polyglot.io.IOAccess;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class ScriptManager {
  private final InstanceManager instanceManager;
  private final Path graalResourceCache;
  private final Map<UUID, Script> scripts = new ConcurrentHashMap<>();

  public ScriptManager(InstanceManager instanceManager) {
    this.instanceManager = instanceManager;
    this.graalResourceCache = instanceManager.soulFireServer().baseDirectory().resolve(".graal-resource-cache");
    System.setProperty("polyglot.engine.userResourceCache", graalResourceCache.toString());
  }

  @SneakyThrows
  public void registerScript(UUID id, String name, ScriptLanguage language) {
    var dataPath = instanceManager.getInstanceObjectStoragePath().resolve("script-data-" + id);
    var codePath = instanceManager.soulFireServer().getObjectStoragePath().resolve("script-code-" + id);
    Files.createDirectories(dataPath);
    Files.createDirectories(codePath);

    scripts.put(id, new Script(
      id,
      name,
      dataPath,
      codePath,
      language,
      new AtomicReference<>()
    ));
  }

  static String firstLetterUpperCase(String name) {
    if (name.isEmpty()) {
      return name;
    }
    return Character.toUpperCase(name.charAt(0)) + name.substring(1);
  }

  public void killAllScripts() {
    log.info("Stopping scripts");

    for (var script : scripts.values()) {
      script.context().get().close();
    }

    log.info("Stopped scripts");
  }

  public record Script(UUID scriptId, String name, Path dataPath, Path codePath, ScriptLanguage language, AtomicReference<Context> context) {
  }

  static String firstLetterLowerCase(String name) {
    if (name.isEmpty()) {
      return name;
    }
    return Character.toLowerCase(name.charAt(0)) + name.substring(1);
  }

  public void startScripts() {
    log.info("Starting scripts");
    for (var script : scripts.values()) {
      var sandboxPolicy = switch (script.language()) {
        case JAVASCRIPT -> SandboxPolicy.CONSTRAINED;
        case PYTHON -> SandboxPolicy.TRUSTED;
      };
      var context = Context.newBuilder(script.language().languageId())
        .allowExperimentalOptions(true)
        .engine(Engine.newBuilder(script.language().languageId())
          .sandbox(sandboxPolicy)
          .option("engine.WarnInterpreterOnly", "false")
          .in(InputStream.nullInputStream())
          .out(IoBuilder.forLogger(script.name()).setLevel(Level.INFO).buildPrintStream())
          .err(IoBuilder.forLogger(script.name()).setLevel(Level.ERROR).buildPrintStream())
          .logHandler(IoBuilder.forLogger(ScriptManager.class).setLevel(Level.INFO).buildOutputStream())
          .build())
        .sandbox(sandboxPolicy)
        .allowIO(IOAccess.newBuilder()
          .allowHostFileAccess(false)
          .allowHostSocketAccess(false)
          .fileSystem(new SandboxedFileSystem(
            Set.of(script.dataPath().toAbsolutePath()),
            Set.of(script.codePath().toAbsolutePath(), graalResourceCache.toAbsolutePath()),
            script.dataPath().toAbsolutePath()
          ))
          .build())
        .option("js.strict", "true")
        .allowAllAccess(false)
        .allowNativeAccess(false)
        .allowCreateProcess(false)
        .allowCreateThread(true)
        .allowInnerContextOptions(false)
        .allowPolyglotAccess(PolyglotAccess.NONE)
        .allowHostClassLoading(false)
        .allowHostAccess(HostAccess.newBuilder(HostAccess.CONSTRAINED)
          .allowArrayAccess(true)
          .allowListAccess(true)
          .allowMapAccess(true)
          .allowBufferAccess(true)
          .allowIterableAccess(true)
          .allowIteratorAccess(true)
          .allowBigIntegerNumberAccess(true)
          .build())
        .allowEnvironmentAccess(EnvironmentAccess.NONE)
        .currentWorkingDirectory(script.dataPath().toAbsolutePath())
        .build();

      context.getBindings(script.language().languageId())
        .putMember("api", BeanWrapper.wrap(context.asValue(new ScriptAPI(script, instanceManager))));

      script.context().set(context);

      instanceManager.scheduler().schedule(() -> {
        SFHelpers.mustSupply(() -> switch (script.language()) {
          case JAVASCRIPT -> () -> {
            var mainFile = script.codePath().resolve("main.js");
            if (!Files.exists(mainFile)) {
              throw new IllegalStateException("main.js not found");
            }

            try {
              context.eval(Source.newBuilder("js", mainFile.toFile()).build());
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          };
          case PYTHON -> () -> {
            var mainFile = script.codePath().resolve("main.py");
            if (!Files.exists(mainFile)) {
              throw new IllegalStateException("main.py not found");
            }

            try {
              context.eval(Source.newBuilder("python", mainFile.toFile()).build());
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          };
        });
      });
    }

    log.info("Started scripts");
  }

  @RequiredArgsConstructor
  static final class BeanWrapper implements ProxyObject {
    private final Value delegate;

    public static Object wrap(Value polyglotValue) {
      if (polyglotValue.isHostObject()) {
        if (polyglotValue.asHostObject() instanceof BeanWrapper) {
          return polyglotValue;
        } else {
          return new BeanWrapper(polyglotValue);
        }
      } else {
        return polyglotValue;
      }
    }

    @Override
    public Object getMember(String key) {
      var getter = getGetter(key);
      if (getter != null && getter.canExecute()) {
        return wrap(getter.execute());
      } else {
        return wrap(delegate.getMember(key));
      }
    }

    @Override
    public List<String> getMemberKeys() {
      var list = new ArrayList<>(delegate.getMemberKeys());
      for (var key : delegate.getMemberKeys()) {
        if (key.startsWith("get")) {
          list.add(firstLetterLowerCase(key.substring(3)));
        } else if (key.startsWith("is")) {
          list.add(firstLetterLowerCase(key.substring(2)));
        }
      }

      return list;
    }

    @Override
    public boolean hasMember(String key) {
      return getGetter(key) != null || delegate.hasMember(key);
    }

    @Override
    public void putMember(String key, Value value) {
      var setter = getSetter(key);
      if (setter != null && setter.canExecute()) {
        setter.execute(value);
      } else {
        delegate.putMember(key, value);
      }
    }

    private @Nullable Value getGetter(String key) {
      var getter = nullableMember("get" + firstLetterUpperCase(key));
      if (getter == null) {
        getter = nullableMember("is" + firstLetterUpperCase(key));
      }
      return getter;
    }

    private @Nullable Value getSetter(String key) {
      return nullableMember("set" + firstLetterUpperCase(key));
    }

    private @Nullable Value nullableMember(String key) {
      return delegate.hasMember(key) ? delegate.getMember(key) : null;
    }
  }
}
