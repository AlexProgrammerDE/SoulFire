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
package com.soulfiremc.server;

import ch.jalu.injector.Injector;
import ch.jalu.injector.InjectorBuilder;
import com.soulfiremc.builddata.BuildData;
import com.soulfiremc.grpc.generated.SettingsPage;
import com.soulfiremc.server.api.EventBusOwner;
import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.api.event.EventExceptionHandler;
import com.soulfiremc.server.api.event.SoulFireGlobalEvent;
import com.soulfiremc.server.api.event.attack.InstanceInitEvent;
import com.soulfiremc.server.api.event.lifecycle.InstanceSettingsRegistryInitEvent;
import com.soulfiremc.server.api.event.lifecycle.ServerSettingsRegistryInitEvent;
import com.soulfiremc.server.data.TranslationMapper;
import com.soulfiremc.server.grpc.RPCServer;
import com.soulfiremc.server.settings.AccountSettings;
import com.soulfiremc.server.settings.BotSettings;
import com.soulfiremc.server.settings.DevSettings;
import com.soulfiremc.server.settings.ProxySettings;
import com.soulfiremc.server.settings.lib.ServerSettingsRegistry;
import com.soulfiremc.server.settings.lib.SettingsHolder;
import com.soulfiremc.server.spark.SFSparkPlugin;
import com.soulfiremc.server.user.AuthSystem;
import com.soulfiremc.server.util.SFUpdateChecker;
import com.soulfiremc.server.viaversion.SFViaLoader;
import com.soulfiremc.server.viaversion.platform.SFViaAprilFools;
import com.soulfiremc.server.viaversion.platform.SFViaBackwards;
import com.soulfiremc.server.viaversion.platform.SFViaBedrock;
import com.soulfiremc.server.viaversion.platform.SFViaLegacy;
import com.soulfiremc.server.viaversion.platform.SFViaPlatform;
import com.soulfiremc.server.viaversion.platform.SFViaRewind;
import com.soulfiremc.util.KeyHelper;
import com.soulfiremc.util.SFPathConstants;
import com.soulfiremc.util.ShutdownManager;
import com.viaversion.viaversion.ViaManagerImpl;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.protocol.ProtocolManagerImpl;
import io.jsonwebtoken.Jwts;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.SecretKey;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.lenni0451.lambdaevents.LambdaManager;
import net.lenni0451.lambdaevents.generator.ASMGenerator;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.pf4j.PluginManager;

@Slf4j
@Getter
public class SoulFireServer implements EventBusOwner<SoulFireGlobalEvent> {
  public static final ComponentFlattener FLATTENER =
    ComponentFlattener.basic().toBuilder()
      .mapper(TranslatableComponent.class, TranslationMapper.INSTANCE)
      .build();
  public static final PlainTextComponentSerializer PLAIN_MESSAGE_SERIALIZER =
    PlainTextComponentSerializer.builder().flattener(FLATTENER).build();

  private final Injector injector =
    new InjectorBuilder().addDefaultHandlers("com.soulfiremc").create();
  private final SoulFireScheduler scheduler = new SoulFireScheduler(log);
  private final Map<String, String> serviceServerConfig = new HashMap<>();
  private final Map<UUID, InstanceManager> instances = Collections.synchronizedMap(new HashMap<>());
  private final RPCServer rpcServer;
  private final ServerSettingsRegistry serverSettingsRegistry;
  private final ServerSettingsRegistry instanceSettingsRegistry;
  private final SecretKey jwtSecretKey;
  private final PluginManager pluginManager;
  private final ShutdownManager shutdownManager;
  private final Path baseDirectory;
  private final LambdaManager eventBus =
    LambdaManager.basic(new ASMGenerator())
      .setExceptionHandler(EventExceptionHandler.INSTANCE)
      .setEventFilter(
        (c, h) -> {
          if (SoulFireGlobalEvent.class.isAssignableFrom(c)) {
            return true;
          } else {
            throw new IllegalStateException("This event handler only accepts global events");
          }
        });

  public SoulFireServer(
    String host,
    int port,
    PluginManager pluginManager,
    Instant startTime,
    AuthSystem authSystem,
    Path baseDirectory) {
    this.pluginManager = pluginManager;
    this.shutdownManager = new ShutdownManager(this::shutdownHook, pluginManager);
    this.baseDirectory = baseDirectory;

    // Register into injector
    injector.register(SoulFireServer.class, this);

    injector.register(ShutdownManager.class, shutdownManager);

    this.jwtSecretKey = KeyHelper.getOrCreateJWTSecretKey(SFPathConstants.getSecretKeyFile(baseDirectory));

    rpcServer = new RPCServer(host, port, injector, jwtSecretKey, authSystem);
    var rpcServerStart =
      CompletableFuture.runAsync(
        () -> {
          try {
            rpcServer.start();
          } catch (IOException e) {
            throw new CompletionException(e);
          }
        });

    log.info("Starting SoulFire v{}...", BuildData.VERSION);

    var configDirectory = SFPathConstants.getConfigDirectory(baseDirectory);
    var viaStart =
      CompletableFuture.runAsync(
        () -> {
          // Init via
          var platform = new SFViaPlatform(configDirectory.resolve("ViaVersion"));

          Via.init(
            ViaManagerImpl.builder()
              .platform(platform)
              .injector(platform.injector())
              .loader(new SFViaLoader())
              .build());

          platform.init();

          // For ViaLegacy
          Via.getManager().getProtocolManager().setMaxProtocolPathSize(Integer.MAX_VALUE);
          Via.getManager().getProtocolManager().setMaxPathDeltaIncrease(-1);
          ((ProtocolManagerImpl) Via.getManager().getProtocolManager()).refreshVersions();

          Via.getManager()
            .addEnableListener(
              () -> {
                new SFViaRewind(configDirectory.resolve("ViaRewind")).init();
                new SFViaBackwards(configDirectory.resolve("ViaBackwards")).init();
                new SFViaAprilFools(configDirectory.resolve("ViaAprilFools")).init();
                new SFViaLegacy(configDirectory.resolve("ViaLegacy")).init();
                new SFViaBedrock(configDirectory.resolve("ViaBedrock")).init();
              });

          var manager = (ViaManagerImpl) Via.getManager();
          manager.init();

          manager.getPlatform().getConf().setCheckForUpdates(false);

          manager.onServerLoaded();
        });
    var sparkStart =
      CompletableFuture.runAsync(
        () -> {
          var sparkPlugin = new SFSparkPlugin(configDirectory.resolve("spark"), this);
          sparkPlugin.init();
        });

    var newVersion = new AtomicReference<String>();
    var updateCheck =
      CompletableFuture.runAsync(
        () -> {
          log.info("Checking for updates...");
          newVersion.set(SFUpdateChecker.getInstance().join().getUpdateVersion().orElse(null));
        });

    CompletableFuture.allOf(rpcServerStart, viaStart, sparkStart, updateCheck).join();

    if (newVersion.get() != null) {
      log.warn(
        "SoulFire is outdated! Current version: {}, latest version: {}",
        BuildData.VERSION,
        newVersion.get());
    } else {
      log.info("SoulFire is up to date!");
    }

    for (var serverExtension : SoulFireAPI.getServerExtensions()) {
      serverExtension.onServer(this);
    }

    eventBus.call(
      new ServerSettingsRegistryInitEvent(
        serverSettingsRegistry =
          new ServerSettingsRegistry(SettingsPage.Type.SERVER)
            .addClass(DevSettings.class, "Dev Settings")));
    eventBus.call(
      new InstanceSettingsRegistryInitEvent(
        instanceSettingsRegistry =
          new ServerSettingsRegistry(SettingsPage.Type.INSTANCE)
            // Needs Via loaded to have all protocol versions
            .addClass(BotSettings.class, "Bot Settings")
            .addClass(AccountSettings.class, "Account Settings")
            .addClass(ProxySettings.class, "Proxy Settings")));

    log.info(
      "Finished loading! (Took {}ms)", Duration.between(startTime, Instant.now()).toMillis());
  }

  public static void setupLoggingAndVia(SettingsHolder settingsHolder) {
    Via.getManager().debugHandler().setEnabled(settingsHolder.get(DevSettings.VIA_DEBUG));
    setupLogging(settingsHolder);
  }

  public static void setupLogging(SettingsHolder settingsHolder) {
    var level = settingsHolder.get(DevSettings.CORE_DEBUG) ? Level.DEBUG : Level.INFO;
    var nettyLevel = settingsHolder.get(DevSettings.NETTY_DEBUG) ? Level.DEBUG : Level.INFO;
    var grpcLevel = settingsHolder.get(DevSettings.GRPC_DEBUG) ? Level.DEBUG : Level.INFO;
    Configurator.setRootLevel(level);
    Configurator.setLevel("io.netty", nettyLevel);
    Configurator.setLevel("io.grpc", grpcLevel);
  }

  public String generateRemoteUserJWT() {
    return generateJWT("remote-user");
  }

  public String generateIntegratedUserJWT() {
    return generateJWT("integrated-user");
  }

  private String generateJWT(String subject) {
    return Jwts.builder()
      .subject(subject)
      .issuedAt(Date.from(Instant.now()))
      .signWith(jwtSecretKey, Jwts.SIG.HS256)
      .compact();
  }

  private void shutdownHook() {
    // Shutdown the attacks if there is any
    stopAllAttacksSessions().join();

    // Shutdown scheduled tasks
    scheduler.shutdown();

    // Shut down RPC
    try {
      rpcServer.shutdown();
    } catch (InterruptedException e) {
      log.error("Failed to stop RPC server", e);
    }
  }

  public UUID createInstance(String friendlyName) {
    var attackManager = new InstanceManager(UUID.randomUUID(), friendlyName, this, SettingsHolder.EMPTY);
    eventBus.call(new InstanceInitEvent(attackManager));

    instances.put(attackManager.id(), attackManager);

    log.debug("Created instance with id {}", attackManager.id());

    return attackManager.id();
  }

  public CompletableFuture<?> stopAllAttacksSessions() {
    return CompletableFuture.allOf(
      Set.copyOf(instances.values()).stream()
        .map(InstanceManager::stopAttackSession)
        .toArray(CompletableFuture[]::new));
  }

  public CompletableFuture<?> stopAttack(UUID id) {
    return instances.get(id).stopAttackPermanently();
  }

  public CompletableFuture<?> deleteInstance(UUID id) {
    return instances.remove(id).stopAttackPermanently();
  }

  public InstanceManager getInstance(UUID id) {
    return instances.get(id);
  }
}
