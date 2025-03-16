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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.io.IoBuilder;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.SandboxPolicy;
import org.graalvm.polyglot.io.IOAccess;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class ScriptManager {
  private final Map<UUID, Script> scripts = new ConcurrentHashMap<>();

  public void registerScript(Script script) {
    scripts.put(script.scriptId(), script);
  }

  public void spawnScripts() {
    for (var script : scripts.values()) {
      spawnScript(script);
    }
  }

  private void spawnScript(Script script) {
    var context = Context.newBuilder(script.language().languageId())
      .sandbox(SandboxPolicy.CONSTRAINED)
      .in(new ByteArrayInputStream(new byte[0]))
      .out(IoBuilder.forLogger(script.logger()).setLevel(Level.INFO).buildPrintStream())
      .err(IoBuilder.forLogger(script.logger()).setLevel(Level.ERROR).buildPrintStream())
      .allowIO(IOAccess.newBuilder()
        .fileSystem(new SandboxedFileSystem(script.runPath()))
        .build())
      .build();

    script.context().set(context);
  }

  public record Script(UUID scriptId, Path runPath, Logger logger, ScriptLanguage language, ScriptSecurityMode security, AtomicReference<Context> context) {
  }
}
