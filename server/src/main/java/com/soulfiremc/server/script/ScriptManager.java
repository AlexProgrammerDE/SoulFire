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
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.io.IoBuilder;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.SandboxPolicy;
import org.graalvm.polyglot.io.IOAccess;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@RequiredArgsConstructor
public class ScriptManager {
  private final InstanceManager instanceManager;
  private final Map<UUID, Script> scripts = new ConcurrentHashMap<>();

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
        .sandbox(SandboxPolicy.CONSTRAINED)
        .in(new ByteArrayInputStream(new byte[0]))
        .out(IoBuilder.forLogger(script.logger()).setLevel(Level.INFO).buildPrintStream())
        .err(IoBuilder.forLogger(script.logger()).setLevel(Level.ERROR).buildPrintStream())
        .allowIO(IOAccess.newBuilder()
          .fileSystem(new SandboxedFileSystem(script.runPath(), script.codePath()))
          .build())
        .logHandler(IoBuilder.forLogger("ScriptManager").setLevel(Level.INFO).buildOutputStream())
        .currentWorkingDirectory(script.runPath())
        .build();

      script.context().set(context);
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
