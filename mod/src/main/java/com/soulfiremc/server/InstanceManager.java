/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server;

import com.soulfiremc.server.account.MCAuthService;
import com.soulfiremc.server.account.MinecraftAccount;
import com.soulfiremc.server.api.SessionLifecycle;
import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.api.event.lifecycle.InstanceSettingsRegistryInitEvent;
import com.soulfiremc.server.api.event.session.SessionBotRemoveEvent;
import com.soulfiremc.server.api.event.session.SessionEndedEvent;
import com.soulfiremc.server.api.event.session.SessionStartEvent;
import com.soulfiremc.server.api.event.session.SessionTickEvent;
import com.soulfiremc.server.api.metadata.MetadataHolder;
import com.soulfiremc.server.bot.BotConnection;
import com.soulfiremc.server.bot.BotConnectionFactory;
import com.soulfiremc.server.database.InstanceAuditLogEntity;
import com.soulfiremc.server.database.InstanceEntity;
import com.soulfiremc.server.database.ScriptEntity;
import com.soulfiremc.server.database.UserEntity;
import com.soulfiremc.server.proxy.SFProxy;
import com.soulfiremc.server.script.ScriptManager;
import com.soulfiremc.server.settings.instance.*;
import com.soulfiremc.server.settings.lib.*;
import com.soulfiremc.server.user.SoulFireUser;
import com.soulfiremc.server.util.MathHelper;
import com.soulfiremc.server.util.SFHelpers;
import com.soulfiremc.server.util.TimeUtil;
import com.soulfiremc.server.util.netty.NettyHelper;
import com.soulfiremc.server.util.structs.CachedLazyObject;
import com.soulfiremc.shared.SFLogAppender;
import io.netty.channel.EventLoopGroup;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.SessionFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/// Represents a single instance.
/// An instance persists settings over restarts and manages bot sessions and session state.
@Slf4j
@Getter
public final class InstanceManager {
  public static final ThreadLocal<InstanceManager> CURRENT = new InheritableThreadLocal<>();
  private final Map<UUID, BotConnection> botConnections = new ConcurrentHashMap<>();
  private final MetadataHolder metadata = new MetadataHolder();
  private final ScriptManager scriptManager;
  private final UUID id;
  private final SoulFireScheduler scheduler;
  private final InstanceSettingsDelegate settingsSource;
  private final SoulFireServer soulFireServer;
  private final SessionFactory sessionFactory;
  private final SoulFireScheduler.RunnableWrapper runnableWrapper;
  private final CachedLazyObject<String> friendlyNameCache;
  private final SettingsPageRegistry instanceSettingsPageRegistry;
  private final AtomicBoolean allBotsConnected = new AtomicBoolean(false);
  private SessionLifecycle sessionLifecycle = SessionLifecycle.STOPPED;

  public InstanceManager(SoulFireServer soulFireServer, SessionFactory sessionFactory, UUID id, SessionLifecycle lastState) {
    this.id = id;
    this.runnableWrapper = soulFireServer.runnableWrapper().with(new InstanceRunnableWrapper(this));
    this.scheduler = new SoulFireScheduler(runnableWrapper);
    this.soulFireServer = soulFireServer;
    this.sessionFactory = sessionFactory;
    this.settingsSource = new InstanceSettingsDelegate(new CachedLazyObject<>(this::fetchSettingsSource, 1, TimeUnit.SECONDS));
    this.friendlyNameCache = new CachedLazyObject<>(this::fetchFriendlyName, 1, TimeUnit.SECONDS);
    this.scriptManager = new ScriptManager(this);

    try {
      Files.createDirectories(getInstanceObjectStoragePath());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    try {
      for (var script : sessionFactory.fromTransaction(session -> {
        var instance = session.find(InstanceEntity.class, id);
        if (instance == null) {
          return Collections.<ScriptEntity>emptyList();
        }

        return session.createQuery("FROM ScriptEntity WHERE instance IS NULL OR instance = :instance", ScriptEntity.class)
          .setParameter("instance", instance)
          .list();
      })) {
        scriptManager.registerScript(script);
      }
    } catch (Throwable t) {
      log.error("Error while loading scripts for instance {}", id, t);
    }

    this.instanceSettingsPageRegistry = scheduler.supplyAsync(() -> {
      var registry = new SettingsPageRegistry()
        // Needs Via loaded to have all protocol versions
        .addInternalPage(BotSettings.class, "bot", "Bot Settings", "bot")
        .addInternalPage(AccountSettings.class, "account", "Account Settings", "users")
        .addInternalPage(ProxySettings.class, "proxy", "Proxy Settings", "waypoints")
        .addInternalPage(AISettings.class, "ai", "AI Settings", "sparkles")
        .addInternalPage(PathfindingSettings.class, "pathfinding", "Pathfinding Settings", "route");

      SoulFireAPI.postEvent(new InstanceSettingsRegistryInitEvent(this, registry));

      return registry;
    }).join();

    this.scheduler.scheduleWithFixedDelay(this::tick, 0, 500, TimeUnit.MILLISECONDS);
    this.scheduler.scheduleWithFixedDelay(this::refreshExpiredAccounts, 0, 1, TimeUnit.HOURS);

    // Resync stopped state to DB
    this.sessionLifecycle(SessionLifecycle.STOPPED);

    if (settingsSource.get(BotSettings.RESTORE_ON_REBOOT)) {
      switchToState(null, lastState);
    }
  }

  private InstanceSettingsSource fetchSettingsSource() {
    return sessionFactory.fromTransaction(session -> {
      var instance = session.find(InstanceEntity.class, id);

      if (instance == null) {
        throw new IllegalStateException("Instance not found");
      } else {
        return new InstanceSettingsImpl(instance.settings(), soulFireServer.settingsSource());
      }
    });
  }

  private String fetchFriendlyName() {
    return sessionFactory.fromTransaction(session -> {
      var instance = session.find(InstanceEntity.class, id);

      if (instance == null) {
        return "Unknown";
      } else {
        return instance.friendlyName();
      }
    });
  }

  private static Optional<SFProxy> getProxy(List<ProxyData> proxies) {
    if (proxies.isEmpty()) {
      return Optional.empty();
    }

    var selectedProxy =
      proxies.stream()
        .filter(ProxyData::isAvailable)
        .min(Comparator.comparingInt(ProxyData::usedBots))
        .orElseThrow(
          () -> new IllegalStateException("No proxies available!"));

    selectedProxy.useCount().incrementAndGet();

    return Optional.of(selectedProxy.proxy());
  }

  private void tick() {
    if (sessionLifecycle().isTicking()) {
      SoulFireAPI.postEvent(new SessionTickEvent(this));
    }

    evictBots();
  }

  private void evictBots() {
    // Remove botConnections from the map that are closed
    botConnections.entrySet().removeIf(entry -> {
      var bot = entry.getValue();
      if (bot.isDisconnected()) {
        log.debug("Removing bot {}", bot.accountName());
        SoulFireAPI.postEvent(new SessionBotRemoveEvent(this, bot));
        return true;
      }
      return false;
    });
  }

  private void refreshExpiredAccounts() {
    if (settingsSource.accounts().isEmpty()) {
      // Nothing to refresh
      return;
    }

    var accounts = new ArrayList<MinecraftAccount>();
    var refreshed = 0;
    for (var account : settingsSource.accounts().values()) {
      var authService = MCAuthService.convertService(account.authType());
      if (authService.isExpired(account)) {
        if (refreshed == 0) {
          log.info("Refreshing expired accounts");
        }

        accounts.add(authService.refresh(
          account,
          settingsSource.get(AccountSettings.USE_PROXIES_FOR_ACCOUNT_AUTH)
            ? SFHelpers.getRandomEntry(settingsSource.proxies()) : null,
          scheduler
        ).join());
        refreshed++;
      } else {
        accounts.add(account);
      }
    }

    if (refreshed > 0) {
      log.info("Refreshed {} accounts", refreshed);
      sessionFactory.inTransaction(session -> {
        var instanceEntity = session.find(InstanceEntity.class, id);

        instanceEntity.settings(instanceEntity.settings().withAccounts(accounts));

        session.merge(instanceEntity);
      });
    }
  }

  private MinecraftAccount refreshAccount(MinecraftAccount account) {
    var authService = MCAuthService.convertService(account.authType());
    if (!authService.isExpired(account)) {
      return account;
    }

    log.info("Account {} is expired, refreshing before connecting", account.lastKnownName());
    var refreshedAccount = authService.refresh(
      account,
      settingsSource.get(AccountSettings.USE_PROXIES_FOR_ACCOUNT_AUTH)
        ? SFHelpers.getRandomEntry(settingsSource.proxies()) : null,
      scheduler
    ).join();
    var accounts = new ArrayList<>(settingsSource.accounts().values());
    accounts.replaceAll(a -> a.authType().equals(refreshedAccount.authType())
      && a.profileId().equals(refreshedAccount.profileId()) ? refreshedAccount : a);
    sessionFactory.inTransaction(session -> {
      var instanceEntity = session.find(InstanceEntity.class, id);

      instanceEntity.settings(instanceEntity.settings().withAccounts(accounts));

      session.merge(instanceEntity);
    });

    return refreshedAccount;
  }

  public CompletableFuture<?> switchToState(@Nullable SoulFireUser initiator, SessionLifecycle targetState) {
    return switch (targetState) {
      case STARTING, RUNNING -> switch (sessionLifecycle()) {
        case STARTING, RUNNING, STOPPING -> CompletableFuture.completedFuture(null);
        case PAUSED -> {
          if (initiator != null) {
            addAuditLog(initiator, InstanceAuditLogEntity.AuditLogType.RESUME_SESSION, null);
          }

          this.sessionLifecycle(allBotsConnected.get() ? SessionLifecycle.RUNNING : SessionLifecycle.STARTING);
          yield CompletableFuture.completedFuture(null);
        }
        case STOPPED -> {
          if (initiator != null) {
            addAuditLog(initiator, InstanceAuditLogEntity.AuditLogType.START_SESSION, null);
          }

          yield scheduler.runAsync(this::start);
        }
      };
      case PAUSED -> switch (sessionLifecycle()) {
        case STARTING, RUNNING -> {
          if (initiator != null) {
            addAuditLog(initiator, InstanceAuditLogEntity.AuditLogType.PAUSE_SESSION, null);
          }

          this.sessionLifecycle(SessionLifecycle.PAUSED);
          yield CompletableFuture.completedFuture(null);
        }
        case STOPPING, PAUSED -> CompletableFuture.completedFuture(null);
        case STOPPED -> {
          if (initiator != null) {
            addAuditLog(initiator, InstanceAuditLogEntity.AuditLogType.START_SESSION, null);
            addAuditLog(initiator, InstanceAuditLogEntity.AuditLogType.PAUSE_SESSION, null);
          }

          yield scheduler.runAsync(this::start)
            .thenRunAsync(() -> this.sessionLifecycle(SessionLifecycle.PAUSED), scheduler);
        }
      };
      case STOPPING, STOPPED -> switch (sessionLifecycle()) {
        case STARTING, RUNNING, PAUSED -> {
          if (initiator != null) {
            addAuditLog(initiator, InstanceAuditLogEntity.AuditLogType.STOP_SESSION, null);
          }
          yield stopSessionPermanently();
        }
        case STOPPING, STOPPED -> CompletableFuture.completedFuture(null);
      };
    };
  }

  private void start() {
    if (!sessionLifecycle().isFullyStopped()) {
      throw new IllegalStateException("Another session is still running");
    }

    allBotsConnected.set(false);
    this.sessionLifecycle(SessionLifecycle.STARTING);

    log.info("Preparing bot session");

    var botAmount = settingsSource.get(BotSettings.AMOUNT); // How many bots to connect
    var botsPerProxy =
      settingsSource.get(ProxySettings.BOTS_PER_PROXY); // How many bots per proxy are allowed

    var proxies = new ArrayList<>(settingsSource.proxies()
      .stream()
      .map(p -> new ProxyData(p, botsPerProxy, new AtomicInteger(0)))
      .toList());
    {
      var availableProxies = proxies.size();
      if (availableProxies == 0) {
        log.info("No proxies provided, session will be performed without proxies");
      } else {
        var maxBots = MathHelper.sumCapOverflow(proxies.stream().mapToInt(ProxyData::availableBots));
        if (botAmount > maxBots) {
          log.warn("You have requested {} bots, but only {} are possible with the current amount of proxies.", botAmount, maxBots);
          log.warn("Continuing with {} bots due to proxies.", maxBots);
          botAmount = maxBots;
        }

        if (settingsSource.get(ProxySettings.SHUFFLE_PROXIES)) {
          Collections.shuffle(proxies);
        }
      }
    }

    var accountQueue = new ArrayBlockingQueue<MinecraftAccount>(botAmount);
    {
      var accounts = new ArrayList<>(settingsSource.accounts().values());
      var availableAccounts = accounts.size();
      if (availableAccounts == 0) {
        throw new IllegalStateException("No accounts configured. Please import accounts before starting a session.");
      }

      if (botAmount > availableAccounts) {
        throw new IllegalStateException(
          "Not enough accounts. You requested %d bots but only have %d accounts imported.".formatted(botAmount, availableAccounts));
      }

      if (settingsSource.get(AccountSettings.SHUFFLE_ACCOUNTS)) {
        Collections.shuffle(accounts);
      }

      accountQueue.addAll(accounts.subList(0, botAmount));
    }

    // Prepare an event loop group for the session
    var sessionEventLoopGroup =
      NettyHelper.createEventLoopGroup("Session-%s".formatted(id), runnableWrapper);

    var protocolVersion = settingsSource.get(BotSettings.PROTOCOL_VERSION, BotSettings.PROTOCOL_VERSION_PARSER);
    var serverAddress = BotConnectionFactory.parseAddress(settingsSource.get(BotSettings.ADDRESS), protocolVersion);

    var factories = new ArrayBlockingQueue<BotConnectionFactory>(botAmount);
    while (!accountQueue.isEmpty()) {
      var minecraftAccount = refreshAccount(accountQueue.poll());
      var proxyData = getProxy(proxies).orElse(null);
      factories.add(
        new BotConnectionFactory(
          this,
          new BotSettingsDelegate(new CachedLazyObject<>(() -> {
            var fetchedSettingsSource = this.settingsSource();
            var fetchedAccount = fetchedSettingsSource.accounts().get(minecraftAccount.profileId());
            return new BotSettingsImpl(fetchedAccount == null || fetchedAccount.settingsStem() == null
              ? BotSettingsImpl.Stem.EMPTY
              : fetchedAccount.settingsStem(), fetchedSettingsSource);
          }, 1, TimeUnit.SECONDS)),
          minecraftAccount,
          protocolVersion,
          serverAddress,
          proxyData,
          sessionEventLoopGroup
        ));
    }

    var usedProxies = proxies.stream().filter(ProxyData::hasBots).count();
    if (usedProxies == 0) {
      log.info("Starting session with {} bots", factories.size());
    } else {
      log.info("Starting session with {} bots and {} active proxies", factories.size(), usedProxies);
    }

    SoulFireAPI.postEvent(new SessionStartEvent(this));

    var connectSemaphore = new Semaphore(settingsSource.get(BotSettings.CONCURRENT_CONNECTS));
    scheduler.schedule(
      () -> {
        allBotsConnected.set(false);
        while (!factories.isEmpty()) {
          var factory = factories.poll();
          if (factory == null) {
            break;
          }

          try {
            connectSemaphore.acquire();
          } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            break;
          }

          log.debug("Scheduling bot {}", factory.minecraftAccount().lastKnownName());
          scheduler.schedule(
            SoulFireScheduler.FinalizableRunnable.withFinalizer(() -> {
              if (sessionLifecycle().isStoppedOrStopping()) {
                return;
              }

              TimeUtil.waitCondition(() -> sessionLifecycle().isPaused());

              log.debug("Connecting bot {}", factory.minecraftAccount().lastKnownName());
              var botConnection = factory.prepareConnection(false);
              storeNewBot(botConnection);

              try {
                botConnection.connect().get();
              } catch (Throwable e) {
                log.error("Error while connecting", e);
              }
            }, connectSemaphore::release));

          TimeUtil.waitTime(
            settingsSource.getRandom(BotSettings.JOIN_DELAY).getAsLong(),
            TimeUnit.MILLISECONDS);
        }

        allBotsConnected.set(true);
        if (this.sessionLifecycle() == SessionLifecycle.STARTING) {
          this.sessionLifecycle(SessionLifecycle.RUNNING);
        }
      });
  }

  public CompletableFuture<?> deleteInstance() {
    return stopSessionPermanently()
      .thenRunAsync(scriptManager::destroyManager, scheduler)
      .thenRunAsync(scheduler::shutdown, soulFireServer.scheduler())
      .thenRunAsync(() -> {
        try {
          SFHelpers.deleteDirectory(getInstanceObjectStoragePath());
        } catch (IOException e) {
          log.error("Error while deleting instance storage", e);
        }
      }, soulFireServer.scheduler());
  }

  public CompletableFuture<?> shutdownHook() {
    return stopSession()
      .thenRunAsync(scriptManager::destroyManager, scheduler)
      .thenRunAsync(scheduler::shutdown, soulFireServer.scheduler());
  }

  public CompletableFuture<?> stopSessionPermanently() {
    if (sessionLifecycle().isStoppedOrStopping()) {
      return CompletableFuture.completedFuture(null);
    }

    log.info("Stopping bot session");
    this.sessionLifecycle(SessionLifecycle.STOPPING);

    return this.stopSession()
      .thenRunAsync(() -> {
        this.sessionLifecycle(SessionLifecycle.STOPPED);
        log.info("Session stopped");
      }, scheduler);
  }

  private void sessionLifecycle(SessionLifecycle sessionLifecycle) {
    this.sessionLifecycle = sessionLifecycle;
    sessionFactory.inTransaction(session -> {
      var instanceEntity = session.find(InstanceEntity.class, id);
      if (instanceEntity == null) {
        return;
      }

      instanceEntity.sessionLifecycle(sessionLifecycle);

      session.merge(instanceEntity);
    });
  }

  // Doesn't shut down properly unless #shutdown() is called
  // Not sure why, netty moment...
  @SuppressWarnings("deprecation")
  public CompletableFuture<?> stopSession() {
    return scheduler.runAsync(() -> {
      allBotsConnected.set(false);
      log.info("Disconnecting bots");
      do {
        var eventLoopGroups = new HashSet<EventLoopGroup>();
        var disconnectFuture = new ArrayList<CompletableFuture<?>>();
        botConnections.entrySet().removeIf(entry -> {
          var botConnection = entry.getValue();
          disconnectFuture.add(scheduler.runAsync(() -> botConnection.disconnect(Component.text("Session stopped"))));
          eventLoopGroups.add(botConnection.eventLoopGroup());
          return true;
        });

        log.info("Waiting for all bots to fully disconnect");
        for (var future : disconnectFuture) {
          try {
            future.get();
          } catch (InterruptedException | ExecutionException e) {
            log.error("Error while shutting down", e);
          }
        }

        log.info("Shutting down session event loop groups");
        for (var eventLoopGroup : eventLoopGroups) {
          try {
            eventLoopGroup.shutdown();
            eventLoopGroup.shutdownGracefully().get();
          } catch (InterruptedException | ExecutionException e) {
            log.error("Error while shutting down", e);
          }
        }
      } while (!botConnections.isEmpty()); // To make sure really all bots are disconnected

      // Notify plugins of state change
      SoulFireAPI.postEvent(new SessionEndedEvent(this));
    });
  }

  public void addAuditLog(SoulFireUser source, InstanceAuditLogEntity.AuditLogType logType, @Nullable String data) {
    scheduler.execute(() -> sessionFactory.inTransaction(session -> {
      var instanceEntity = session.find(InstanceEntity.class, id);
      if (instanceEntity == null) {
        return;
      }

      var userEntity = session.find(UserEntity.class, source.getUniqueId());
      if (userEntity == null) {
        return;
      }

      var auditLogEntry = new InstanceAuditLogEntity();
      auditLogEntry.type(logType);
      auditLogEntry.data(data);
      auditLogEntry.instance(instanceEntity);
      auditLogEntry.user(userEntity);

      session.persist(auditLogEntry);
    }));
  }

  public Path getInstanceObjectStoragePath() {
    return soulFireServer.getObjectStoragePath().resolve("instance-" + id.toString());
  }

  public Path getScriptDataPath(UUID id) {
    return getInstanceObjectStoragePath().resolve("script-data-" + id);
  }

  public List<BotConnection> getConnectedBots() {
    return new ArrayList<>(botConnections.values());
  }

  public void storeNewBot(BotConnection connection) {
    botConnections.put(connection.accountProfileId(), connection);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof InstanceManager that)) {
      return false;
    }

    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  private record ProxyData(SFProxy proxy, int maxBots, AtomicInteger useCount) {
    public boolean unlimited() {
      return maxBots == -1;
    }

    public int availableBots() {
      return unlimited() ? Integer.MAX_VALUE : maxBots - useCount.get();
    }

    public boolean isAvailable() {
      return availableBots() > 0;
    }

    public int usedBots() {
      return useCount.get();
    }

    public boolean hasBots() {
      return usedBots() > 0;
    }
  }

  private record InstanceRunnableWrapper(InstanceManager instanceManager) implements SoulFireScheduler.RunnableWrapper {
    @Override
    public Runnable wrap(Runnable runnable) {
      return () -> {
        try (
          var ignored1 = SFHelpers.smartThreadLocalCloseable(CURRENT, instanceManager);
          var ignored2 = SFHelpers.smartMDCCloseable(SFLogAppender.SF_INSTANCE_ID, instanceManager.id().toString());
          var ignored3 = SFHelpers.smartMDCCloseable(SFLogAppender.SF_INSTANCE_NAME, instanceManager.friendlyNameCache().get())) {
          runnable.run();
        }
      };
    }
  }
}
