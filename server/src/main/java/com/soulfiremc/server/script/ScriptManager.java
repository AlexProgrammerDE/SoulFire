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
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.io.IoBuilder;
import org.graalvm.polyglot.*;
import org.graalvm.polyglot.io.IOAccess;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
        .allowHostAccess(HostAccess.CONSTRAINED)
        .allowEnvironmentAccess(EnvironmentAccess.NONE)
        .currentWorkingDirectory(script.dataPath().toAbsolutePath())
        .build();

      context.getBindings(script.language().languageId())
        .putMember("api", new ScriptAPI(script, instanceManager));

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

  public void killAllScripts() {
    log.info("Stopping scripts");

    for (var script : scripts.values()) {
      script.context().get().close();
    }

    log.info("Stopped scripts");
  }

  public record Script(UUID scriptId, String name, Path dataPath, Path codePath, ScriptLanguage language, AtomicReference<Context> context) {
  }
}
