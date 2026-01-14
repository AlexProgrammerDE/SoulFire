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

import com.google.protobuf.Value;
import com.soulfiremc.server.account.MCAuthService;
import com.soulfiremc.server.account.MinecraftAccount;
import com.soulfiremc.server.account.OfflineAuthService;
import com.soulfiremc.server.api.AttackLifecycle;
import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.api.event.attack.AttackBotRemoveEvent;
import com.soulfiremc.server.api.event.attack.AttackEndedEvent;
import com.soulfiremc.server.api.event.attack.AttackStartEvent;
import com.soulfiremc.server.api.event.attack.AttackTickEvent;
import com.soulfiremc.server.api.event.lifecycle.InstanceSettingsRegistryInitEvent;
import com.soulfiremc.server.api.metadata.MetadataHolder;
import com.soulfiremc.server.bot.BotConnection;
import com.soulfiremc.server.bot.BotConnectionFactory;
import com.soulfiremc.server.bot.BotEntity;
import com.soulfiremc.server.database.InstanceAuditLogEntity;
import com.soulfiremc.server.database.InstanceEntity;
import com.soulfiremc.server.database.ScriptEntity;
import com.soulfiremc.server.database.UserEntity;
import com.soulfiremc.server.proxy.SFProxy;
import com.soulfiremc.server.script.ScriptManager;
import com.soulfiremc.server.settings.instance.*;
import com.soulfiremc.server.settings.lib.InstanceSettingsDelegate;
import com.soulfiremc.server.settings.lib.InstanceSettingsSource;
import com.soulfiremc.server.settings.lib.ServerSettingsRegistry;
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
/// An instance persists settings over restarts and managed attack session and attack state.
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
  private final ServerSettingsRegistry instanceSettingsRegistry;
  private final AtomicBoolean allBotsConnected = new AtomicBoolean(false);
  private AttackLifecycle attackLifecycle = AttackLifecycle.STOPPED;

  public InstanceManager(SoulFireServer soulFireServer, SessionFactory sessionFactory, UUID id, AttackLifecycle lastState) {
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

    this.instanceSettingsRegistry = scheduler.supplyAsync(() -> {
      var registry = new ServerSettingsRegistry()
        // Needs Via loaded to have all protocol versions
        .addInternalPage(BotSettings.class, "Bot Settings")
        .addInternalPage(AccountSettings.class, "Account Settings")
        .addInternalPage(ProxySettings.class, "Proxy Settings")
        .addInternalPage(AISettings.class, "AI Settings")
        .addInternalPage(PathfindingSettings.class, "Pathfinding Settings");

      SoulFireAPI.postEvent(new InstanceSettingsRegistryInitEvent(this, registry));

      return registry;
    }).join();

    this.scheduler.scheduleWithFixedDelay(this::tick, 0, 500, TimeUnit.MILLISECONDS);
    this.scheduler.scheduleWithFixedDelay(this::refreshExpiredAccounts, 0, 1, TimeUnit.HOURS);

    // Resync stopped state to DB
    this.attackLifecycle(AttackLifecycle.STOPPED);

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
        return instance.settings();
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

  private void tick() {
    if (attackLifecycle().isTicking()) {
      SoulFireAPI.postEvent(new AttackTickEvent(this));
    }

    evictBots();
  }

  private void evictBots() {
    // Remove botConnections from the map that are closed
    botConnections.entrySet().removeIf(entry -> {
      var bot = entry.getValue();
      if (bot.isDisconnected()) {
        log.debug("Removing bot {}", bot.accountName());
        SoulFireAPI.postEvent(new AttackBotRemoveEvent(this, bot));
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
    for (var account : settingsSource.accounts()) {
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
    var accounts = new ArrayList<>(settingsSource.accounts());
    accounts.replaceAll(a -> a.authType().equals(refreshedAccount.authType())
      && a.profileId().equals(refreshedAccount.profileId()) ? refreshedAccount : a);
    sessionFactory.inTransaction(session -> {
      var instanceEntity = session.find(InstanceEntity.class, id);

      instanceEntity.settings(instanceEntity.settings().withAccounts(accounts));

      session.merge(instanceEntity);
    });

    return refreshedAccount;
  }

  public CompletableFuture<?> switchToState(@Nullable SoulFireUser initiator, AttackLifecycle targetState) {
    return switch (targetState) {
      case STARTING, RUNNING -> switch (attackLifecycle()) {
        case STARTING, RUNNING, STOPPING -> CompletableFuture.completedFuture(null);
        case PAUSED -> {
          if (initiator != null) {
            addAuditLog(initiator, InstanceAuditLogEntity.AuditLogType.RESUME_ATTACK, null);
          }

          this.attackLifecycle(allBotsConnected.get() ? AttackLifecycle.RUNNING : AttackLifecycle.STARTING);
          yield CompletableFuture.completedFuture(null);
        }
        case STOPPED -> {
          if (initiator != null) {
            addAuditLog(initiator, InstanceAuditLogEntity.AuditLogType.START_ATTACK, null);
          }

          yield scheduler.runAsync(this::start);
        }
      };
      case PAUSED -> switch (attackLifecycle()) {
        case STARTING, RUNNING -> {
          if (initiator != null) {
            addAuditLog(initiator, InstanceAuditLogEntity.AuditLogType.PAUSE_ATTACK, null);
          }

          this.attackLifecycle(AttackLifecycle.PAUSED);
          yield CompletableFuture.completedFuture(null);
        }
        case STOPPING, PAUSED -> CompletableFuture.completedFuture(null);
        case STOPPED -> {
          if (initiator != null) {
            addAuditLog(initiator, InstanceAuditLogEntity.AuditLogType.START_ATTACK, null);
            addAuditLog(initiator, InstanceAuditLogEntity.AuditLogType.PAUSE_ATTACK, null);
          }

          yield scheduler.runAsync(this::start)
            .thenRunAsync(() -> this.attackLifecycle(AttackLifecycle.PAUSED), scheduler);
        }
      };
      case STOPPING, STOPPED -> switch (attackLifecycle()) {
        case STARTING, RUNNING, PAUSED -> {
          if (initiator != null) {
            addAuditLog(initiator, InstanceAuditLogEntity.AuditLogType.STOP_ATTACK, null);
          }
          yield stopAttackPermanently();
        }
        case STOPPING, STOPPED -> CompletableFuture.completedFuture(null);
      };
    };
  }

  private void start() {
    if (!attackLifecycle().isFullyStopped()) {
      throw new IllegalStateException("Another attack is still running");
    }

    allBotsConnected.set(false);
    this.attackLifecycle(AttackLifecycle.STARTING);

    log.info("Preparing bot attack at server");

    var botAmount = settingsSource.get(BotSettings.AMOUNT); // How many bots to connect
    var botsPerProxy =
      settingsSource.get(ProxySettings.BOTS_PER_PROXY); // How many bots per proxy are allowed

    // Prepare proxy tracking
    var proxies = new ArrayList<>(settingsSource.proxies()
      .stream()
      .map(p -> new ProxyData(p, botsPerProxy, new AtomicInteger(0)))
      .toList());

    if (!proxies.isEmpty() && settingsSource.get(ProxySettings.SHUFFLE_PROXIES)) {
      Collections.shuffle(proxies);
    }

    // Get existing enabled bots with valid account references
    var existingBots = settingsSource.bots().stream()
      .filter(BotEntity::enabled)
      .filter(bot -> settingsSource.getAccountById(bot.accountId()).isPresent())
      .toList();

    // Find accounts not yet assigned to a bot
    var usedAccountIds = existingBots.stream()
      .map(BotEntity::accountId)
      .collect(java.util.stream.Collectors.toSet());

    var availableAccounts = new ArrayList<>(settingsSource.accounts().stream()
      .filter(acc -> !usedAccountIds.contains(acc.profileId()))
      .toList());

    // If no accounts exist at all, generate offline accounts
    if (settingsSource.accounts().isEmpty()) {
      log.info("No custom accounts provided, generating offline accounts based on name format");
      for (var i = 0; i < botAmount; i++) {
        availableAccounts.add(OfflineAuthService.createAccount(settingsSource.get(AccountSettings.NAME_FORMAT).formatted(i + 1)));
      }
      // Persist the generated accounts
      sessionFactory.inTransaction(session -> {
        var instanceEntity = session.find(InstanceEntity.class, id);
        instanceEntity.settings(instanceEntity.settings().withAccounts(availableAccounts));
        session.merge(instanceEntity);
      });
      settingsSource.invalidateCache();
    }

    // Calculate how many bots we can have
    var maxPossibleBots = existingBots.size() + availableAccounts.size();
    if (!proxies.isEmpty()) {
      var maxByProxies = MathHelper.sumCapOverflow(proxies.stream().mapToInt(ProxyData::availableBots));
      maxPossibleBots = Math.min(maxPossibleBots, maxByProxies);
    }

    if (botAmount > maxPossibleBots) {
      log.warn("You have requested {} bots, but only {} are possible.", botAmount, maxPossibleBots);
      log.warn("Continuing with {} bots.", maxPossibleBots);
      botAmount = maxPossibleBots;
    }

    // Calculate how many more bots we need to create
    var botsToCreate = Math.max(0, botAmount - existingBots.size());

    // Count existing proxy usage from persisted bots
    for (var bot : existingBots) {
      if (bot.proxyId() != null) {
        proxies.stream()
          .filter(p -> p.proxy().id().equals(bot.proxyId()))
          .findFirst()
          .ifPresent(p -> p.useCount().incrementAndGet());
      }
    }

    // Create new bots if needed
    if (botsToCreate > 0 && !availableAccounts.isEmpty()) {
      log.info("Auto-creating {} bots from available accounts", botsToCreate);

      if (settingsSource.get(AccountSettings.SHUFFLE_ACCOUNTS)) {
        Collections.shuffle(availableAccounts);
      }

      var newBots = new ArrayList<BotEntity>();
      var accountIterator = availableAccounts.iterator();

      for (int i = 0; i < botsToCreate && accountIterator.hasNext(); i++) {
        var account = accountIterator.next();
        var proxy = getAvailableProxy(proxies).orElse(null);

        newBots.add(new BotEntity(
          UUID.randomUUID(),
          account.profileId(),
          proxy != null ? proxy.id() : null,
          new ConcurrentHashMap<>(),
          true,
          null
        ));
      }

      // Persist new bots
      if (!newBots.isEmpty()) {
        sessionFactory.inTransaction(session -> {
          var instanceEntity = session.find(InstanceEntity.class, id);
          var allBots = new ArrayList<>(instanceEntity.settings().bots());
          allBots.addAll(newBots);
          instanceEntity.settings(instanceEntity.settings().withBots(allBots));
          session.merge(instanceEntity);
        });

        // Refresh settings cache
        settingsSource.invalidateCache();
      }
    }

    // Now get final list of bots to connect
    var botsToConnect = settingsSource.bots().stream()
      .filter(BotEntity::enabled)
      .filter(bot -> settingsSource.getAccountById(bot.accountId()).isPresent())
      .limit(botAmount)
      .toList();

    if (botsToConnect.isEmpty()) {
      log.warn("No bots available to connect - add accounts first");
      this.attackLifecycle(AttackLifecycle.STOPPED);
      return;
    }

    // Prepare an event loop group for the attack
    var attackEventLoopGroup =
      NettyHelper.createEventLoopGroup("Attack-%s".formatted(id), runnableWrapper);

    var protocolVersion = settingsSource.get(BotSettings.PROTOCOL_VERSION, BotSettings.PROTOCOL_VERSION_PARSER);
    var serverAddress = BotConnectionFactory.parseAddress(settingsSource.get(BotSettings.ADDRESS), protocolVersion);

    var factories = new ArrayBlockingQueue<BotConnectionFactory>(botsToConnect.size());
    for (var bot : botsToConnect) {
      var account = settingsSource.getAccountById(bot.accountId()).orElseThrow();
      var refreshedAccount = refreshAccount(account);
      var proxy = bot.proxyId() != null
        ? settingsSource.getProxyById(bot.proxyId()).orElse(null)
        : null;

      factories.add(
        new BotConnectionFactory(
          this,
          settingsSource,
          bot,
          refreshedAccount,
          protocolVersion,
          serverAddress,
          proxy,
          attackEventLoopGroup
        ));
    }

    var usedProxies = proxies.stream().filter(ProxyData::hasBots).count();
    if (usedProxies == 0) {
      log.info("Starting attack at server with {} bots", factories.size());
    } else {
      log.info("Starting attack at server with {} bots and {} active proxies", factories.size(), usedProxies);
    }

    SoulFireAPI.postEvent(new AttackStartEvent(this));

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
              if (attackLifecycle().isStoppedOrStopping()) {
                return;
              }

              TimeUtil.waitCondition(() -> attackLifecycle().isPaused());

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
        if (this.attackLifecycle() == AttackLifecycle.STARTING) {
          this.attackLifecycle(AttackLifecycle.RUNNING);
        }
      });
  }

  private Optional<SFProxy> getAvailableProxy(List<ProxyData> proxies) {
    return proxies.stream()
      .filter(ProxyData::isAvailable)
      .min(Comparator.comparingInt(ProxyData::usedBots))
      .map(p -> {
        p.useCount().incrementAndGet();
        return p.proxy();
      });
  }

  public CompletableFuture<?> deleteInstance() {
    return stopAttackPermanently()
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
    return stopAttackSession()
      .thenRunAsync(scriptManager::destroyManager, scheduler)
      .thenRunAsync(scheduler::shutdown, soulFireServer.scheduler());
  }

  public CompletableFuture<?> stopAttackPermanently() {
    if (attackLifecycle().isStoppedOrStopping()) {
      return CompletableFuture.completedFuture(null);
    }

    log.info("Stopping bot attack");
    this.attackLifecycle(AttackLifecycle.STOPPING);

    return this.stopAttackSession()
      .thenRunAsync(() -> {
        this.attackLifecycle(AttackLifecycle.STOPPED);
        log.info("Attack stopped");
      }, scheduler);
  }

  private void attackLifecycle(AttackLifecycle attackLifecycle) {
    this.attackLifecycle = attackLifecycle;
    sessionFactory.inTransaction(session -> {
      var instanceEntity = session.find(InstanceEntity.class, id);
      if (instanceEntity == null) {
        return;
      }

      instanceEntity.attackLifecycle(attackLifecycle);

      session.merge(instanceEntity);
    });
  }

  // Doesn't shut down properly unless #shutdown() is called
  // Not sure why, netty moment...
  @SuppressWarnings("deprecation")
  public CompletableFuture<?> stopAttackSession() {
    return scheduler.runAsync(() -> {
      allBotsConnected.set(false);
      log.info("Disconnecting bots");
      do {
        var eventLoopGroups = new HashSet<EventLoopGroup>();
        var disconnectFuture = new ArrayList<CompletableFuture<?>>();
        botConnections.entrySet().removeIf(entry -> {
          var botConnection = entry.getValue();
          disconnectFuture.add(scheduler.runAsync(() -> botConnection.disconnect(Component.text("Attack stopped"))));
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

        log.info("Shutting down attack event loop groups");
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
      SoulFireAPI.postEvent(new AttackEndedEvent(this));
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

  public Optional<BotConnection> getBotConnection(UUID botId) {
    return Optional.ofNullable(botConnections.get(botId));
  }

  public void storeNewBot(BotConnection connection) {
    botConnections.put(connection.botId(), connection);
  }

  /**
   * Update bot metadata and persist to profile.
   */
  public void updateBotMetadata(UUID botId, Map<String, Value> updates, boolean merge) {
    // Update runtime bot if connected
    var connection = botConnections.get(botId);
    if (connection != null) {
      if (merge) {
        connection.botEntity().mergeMetadata(updates);
      } else {
        connection.botEntity().clearMetadata();
        connection.botEntity().mergeMetadata(updates);
      }
    }

    // Persist to profile
    sessionFactory.inTransaction(session -> {
      var instanceEntity = session.find(InstanceEntity.class, id);
      var settings = instanceEntity.settings();

      var updatedBot = settings.getBotById(botId)
        .map(b -> {
          if (merge) {
            b.mergeMetadata(updates);
          } else {
            b.clearMetadata();
            b.mergeMetadata(updates);
          }
          return b;
        })
        .orElseThrow(() -> new IllegalArgumentException("Bot not found: " + botId));

      instanceEntity.settings(settings.withUpdatedBot(updatedBot));
      session.merge(instanceEntity);
    });
  }

  /**
   * Delete specific metadata keys from a bot.
   */
  public void deleteBotMetadataKeys(UUID botId, List<String> keys) {
    // Update runtime bot if connected
    var connection = botConnections.get(botId);
    if (connection != null) {
      keys.forEach(connection.botEntity()::removeMetadata);
    }

    // Persist to profile
    sessionFactory.inTransaction(session -> {
      var instanceEntity = session.find(InstanceEntity.class, id);
      var settings = instanceEntity.settings();

      var updatedBot = settings.getBotById(botId)
        .map(b -> {
          keys.forEach(b::removeMetadata);
          return b;
        })
        .orElseThrow(() -> new IllegalArgumentException("Bot not found: " + botId));

      instanceEntity.settings(settings.withUpdatedBot(updatedBot));
      session.merge(instanceEntity);
    });
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
