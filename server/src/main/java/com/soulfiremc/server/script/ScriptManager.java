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

import com.caoccao.javet.swc4j.Swc4j;
import com.caoccao.javet.swc4j.enums.Swc4jMediaType;
import com.caoccao.javet.swc4j.enums.Swc4jSourceMapOption;
import com.caoccao.javet.swc4j.exceptions.Swc4jCoreException;
import com.caoccao.javet.swc4j.options.Swc4jTranspileOptions;
import com.soulfiremc.server.InstanceManager;
import com.soulfiremc.server.SoulFireScheduler;
import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.api.event.SoulFireBotEvent;
import com.soulfiremc.server.api.event.SoulFireInstanceEvent;
import com.soulfiremc.server.api.event.attack.AttackBotRemoveEvent;
import com.soulfiremc.server.api.event.attack.AttackEndedEvent;
import com.soulfiremc.server.api.event.attack.AttackStartEvent;
import com.soulfiremc.server.api.event.attack.AttackTickEvent;
import com.soulfiremc.server.api.event.bot.*;
import com.soulfiremc.server.database.ScriptEntity;
import com.soulfiremc.server.pathfinding.SFVec3i;
import com.soulfiremc.server.script.api.ScriptAPI;
import com.soulfiremc.server.script.api.ScriptBotAPI;
import com.soulfiremc.server.util.SFHelpers;
import com.soulfiremc.server.util.log4j.SFLogAppender;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
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
import java.io.OutputStream;
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
  @Getter
  private final Map<UUID, Script> scripts = new ConcurrentHashMap<>();

  public ScriptManager(InstanceManager instanceManager) {
    this.instanceManager = instanceManager;
    this.graalResourceCache = instanceManager.soulFireServer().baseDirectory().resolve(".graal-resource-cache");
    System.setProperty("polyglot.engine.userResourceCache", graalResourceCache.toString());
    SoulFireAPI.registerListenersOfObject(this);
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

  public void registerScript(ScriptEntity scriptEntity) {
    this.registerScript(
      scriptEntity.id(),
      scriptEntity.scriptName(),
      scriptEntity.type(),
      scriptEntity.elevatedPermissions()
    );
  }

  public void maybeReRegisterScript(ScriptEntity scriptEntity) {
    if (scripts.containsKey(scriptEntity.id())) {
      this.registerScript(scriptEntity);
    }
  }

  @SneakyThrows
  public void registerScript(UUID id, String name, ScriptEntity.ScriptType scriptType, boolean elevatedPermissions) {
    if (scripts.containsKey(id)) {
      log.info("Reloading script: {}", name);
      this.killScript(id);
    }

    var dataPath = instanceManager.getScriptDataPath(id);
    Files.createDirectories(dataPath);

    var codePath = instanceManager.soulFireServer().getScriptCodePath(id);
    Files.createDirectories(codePath);

    var scriptLanguage = ScriptLanguage.determineLanguage(codePath);
    scripts.put(id, new Script(
      id,
      name,
      dataPath,
      codePath,
      scriptType,
      elevatedPermissions,
      scriptLanguage,
      new AtomicReference<>()
    ));

    this.startScript(id);
  }

  public void destroyManager() {
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

  public void unregisterScript(UUID id) {
    this.killScript(id);
    scripts.remove(id);
  }

  public HostAccess buildHostAccess(Script script) {
    if (script.elevatedPermissions) {
      return HostAccess.ALL;
    }

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

    if (script.language() == ScriptLanguage.TYPESCRIPT) {
      compileTypescriptFiles(script);
    }

    log.info("Starting script: {}", script.name());
    var scriptLogger = LogManager.getLogger((switch (script.scriptType) {
      case INSTANCE -> "Instance Script: %s";
      case GLOBAL -> "Global Script: %s";
    }).formatted(script.name()));
    var wrapper = instanceManager.runnableWrapper().with(new ScriptRunnableWrapper(id));
    var sandbox = script.elevatedPermissions ? SandboxPolicy.TRUSTED : switch (script.language) {
      case JAVASCRIPT, TYPESCRIPT -> SandboxPolicy.CONSTRAINED;
      case PYTHON -> SandboxPolicy.TRUSTED;
    };
    var context = Context.newBuilder(script.language().languageId())
      .allowExperimentalOptions(true)
      .engine(Engine.newBuilder(script.language().languageId())
        .sandbox(sandbox)
        .option("engine.WarnInterpreterOnly", "false")
        .in(InputStream.nullInputStream())
        .out(new WrappingOutputStream(IoBuilder.forLogger(scriptLogger).setLevel(Level.INFO).buildOutputStream(), wrapper))
        .err(new WrappingOutputStream(IoBuilder.forLogger(scriptLogger).setLevel(Level.ERROR).buildOutputStream(), wrapper))
        .logHandler(new WrappingOutputStream(IoBuilder.forLogger(ScriptManager.class).setLevel(Level.INFO).buildOutputStream(), wrapper))
        .build())
      .sandbox(sandbox)
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
      .option("js.ecmascript-version", "latest")
      .allowAllAccess(false)
      .allowNativeAccess(false)
      .allowCreateProcess(false)
      .allowCreateThread(true)
      .allowInnerContextOptions(false)
      .allowPolyglotAccess(PolyglotAccess.NONE)
      .allowHostClassLoading(false)
      .allowHostAccess(buildHostAccess(script))
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

    var mainFile = script.codePath().resolve(script.language.entryFile());
    if (!Files.exists(mainFile)) {
      log.warn("Main file not found for script: {}", script.name());
      return;
    }

    try {
      context.eval(Source.newBuilder(script.language.languageId(), mainFile.toFile()).build());
    } catch (IOException e) {
      log.error("Failed to load script", e);
      return;
    }

    log.info("Started script: {}", script.name());
  }

  private void compileTypescriptFiles(Script script) {
    log.info("Compiling TypeScript files for script: {}", script.name());
    try {
      Files.walk(script.codePath())
        .filter(Files::isRegularFile)
        .filter(path -> path.toString().endsWith(".ts"))
        .forEach(this::compileTypescriptFile);
    } catch (IOException e) {
      log.error("Failed to create dist folder for script", e);
    }
  }

  private void compileTypescriptFile(Path file) {
    try {
      var swc4j = new Swc4j();
      var options = new Swc4jTranspileOptions()
        .setSpecifier(file.toUri().toURL())
        .setInlineSources(false)
        .setSourceMap(Swc4jSourceMapOption.None)
        .setMediaType(Swc4jMediaType.TypeScript);
      var output = swc4j.transpile(Files.readString(file), options);
      var outFileName = SFHelpers.changeExtension(file.getFileName().toString(), "js");
      var outFile = file.resolveSibling(outFileName);

      Files.writeString(outFile, output.getCode());
    } catch (IOException e) {
      log.error("Failed to create dist folder for script: {}", file, e);
    } catch (Swc4jCoreException e) {
      throw new RuntimeException(e);
    }
  }

  public record Script(
    UUID scriptId,
    String name,
    Path dataPath,
    Path codePath,
    ScriptEntity.ScriptType scriptType,
    boolean elevatedPermissions,
    ScriptLanguage language,
    AtomicReference<RuntimeComponents> runtime
  ) {
  }

  public record RuntimeComponents(Context context, ScriptAPI scriptAPI) {
  }

  @RequiredArgsConstructor
  private static class WrappingOutputStream extends OutputStream {
    private final OutputStream delegate;
    private final SoulFireScheduler.RunnableWrapper wrapper;

    @Override
    public void write(int b) throws IOException {
      wrapper.runWrappedWithIOException(() -> delegate.write(b));
    }

    @Override
    public void write(byte[] b) throws IOException {
      wrapper.runWrappedWithIOException(() -> delegate.write(b));
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      wrapper.runWrappedWithIOException(() -> delegate.write(b, off, len));
    }

    @Override
    public void flush() throws IOException {
      wrapper.runWrappedWithIOException(delegate::flush);
    }

    @Override
    public void close() throws IOException {
      wrapper.runWrappedWithIOException(delegate::close);
    }
  }

  private record ScriptRunnableWrapper(UUID scriptId) implements SoulFireScheduler.RunnableWrapper {
    @Override
    public Runnable wrap(Runnable runnable) {
      return () -> {
        try (var ignored1 = SFHelpers.smartMDCCloseable(SFLogAppender.SF_SCRIPT_ID, scriptId.toString())) {
          runnable.run();
        }
      };
    }
  }
}
