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
import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.api.event.attack.InstanceInitEvent;
import com.soulfiremc.server.api.event.lifecycle.InstanceSettingsRegistryInitEvent;
import com.soulfiremc.server.api.event.lifecycle.ServerSettingsRegistryInitEvent;
import com.soulfiremc.server.api.metadata.MetadataHolder;
import com.soulfiremc.server.database.DatabaseManager;
import com.soulfiremc.server.database.InstanceEntity;
import com.soulfiremc.server.database.ServerConfigEntity;
import com.soulfiremc.server.database.UserEntity;
import com.soulfiremc.server.grpc.RPCServer;
import com.soulfiremc.server.settings.instance.AISettings;
import com.soulfiremc.server.settings.instance.AccountSettings;
import com.soulfiremc.server.settings.instance.BotSettings;
import com.soulfiremc.server.settings.instance.ProxySettings;
import com.soulfiremc.server.settings.lib.ServerSettingsDelegate;
import com.soulfiremc.server.settings.lib.ServerSettingsRegistry;
import com.soulfiremc.server.settings.server.DevSettings;
import com.soulfiremc.server.settings.server.ServerSettings;
import com.soulfiremc.server.spark.SFSparkPlugin;
import com.soulfiremc.server.user.*;
import com.soulfiremc.server.util.SFHelpers;
import com.soulfiremc.server.util.SFPathConstants;
import com.soulfiremc.server.util.TimeUtil;
import com.soulfiremc.server.util.structs.CachedLazyObject;
import com.soulfiremc.server.util.structs.SFUpdateChecker;
import com.soulfiremc.server.util.structs.ShutdownManager;
import com.soulfiremc.server.viaversion.SFVLLoaderImpl;
import com.soulfiremc.server.viaversion.SFViaPlatform;
import com.viaversion.vialoader.ViaLoader;
import com.viaversion.vialoader.impl.platform.*;
import com.viaversion.viaversion.api.Via;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.hibernate.SessionFactory;
import org.pf4j.PluginManager;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
public class SoulFireServer {
  public static final ThreadLocal<SoulFireServer> CURRENT = new ThreadLocal<>();

  private final Injector injector =
    new InjectorBuilder().addDefaultHandlers("com.soulfiremc").create();
  private final SoulFireScheduler.RunnableWrapper runnableWrapper = runnable -> () -> {
    CURRENT.set(this);
    try {
      runnable.run();
    } finally {
      CURRENT.remove();
    }
  };
  private final SoulFireScheduler scheduler = new SoulFireScheduler(log, runnableWrapper);
  private final Map<UUID, InstanceManager> instances = new ConcurrentHashMap<>();
  private final MetadataHolder metadata = new MetadataHolder();
  private final ServerSettingsDelegate settingsSource;
  private final RPCServer rpcServer;
  private final AuthSystem authSystem;
  private final ServerSettingsRegistry serverSettingsRegistry;
  private final ServerSettingsRegistry instanceSettingsRegistry;
  private final PluginManager pluginManager;
  private final ShutdownManager shutdownManager;
  private final SessionFactory sessionFactory;
  private final Path baseDirectory;

  public SoulFireServer(
    String host,
    int port,
    PluginManager pluginManager,
    Instant startTime,
    Path baseDirectory) {
    log.info("Starting SoulFire v{}...", BuildData.VERSION);

    this.pluginManager = pluginManager;
    this.shutdownManager = new ShutdownManager(this::shutdownHook, pluginManager);
    this.baseDirectory = baseDirectory;

    // Register into injector
    injector.register(SoulFireServer.class, this);

    injector.register(ShutdownManager.class, shutdownManager);

    this.sessionFactory = DatabaseManager.forSqlite(baseDirectory.resolve("soulfire.sqlite"));
    injector.register(SessionFactory.class, sessionFactory);

    this.settingsSource = new ServerSettingsDelegate(new CachedLazyObject<>(() ->
      sessionFactory.fromTransaction(session -> {
        var entity = session.find(ServerConfigEntity.class, 1);
        if (entity == null) {
          entity = new ServerConfigEntity();
          session.persist(entity);
        }

        return entity.settings();
      }), 1, TimeUnit.SECONDS));
    this.authSystem = new AuthSystem(this);
    this.rpcServer = new RPCServer(host, port, injector, authSystem);

    var configDirectory = SFPathConstants.getConfigDirectory(baseDirectory);
    var viaStart =
      scheduler.runAsync(
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
        });
    var sparkStart =
      scheduler.runAsync(
        () -> {
          var sparkPlugin = new SFSparkPlugin(configDirectory.resolve("spark"), this);
          sparkPlugin.init();
        });

    var updateCheck =
      CompletableFuture.supplyAsync(
        () -> {
          log.info("Checking for updates...");
          return SFUpdateChecker.getInstance(this).join().getUpdateVersion().orElse(null);
        }, scheduler);

    CompletableFuture.allOf(viaStart, sparkStart, updateCheck).join();

    // Via is ready, we can now set up all config stuff
    setupLoggingAndVia();

    var newVersion = updateCheck.join();
    if (newVersion != null) {
      log.warn(
        "SoulFire is outdated! Current version: {}, latest version: {}",
        BuildData.VERSION,
        newVersion);
    } else {
      log.info("SoulFire is up to date!");
    }

    SoulFireAPI.postEvent(
      new ServerSettingsRegistryInitEvent(
        this,
        serverSettingsRegistry =
          new ServerSettingsRegistry()
            .addInternalPage(ServerSettings.class, "Server Settings", "server")
            .addInternalPage(DevSettings.class, "Dev Settings", "bug")));
    SoulFireAPI.postEvent(
      new InstanceSettingsRegistryInitEvent(
        this,
        instanceSettingsRegistry =
          new ServerSettingsRegistry()
            // Needs Via loaded to have all protocol versions
            .addInternalPage(BotSettings.class, "Bot Settings", "bot")
            .addInternalPage(AccountSettings.class, "Account Settings", "users")
            .addInternalPage(ProxySettings.class, "Proxy Settings", "waypoints")
            .addInternalPage(AISettings.class, "AI Settings", "sparkles")));

    log.info("Loading instances...");
    loadInstances();

    var rpcServerStart =
      scheduler.runAsync(
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

  public EmailSender emailSender() {
    return switch (settingsSource.get(ServerSettings.EMAIL_TYPE, ServerSettings.EmailType.class)) {
      case CONSOLE -> injector.getSingleton(ConsoleEmailSender.class);
      case SMTP -> injector.getSingleton(SmtpEmailSender.class);
    };
  }

  public void configUpdateHook() {
    setupLoggingAndVia();
  }

  public void setupLoggingAndVia() {
    Via.getManager().debugHandler().setEnabled(settingsSource.get(DevSettings.VIA_DEBUG));

    Configurator.setRootLevel(settingsSource.get(DevSettings.CORE_DEBUG) ? Level.DEBUG : Level.INFO);
    Configurator.setLevel("io.netty", settingsSource.get(DevSettings.NETTY_DEBUG) ? Level.DEBUG : Level.INFO);
    Configurator.setLevel("io.grpc", settingsSource.get(DevSettings.GRPC_DEBUG) ? Level.DEBUG : Level.INFO);
    Configurator.setLevel("org.geysermc.mcprotocollib", settingsSource.get(DevSettings.MCPROTOCOLLIB_DEBUG) ? Level.DEBUG : Level.INFO);
  }

  private void loadInstances() {
    try {
      for (var instanceData : sessionFactory.fromTransaction(s ->
        s.createQuery("FROM InstanceEntity", InstanceEntity.class).list())) {
        try {
          var instance = new InstanceManager(this, sessionFactory, instanceData);
          SoulFireAPI.postEvent(new InstanceInitEvent(instance));

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

    // Shutdown database
    sessionFactory.close();
  }

  public UUID createInstance(String friendlyName, SoulFireUser owner) {
    var instanceEntity = sessionFactory.fromTransaction(s -> {
      var newInstanceEntity = new InstanceEntity();
      newInstanceEntity.friendlyName(friendlyName);
      newInstanceEntity.icon(InstanceEntity.randomInstanceIcon());
      newInstanceEntity.owner(s.find(UserEntity.class, owner.getUniqueId()));
      s.persist(newInstanceEntity);

      return newInstanceEntity;
    });
    var instanceManager = new InstanceManager(this, sessionFactory, instanceEntity);
    SoulFireAPI.postEvent(new InstanceInitEvent(instanceManager));

    instances.put(instanceManager.id(), instanceManager);

    log.debug("Created instance with id {}", instanceManager.id());

    return instanceManager.id();
  }

  public CompletableFuture<?> shutdownInstances() {
    return CompletableFuture.allOf(instances.values().stream()
      .map(InstanceManager::shutdownHook)
      .toArray(CompletableFuture[]::new));
  }

  public Optional<CompletableFuture<?>> deleteInstance(UUID id) {
    sessionFactory.inTransaction(s -> s.createMutationQuery("DELETE FROM InstanceEntity WHERE id = :id")
      .setParameter("id", id)
      .executeUpdate());

    return Optional.ofNullable(instances.remove(id)).map(InstanceManager::deleteInstance);
  }

  public Optional<InstanceManager> getInstance(UUID id) {
    return Optional.ofNullable(instances.get(id));
  }
}
