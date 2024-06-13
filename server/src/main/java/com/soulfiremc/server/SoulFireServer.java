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
import com.soulfiremc.server.api.AttackState;
import com.soulfiremc.server.api.ServerPlugin;
import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.api.event.attack.AttackInitEvent;
import com.soulfiremc.server.api.event.lifecycle.SettingsRegistryInitEvent;
import com.soulfiremc.server.data.TranslationMapper;
import com.soulfiremc.server.grpc.RPCServer;
import com.soulfiremc.server.plugins.AutoArmor;
import com.soulfiremc.server.plugins.AutoEat;
import com.soulfiremc.server.plugins.AutoJump;
import com.soulfiremc.server.plugins.AutoReconnect;
import com.soulfiremc.server.plugins.AutoRegister;
import com.soulfiremc.server.plugins.AutoRespawn;
import com.soulfiremc.server.plugins.AutoTotem;
import com.soulfiremc.server.plugins.ChatControl;
import com.soulfiremc.server.plugins.ChatMessageLogger;
import com.soulfiremc.server.plugins.ClientBrand;
import com.soulfiremc.server.plugins.ClientSettings;
import com.soulfiremc.server.plugins.FakeVirtualHost;
import com.soulfiremc.server.plugins.ForwardingBypass;
import com.soulfiremc.server.plugins.InternalPlugin;
import com.soulfiremc.server.plugins.KillAura;
import com.soulfiremc.server.plugins.ModLoaderSupport;
import com.soulfiremc.server.plugins.POVServer;
import com.soulfiremc.server.plugins.ServerListBypass;
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
import com.soulfiremc.util.SFFeatureFlags;
import com.soulfiremc.util.SFPathConstants;
import com.soulfiremc.util.ShutdownManager;
import com.viaversion.viaversion.ViaManagerImpl;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.protocol.ProtocolManagerImpl;
import io.jsonwebtoken.Jwts;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.SecretKey;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.pf4j.PluginManager;

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
  @Getter
  private final ExecutorService threadPool = Executors.newCachedThreadPool();
  private final Map<String, String> serviceServerConfig = new HashMap<>();
  private final Int2ObjectMap<AttackManager> attacks =
    Int2ObjectMaps.synchronize(new Int2ObjectArrayMap<>());
  private final RPCServer rpcServer;
  private final ServerSettingsRegistry settingsRegistry;
  private final SecretKey jwtSecretKey;
  private final PluginManager pluginManager;
  private final ShutdownManager shutdownManager;
  private final Path baseDirectory;

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

    // Init API
    SoulFireAPI.setSoulFire(this);

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

    registerInternalServerExtensions();
    registerServerExtensions();

    for (var serverExtension : SoulFireAPI.getServerExtensions()) {
      serverExtension.onEnable(this);
    }

    SoulFireAPI.postEvent(
      new SettingsRegistryInitEvent(
        settingsRegistry =
          new ServerSettingsRegistry()
            // Needs Via loaded to have all protocol versions
            .addClass(BotSettings.class, "Bot Settings", true)
            .addClass(DevSettings.class, "Dev Settings", true)
            .addClass(AccountSettings.class, "Account Settings", true)
            .addClass(ProxySettings.class, "Proxy Settings", true)));

    log.info(
      "Finished loading! (Took {}ms)", Duration.between(startTime, Instant.now()).toMillis());
  }

  private static void registerInternalServerExtensions() {
    var plugins =
      new InternalPlugin[] {
        new ClientBrand(),
        new ClientSettings(),
        new ChatControl(),
        new AutoReconnect(),
        new AutoRegister(),
        new AutoRespawn(),
        new AutoTotem(),
        new AutoJump(),
        new AutoArmor(),
        new AutoEat(),
        new ChatMessageLogger(),
        new ServerListBypass(),
        new FakeVirtualHost(), // Needs to be before ModLoaderSupport to not break it
        SFFeatureFlags.MOD_SUPPORT
          ? new ModLoaderSupport()
          : null, // Needs to be before ForwardingBypass to not break it
        new ForwardingBypass(),
        new KillAura(),
        new POVServer()
      };

    for (var plugin : plugins) {
      if (plugin == null) {
        continue;
      }

      SoulFireAPI.registerServerExtension(plugin);
    }
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

  private void registerServerExtensions() {
    pluginManager.getExtensions(ServerPlugin.class).forEach(SoulFireAPI::registerServerExtension);
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
    stopAllAttacks().join();

    // Shutdown scheduled tasks
    threadPool.shutdown();

    // Shut down RPC
    try {
      rpcServer.shutdown();
    } catch (InterruptedException e) {
      log.error("Failed to stop RPC server", e);
    }
  }

  public int startAttack(SettingsHolder settingsHolder) {
    var attackManager = new AttackManager(this, settingsHolder);
    SoulFireAPI.postEvent(new AttackInitEvent(attackManager));

    attacks.put(attackManager.id(), attackManager);

    attackManager.start();

    log.debug("Started attack with id {}", attackManager.id());

    return attackManager.id();
  }

  public void toggleAttackState(int id, boolean pause) {
    attacks.get(id).attackState(pause ? AttackState.PAUSED : AttackState.RUNNING);
  }

  public CompletableFuture<?> stopAllAttacks() {
    return CompletableFuture.allOf(
      Set.copyOf(attacks.keySet()).stream()
        .map(this::stopAttack)
        .toArray(CompletableFuture[]::new));
  }

  public CompletableFuture<?> stopAttack(int id) {
    return attacks.remove(id).stop();
  }
}
