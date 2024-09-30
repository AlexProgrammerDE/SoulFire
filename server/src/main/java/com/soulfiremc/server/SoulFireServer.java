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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import com.soulfiremc.server.settings.lib.SettingsImpl;
import com.soulfiremc.server.settings.lib.SettingsSource;
import com.soulfiremc.server.spark.SFSparkPlugin;
import com.soulfiremc.server.user.AuthSystem;
import com.soulfiremc.server.util.SFHelpers;
import com.soulfiremc.server.util.SFUpdateChecker;
import com.soulfiremc.server.util.TimeUtil;
import com.soulfiremc.server.viaversion.SFVLLoaderImpl;
import com.soulfiremc.server.viaversion.SFViaPlatform;
import com.soulfiremc.util.GsonInstance;
import com.soulfiremc.util.KeyHelper;
import com.soulfiremc.util.SFPathConstants;
import com.soulfiremc.util.ShutdownManager;
import com.viaversion.viaversion.api.Via;
import io.jsonwebtoken.Jwts;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.lenni0451.lambdaevents.LambdaManager;
import net.lenni0451.lambdaevents.generator.ASMGenerator;
import net.raphimc.vialoader.ViaLoader;
import net.raphimc.vialoader.impl.platform.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.pf4j.PluginManager;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * The main class of the SoulFire server.
 * A SoulFire server can hold and manage multiple instances.
 * It also provides an RPC server to communicate with the SoulFire client.
 * More than one SoulFireServer can be running at the same time, but on different ports.
 * Usually there is only one SoulFireServer running on a jvm, but it is possible to run multiple.
 */
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
  private final Map<UUID, InstanceManager> instances = new ConcurrentHashMap<>();
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
    this.rpcServer = new RPCServer(host, port, injector, jwtSecretKey, authSystem);

    log.info("Starting SoulFire v{}...", BuildData.VERSION);

    var configDirectory = SFPathConstants.getConfigDirectory(baseDirectory);
    var viaStart =
      CompletableFuture.runAsync(
        () -> {
          ViaLoader.init(
            new SFViaPlatform(configDirectory.resolve("ViaVersion")),
            new SFVLLoaderImpl(),
            null,
            null,
            ViaBackwardsPlatformImpl::new,
            ViaRewindPlatformImpl::new,
            ViaLegacyPlatformImpl::new,
            ViaAprilFoolsPlatformImpl::new,
            ViaBedrockPlatformImpl::new
          );

          TimeUtil.waitCondition(SFHelpers.not(Via.getManager().getProtocolManager()::hasLoadedMappings));
        }, scheduler);
    var sparkStart =
      CompletableFuture.runAsync(
        () -> {
          var sparkPlugin = new SFSparkPlugin(configDirectory.resolve("spark"), this);
          sparkPlugin.init();
        }, scheduler);

    var updateCheck =
      CompletableFuture.supplyAsync(
        () -> {
          log.info("Checking for updates...");
          return SFUpdateChecker.getInstance(this).join().getUpdateVersion().orElse(null);
        }, scheduler);

    CompletableFuture.allOf(viaStart, sparkStart, updateCheck).join();

    var newVersion = updateCheck.join();
    if (newVersion != null) {
      log.warn(
        "SoulFire is outdated! Current version: {}, latest version: {}",
        BuildData.VERSION,
        newVersion);
    } else {
      log.info("SoulFire is up to date!");
    }

    for (var serverExtension : SoulFireAPI.getServerExtensions()) {
      serverExtension.onServer(this);
    }

    postEvent(
      new ServerSettingsRegistryInitEvent(
        serverSettingsRegistry =
          new ServerSettingsRegistry(SettingsPage.Type.SERVER)
            .addClass(DevSettings.class, "Dev Settings", "bug")));
    postEvent(
      new InstanceSettingsRegistryInitEvent(
        instanceSettingsRegistry =
          new ServerSettingsRegistry(SettingsPage.Type.INSTANCE)
            // Needs Via loaded to have all protocol versions
            .addClass(BotSettings.class, "Bot Settings", "bot")
            .addClass(AccountSettings.class, "Account Settings", "users")
            .addClass(ProxySettings.class, "Proxy Settings", "waypoints")));

    log.info("Loading instances...");
    loadInstances();

    log.info("Starting scheduled tasks...");
    scheduler.scheduleWithFixedDelay(this::saveInstances, 0, 500, TimeUnit.MILLISECONDS);

    var rpcServerStart =
      CompletableFuture.runAsync(
        () -> {
          try {
            rpcServer.start();
          } catch (IOException e) {
            throw new CompletionException(e);
          }
        });

    rpcServerStart.join();

    log.info(
      "Finished loading! (Took {}ms)", Duration.between(startTime, Instant.now()).toMillis());
  }

  public static void setupLoggingAndVia(SettingsSource settingsSource) {
    Via.getManager().debugHandler().setEnabled(settingsSource.get(DevSettings.VIA_DEBUG));
    setupLogging(settingsSource);
  }

  public static void setupLogging(SettingsSource settingsSource) {
    Configurator.setRootLevel(settingsSource.get(DevSettings.CORE_DEBUG) ? Level.DEBUG : Level.INFO);
    Configurator.setLevel("io.netty", settingsSource.get(DevSettings.NETTY_DEBUG) ? Level.DEBUG : Level.INFO);
    Configurator.setLevel("io.grpc", settingsSource.get(DevSettings.GRPC_DEBUG) ? Level.DEBUG : Level.INFO);
    Configurator.setLevel("org.geysermc.mcprotocollib", settingsSource.get(DevSettings.MCPROTOCOLLIB_DEBUG) ? Level.DEBUG : Level.INFO);
  }

  private void loadInstances() {
    var instancesFile = SFPathConstants.getStateDirectory(baseDirectory).resolve("instances.json");
    if (!Files.exists(instancesFile)) {
      return;
    }

    try {
      var instancesJson = Files.readString(instancesFile);
      var instancesArray = GsonInstance.GSON.fromJson(instancesJson, JsonObject[].class);
      for (var instanceData : instancesArray) {
        try {
          var instance = InstanceManager.fromJson(this, instanceData);
          postEvent(new InstanceInitEvent(instance));

          instances.put(instance.id(), instance);

          log.info("Restored instance with id {}", instance.id());
        } catch (Exception e) {
          log.error("Failed to load existing instance", e);
        }
      }
    } catch (Exception e) {
      log.error("Failed to load existing instances", e);
    }
  }

  private void saveInstances() {
    var instancesFile = SFPathConstants.getStateDirectory(baseDirectory).resolve("instances.json");
    try {
      var instancesJson =
        GsonInstance.GSON.toJson(
          instances.values().stream().map(InstanceManager::toJson).toArray(JsonElement[]::new));
      SFHelpers.writeIfNeeded(instancesFile, instancesJson);
    } catch (Exception e) {
      log.error("Failed to save instances", e);
    }
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
    // Shut down RPC
    try {
      rpcServer.shutdown();
    } catch (InterruptedException e) {
      log.error("Failed to stop RPC server", e);
    }

    // Shutdown the attacks if there is any
    shutdownInstances().join();

    // Shutdown scheduled tasks
    scheduler.shutdown();
  }

  public UUID createInstance(String friendlyName) {
    var attackManager = new InstanceManager(this, UUID.randomUUID(), friendlyName, SettingsImpl.EMPTY);
    postEvent(new InstanceInitEvent(attackManager));

    instances.put(attackManager.id(), attackManager);

    log.debug("Created instance with id {}", attackManager.id());

    return attackManager.id();
  }

  public CompletableFuture<?> shutdownInstances() {
    return CompletableFuture.allOf(instances.values().stream()
        .map(InstanceManager::shutdownHook)
        .toArray(CompletableFuture[]::new));
  }

  public Optional<CompletableFuture<?>> deleteInstance(UUID id) {
    return Optional.ofNullable(instances.remove(id)).map(InstanceManager::deleteInstance);
  }

  public Optional<InstanceManager> getInstance(UUID id) {
    return Optional.ofNullable(instances.get(id));
  }
}
