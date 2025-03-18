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
import com.soulfiremc.server.util.SFHelpers;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
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
  public void registerScript(UUID scriptId, Logger logger, ScriptLanguage language) {
    var dataPath = instanceManager.getInstanceObjectStoragePath().resolve("script-data-" + scriptId);
    var codePath = instanceManager.soulFireServer().getObjectStoragePath().resolve("script-code-" + scriptId);
    Files.createDirectories(dataPath);
    Files.createDirectories(codePath);

    scripts.put(scriptId, new Script(
      scriptId,
      dataPath,
      codePath,
      logger,
      language,
      new AtomicReference<>()
    ));
  }

  public void startScripts() {
    log.info("Starting scripts");
    for (var script : scripts.values()) {
      var context = Context.newBuilder(script.language().languageId())
        .allowExperimentalOptions(true)
        .sandbox(switch (script.language()) {
          case JAVASCRIPT -> SandboxPolicy.CONSTRAINED;
          case PYTHON -> SandboxPolicy.TRUSTED;
        })
        .in(InputStream.nullInputStream())
        .out(IoBuilder.forLogger(script.logger()).setLevel(Level.INFO).buildPrintStream())
        .err(IoBuilder.forLogger(script.logger()).setLevel(Level.ERROR).buildPrintStream())
        .allowIO(IOAccess.newBuilder()
          .allowHostFileAccess(false)
          .allowHostSocketAccess(false)
          .fileSystem(new SandboxedFileSystem(
            Set.of(script.runPath().toAbsolutePath()),
            Set.of(script.codePath().toAbsolutePath(), graalResourceCache.toAbsolutePath()),
            script.runPath().toAbsolutePath()
          ))
          .build())
        .environment("SF_SCRIPT_ID", script.scriptId().toString())
        .environment("SF_SCRIPT_DATA_PATH", script.runPath().toAbsolutePath().toString())
        .environment("SF_SCRIPT_CODE_PATH", script.codePath().toAbsolutePath().toString())
        .option("js.strict", "true")
        .option("js.commonjs-require", "true")
        .option("js.commonjs-require-cwd", script.codePath().resolve("node_modules").toAbsolutePath().toString())
        .allowNativeAccess(false)
        .allowAllAccess(false)
        .allowEnvironmentAccess(EnvironmentAccess.NONE)
        .allowCreateProcess(false)
        .allowInnerContextOptions(false)
        .allowPolyglotAccess(PolyglotAccess.NONE)
        .allowHostAccess(HostAccess.UNTRUSTED)
        .logHandler(IoBuilder.forLogger(ScriptManager.class).setLevel(Level.INFO).buildOutputStream())
        .currentWorkingDirectory(script.runPath().toAbsolutePath())
        .build();

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

  public record Script(UUID scriptId, Path runPath, Path codePath, Logger logger, ScriptLanguage language, AtomicReference<Context> context) {
  }
}
