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
import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.api.event.attack.InstanceInitEvent;
import com.soulfiremc.server.api.event.lifecycle.InstanceSettingsRegistryInitEvent;
import com.soulfiremc.server.api.event.lifecycle.ServerSettingsRegistryInitEvent;
import com.soulfiremc.server.api.metadata.MetadataHolder;
import com.soulfiremc.server.data.TranslationMapper;
import com.soulfiremc.server.database.DatabaseManager;
import com.soulfiremc.server.database.InstanceEntity;
import com.soulfiremc.server.database.UserEntity;
import com.soulfiremc.server.grpc.RPCServer;
import com.soulfiremc.server.settings.*;
import com.soulfiremc.server.settings.lib.ServerSettingsRegistry;
import com.soulfiremc.server.settings.lib.SettingsSource;
import com.soulfiremc.server.spark.SFSparkPlugin;
import com.soulfiremc.server.user.AuthSystem;
import com.soulfiremc.server.user.SoulFireUser;
import com.soulfiremc.server.util.SFHelpers;
import com.soulfiremc.server.util.SFPathConstants;
import com.soulfiremc.server.util.SFUpdateChecker;
import com.soulfiremc.server.util.TimeUtil;
import com.soulfiremc.server.util.structs.ShutdownManager;
import com.soulfiremc.server.viaversion.SFVLLoaderImpl;
import com.soulfiremc.server.viaversion.SFViaPlatform;
import com.viaversion.viaversion.api.Via;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.raphimc.vialoader.ViaLoader;
import net.raphimc.vialoader.impl.platform.*;
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
  public static final ComponentFlattener FLATTENER =
    ComponentFlattener.basic().toBuilder()
      .mapper(TranslatableComponent.class, TranslationMapper.INSTANCE)
      .build();
  public static final PlainTextComponentSerializer PLAIN_MESSAGE_SERIALIZER =
    PlainTextComponentSerializer.builder().flattener(FLATTENER).build();

  private final Injector injector =
    new InjectorBuilder().addDefaultHandlers("com.soulfiremc").create();
  private final SoulFireScheduler scheduler = new SoulFireScheduler(log);
  private final Map<UUID, InstanceManager> instances = new ConcurrentHashMap<>();
  private final MetadataHolder metadata = new MetadataHolder();
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
    this.pluginManager = pluginManager;
    this.shutdownManager = new ShutdownManager(this::shutdownHook, pluginManager);
    this.baseDirectory = baseDirectory;

    // Register into injector
    injector.register(SoulFireServer.class, this);

    injector.register(ShutdownManager.class, shutdownManager);

    this.sessionFactory = DatabaseManager.forSqlite(baseDirectory.resolve("soulfire.sqlite"));
    this.authSystem = new AuthSystem(this);
    this.rpcServer = new RPCServer(host, port, injector, authSystem);

    log.info("Starting SoulFire v{}...", BuildData.VERSION);

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
          new ServerSettingsRegistry(SettingsPage.Type.SERVER)
            .addClass(DevSettings.class, "Dev Settings", "bug")));
    SoulFireAPI.postEvent(
      new InstanceSettingsRegistryInitEvent(
        this,
        instanceSettingsRegistry =
          new ServerSettingsRegistry(SettingsPage.Type.INSTANCE)
            // Needs Via loaded to have all protocol versions
            .addClass(BotSettings.class, "Bot Settings", "bot")
            .addClass(AccountSettings.class, "Account Settings", "users")
            .addClass(ProxySettings.class, "Proxy Settings", "waypoints")
            .addClass(AISettings.class, "AI Settings", "sparkles")));

    log.info("Loading instances...");
    loadInstances();

    log.info("Starting scheduled tasks...");
    scheduler.scheduleWithFixedDelay(this::saveInstances, 0, 1, TimeUnit.SECONDS);

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
    try {
      for (var instanceData : sessionFactory.fromTransaction(s ->
        s.createQuery("FROM InstanceEntity", InstanceEntity.class).getResultList())) {
        try {
          var instance = new InstanceManager(this, instanceData);
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

  private void saveInstances() {
    try {
      sessionFactory.inTransaction(s -> {
        instances.values().stream()
          .map(InstanceManager::instanceEntity)
          .forEach(s::merge);

        s.createMutationQuery("DELETE FROM InstanceEntity WHERE id NOT IN (:ids)")
          .setParameterList("ids", instances.keySet())
          .executeUpdate();
      });
    } catch (Exception e) {
      log.error("Failed to save instances", e);
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
      newInstanceEntity.owner(s.find(UserEntity.class, owner.getUniqueId()));
      s.persist(newInstanceEntity);

      return newInstanceEntity;
    });
    var instanceManager = new InstanceManager(this, instanceEntity);
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
    return Optional.ofNullable(instances.remove(id)).map(InstanceManager::deleteInstance);
  }

  public Optional<InstanceManager> getInstance(UUID id) {
    return Optional.ofNullable(instances.get(id));
  }
}
