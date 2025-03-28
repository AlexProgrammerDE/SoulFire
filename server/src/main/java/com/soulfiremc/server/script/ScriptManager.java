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
import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.api.event.SoulFireBotEvent;
import com.soulfiremc.server.api.event.SoulFireInstanceEvent;
import com.soulfiremc.server.api.event.attack.AttackBotRemoveEvent;
import com.soulfiremc.server.api.event.attack.AttackEndedEvent;
import com.soulfiremc.server.api.event.attack.AttackStartEvent;
import com.soulfiremc.server.api.event.attack.AttackTickEvent;
import com.soulfiremc.server.api.event.bot.*;
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.script.api.ScriptAPI;
import com.soulfiremc.server.script.api.ScriptBotAPI;
import com.soulfiremc.server.util.SFHelpers;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.lenni0451.lambdaevents.EventHandler;
import net.lenni0451.reflect.stream.RStream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.io.IoBuilder;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftCodec;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
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

  @EventHandler
  public void handleGenericEvent(SoulFireInstanceEvent event) {
    if (event.instanceManager() == instanceManager) {
      forwardEvent(event);
    }
  }

  private static void unlockClass(HostAccess.Builder builder, Class<?> clazz) {
    for (var field : clazz.getFields()) {
      builder.allowAccess(field);
    }

    for (var method : clazz.getMethods()) {
      builder.allowAccess(method);
    }

    for (var constructor : clazz.getConstructors()) {
      builder.allowAccess(constructor);
    }
  }

  private void forwardEvent(SoulFireInstanceEvent event) {
    if (scripts.isEmpty()) {
      return;
    }

    if (event instanceof SoulFireBotEvent botEvent) {
      var botApi = new ScriptBotAPI(botEvent.connection());
      switch (event) {
        case BotConnectionInitEvent ignored -> forwardEvent("connectionInit", botApi);
        case BotDisconnectedEvent ignored -> forwardEvent("disconnected", botApi);
        case BotJoinedEvent ignored -> forwardEvent("joined", botApi);
        case BotPostEntityTickEvent ignored -> forwardEvent("postEntityTick", botApi);
        case BotPostTickEvent ignored -> forwardEvent("postTick", botApi);
        case BotPreEntityTickEvent ignored -> forwardEvent("preEntityTick", botApi);
        case BotPreTickEvent ignored -> forwardEvent("preTick", botApi);
        case ChatMessageReceiveEvent chatMessageReceiveEvent -> forwardEvent("message", botApi, chatMessageReceiveEvent.message(), chatMessageReceiveEvent.timestamp());
        case PreBotConnectEvent ignored -> forwardEvent("preConnect", botApi);
        case SFPacketReceiveEvent packetReceiveEvent -> forwardEvent("packetReceive", botApi, packetReceiveEvent.packet());
        case SFPacketSendingEvent packetSendingEvent -> forwardEvent("packetSending", botApi, packetSendingEvent.packet());
        case SFPacketSentEvent packetSentEvent -> forwardEvent("packetSent", botApi, packetSentEvent.packet());
        default -> {
        }
      }
    } else {
      switch (event) {
        case AttackBotRemoveEvent botRemoveEvent -> forwardEvent("botRemove", new ScriptBotAPI(botRemoveEvent.botConnection()));
        case AttackEndedEvent ignored -> forwardEvent("attackEnded");
        case AttackStartEvent ignored -> forwardEvent("attackStart");
        case AttackTickEvent ignored -> forwardEvent("attackTick");
        default -> {
        }
      }
    }
  }

  private void forwardEvent(String event, Object... eventArgs) {
    for (var script : scripts.values()) {
      var runtime = script.runtime().get();
      if (runtime != null) {
        runtime.scriptAPI().getEvent().forwardEvent(event, eventArgs);
      }
    }
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

  public void killAllScripts() {
    log.info("Stopping scripts");

    for (var script : scripts.values()) {
      killScript(script.scriptId());
    }

    log.info("Stopped scripts");

    SoulFireAPI.unregisterListenersOfObject(this);
  }

  public void killScript(UUID id) {
    var script = scripts.get(id);
    if (script == null) {
      return;
    }

    log.info("Stopping script: {}", script.name());
    var runtime = script.runtime().get();
    if (runtime != null) {
      runtime.context().close();
    }

    script.runtime().set(null);

    log.info("Stopped script: {}", script.name());
  }

  public void startScripts() {
    SoulFireAPI.registerListenersOfObject(this);

    log.info("Starting scripts");
    for (var script : scripts.values()) {
      startScript(script.scriptId());
    }

    log.info("Started scripts");
  }

  public HostAccess buildHostAccess() {
    var builder = HostAccess.newBuilder(HostAccess.CONSTRAINED)
      .allowArrayAccess(true)
      .allowListAccess(true)
      .allowBufferAccess(true)
      .allowIterableAccess(true)
      .allowIteratorAccess(true)
      .allowMapAccess(true)
      .allowBigIntegerNumberAccess(true);

    for (var protocolState : ProtocolState.values()) {
      var codec = MinecraftCodec.CODEC.getCodec(protocolState);
      for (var packet : RStream.of(codec).fields().by("clientboundIds").<Map<Class<?>, Integer>>get().keySet()) {
        unlockClass(builder, packet);
      }

      for (var packet : RStream.of(codec).fields().by("serverboundIds").<Map<Class<?>, Integer>>get().keySet()) {
        unlockClass(builder, packet);
      }
    }

    unlockClass(builder, Component.class);
    unlockClass(builder, Vector3i.class);
    unlockClass(builder, Vector3d.class);
    unlockClass(builder, SFVec3i.class);

    return builder.build();
  }

  public void startScript(UUID id) {
    var script = scripts.get(id);
    if (script == null) {
      return;
    }

    log.info("Starting script: {}", script.name());
    var scriptLogger = LogManager.getLogger("Script: %s".formatted(script.name()));
    var context = Context.newBuilder(script.language().languageId())
      .allowExperimentalOptions(true)
      .engine(Engine.newBuilder(script.language().languageId())
        .sandbox(SandboxPolicy.TRUSTED)
        .option("engine.WarnInterpreterOnly", "false")
        .in(InputStream.nullInputStream())
        .out(IoBuilder.forLogger(scriptLogger).setLevel(Level.INFO).buildPrintStream())
        .err(IoBuilder.forLogger(scriptLogger).setLevel(Level.ERROR).buildPrintStream())
        .logHandler(IoBuilder.forLogger(ScriptManager.class).setLevel(Level.INFO).buildOutputStream())
        .build())
      .sandbox(SandboxPolicy.TRUSTED)
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
      .option("js.nashorn-compat", "true")
      .option("js.ecmascript-version", "latest")
      .allowAllAccess(false)
      .allowNativeAccess(false)
      .allowCreateProcess(false)
      .allowCreateThread(true)
      .allowInnerContextOptions(false)
      .allowPolyglotAccess(PolyglotAccess.NONE)
      .allowHostClassLoading(false)
      .allowHostAccess(buildHostAccess())
      .allowEnvironmentAccess(EnvironmentAccess.NONE)
      .currentWorkingDirectory(script.dataPath().toAbsolutePath())
      .build();

    var scriptAPI = new ScriptAPI(script, instanceManager);
    context.getBindings(script.language().languageId())
      .putMember("api", scriptAPI);

    script.runtime().set(new RuntimeComponents(
      context,
      scriptAPI
    ));

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

    log.info("Started script: {}", script.name());
  }

  public record Script(UUID scriptId, String name, Path dataPath, Path codePath, ScriptLanguage language, AtomicReference<RuntimeComponents> runtime) {
  }

  public record RuntimeComponents(Context context, ScriptAPI scriptAPI) {
  }
}
