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

import com.soulfiremc.server.account.MCAuthService;
import com.soulfiremc.server.account.MinecraftAccount;
import com.soulfiremc.server.account.OfflineAuthService;
import com.soulfiremc.server.api.AttackLifecycle;
import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.api.event.attack.AttackBotRemoveEvent;
import com.soulfiremc.server.api.event.attack.AttackEndedEvent;
import com.soulfiremc.server.api.event.attack.AttackStartEvent;
import com.soulfiremc.server.api.event.attack.AttackTickEvent;
import com.soulfiremc.server.api.metadata.MetadataHolder;
import com.soulfiremc.server.database.InstanceAuditLogEntity;
import com.soulfiremc.server.database.InstanceEntity;
import com.soulfiremc.server.database.UserEntity;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.protocol.BotConnectionFactory;
import com.soulfiremc.server.protocol.netty.ResolveUtil;
import com.soulfiremc.server.protocol.netty.SFNettyHelper;
import com.soulfiremc.server.proxy.SFProxy;
import com.soulfiremc.server.script.ScriptManager;
import com.soulfiremc.server.settings.instance.AccountSettings;
import com.soulfiremc.server.settings.instance.BotSettings;
import com.soulfiremc.server.settings.instance.ProxySettings;
import com.soulfiremc.server.settings.lib.InstanceSettingsDelegate;
import com.soulfiremc.server.user.SoulFireUser;
import com.soulfiremc.server.util.MathHelper;
import com.soulfiremc.server.util.SFHelpers;
import com.soulfiremc.server.util.TimeUtil;
import com.soulfiremc.server.util.log4j.SFLogAppender;
import com.soulfiremc.server.util.structs.CachedLazyObject;
import com.soulfiremc.server.viaversion.SFVersionConstants;
import io.netty.channel.EventLoopGroup;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.SessionFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a single instance.
 * An instance persists settings over restarts and managed attack session and attack state.
 */
@Slf4j
@Getter
public final class InstanceManager {
  public static final ThreadLocal<InstanceManager> CURRENT = new ThreadLocal<>();
  private final Map<UUID, BotConnection> botConnections = new ConcurrentHashMap<>();
  private final MetadataHolder metadata = new MetadataHolder();
  private final ScriptManager scriptManager = new ScriptManager();
  private final UUID id;
  private final SoulFireScheduler scheduler;
  private final InstanceSettingsDelegate settingsSource;
  private final SoulFireServer soulFireServer;
  private final SessionFactory sessionFactory;
  private final SoulFireScheduler.RunnableWrapper runnableWrapper;
  private final CachedLazyObject<String> friendlyNameCache;
  private final AtomicBoolean allBotsConnected = new AtomicBoolean(false);
  private AttackLifecycle attackLifecycle = AttackLifecycle.STOPPED;

  public InstanceManager(SoulFireServer soulFireServer, SessionFactory sessionFactory, InstanceEntity instanceEntity) {
    this.id = instanceEntity.id();
    this.runnableWrapper = soulFireServer.runnableWrapper().with(runnable -> () -> {
      CURRENT.set(this);
      try (var ignored1 = MDC.putCloseable(SFLogAppender.SF_INSTANCE_ID, id.toString());
           var ignored2 = MDC.putCloseable(SFLogAppender.SF_INSTANCE_NAME, friendlyNameCache().get())) {
        runnable.run();
      } finally {
        CURRENT.remove();
      }
    });
    this.scheduler = new SoulFireScheduler(runnableWrapper);
    this.soulFireServer = soulFireServer;
    this.sessionFactory = sessionFactory;
    this.settingsSource = new InstanceSettingsDelegate(new CachedLazyObject<>(() ->
      sessionFactory.fromTransaction(session -> session.find(InstanceEntity.class, id).settings()), 1, TimeUnit.SECONDS));
    this.friendlyNameCache = new CachedLazyObject<>(() ->
      sessionFactory.fromTransaction(session -> {
        var instance = session.find(InstanceEntity.class, id);

        if (instance == null) {
          return "Unknown";
        } else {
          return instance.friendlyName();
        }
      }), 1, TimeUnit.SECONDS);

    try {
      Files.createDirectories(getInstanceObjectStoragePath());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    this.scheduler.scheduleWithFixedDelay(this::tick, 0, 500, TimeUnit.MILLISECONDS);
    this.scheduler.scheduleWithFixedDelay(this::refreshExpiredAccounts, 0, 1, TimeUnit.HOURS);

    if (settingsSource.get(BotSettings.RESTORE_ON_REBOOT)) {
      switchToState(null, instanceEntity.attackLifecycle());
    } else {
      attackLifecycle(AttackLifecycle.STOPPED);
    }
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
    if (attackLifecycle().isTicking()) {
      SoulFireAPI.postEvent(new AttackTickEvent(this));
    }

    evictBots();
  }

  private void evictBots() {
    // Remove botConnections from the map that are closed
    botConnections.entrySet().removeIf(entry -> {
      var bot = entry.getValue();
      if (bot.session().isDisconnected() || bot.explicitlyShutdown()) {
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

    scriptManager.startScripts();

    log.info("Preparing bot attack at server");

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
        log.info("No proxies provided, attack will be performed without proxies");
      } else {
        var maxBots = MathHelper.sumCapOverflow(proxies.stream().mapToInt(ProxyData::availableBots));
        if (botAmount > maxBots) {
          log.warn("You have requested {} bots, but only {} are possible with the current amount of proxies.", botAmount, maxBots);
          log.warn("Continuing with {} bots.", maxBots);
          botAmount = maxBots;
        }

        if (settingsSource.get(ProxySettings.SHUFFLE_PROXIES)) {
          Collections.shuffle(proxies);
        }
      }
    }

    var accountQueue = new ArrayBlockingQueue<MinecraftAccount>(botAmount);
    {
      var accounts = new ArrayList<>(settingsSource.accounts());
      var availableAccounts = accounts.size();
      if (availableAccounts > 0) {
        if (botAmount > availableAccounts) {
          log.warn(
            "You have requested {} bots, but only {} are possible with the current amount of accounts.",
            botAmount,
            availableAccounts);
          log.warn("Continuing with {} bots.", availableAccounts);
          botAmount = availableAccounts;
        }
      } else {
        log.info("No custom accounts provided, generating offline accounts based on name format");
        for (var i = 0; i < botAmount; i++) {
          accounts.add(OfflineAuthService.createAccount(String.format(settingsSource.get(AccountSettings.NAME_FORMAT), i + 1)));
        }
      }

      if (settingsSource.get(AccountSettings.SHUFFLE_ACCOUNTS)) {
        Collections.shuffle(accounts);
      }

      accountQueue.addAll(accounts.subList(0, botAmount));
    }

    // Prepare an event loop group for the attack
    var attackEventLoopGroup =
      SFNettyHelper.createEventLoopGroup("Attack-%s".formatted(id), runnableWrapper);

    var protocolVersion = settingsSource.get(BotSettings.PROTOCOL_VERSION, BotSettings.PROTOCOL_VERSION_PARSER);
    var isBedrock = SFVersionConstants.isBedrock(protocolVersion);
    var targetAddress = ResolveUtil.resolveAddress(isBedrock, settingsSource)
      .orElseThrow(() -> new IllegalStateException("Could not resolve address"));

    var factories = new ArrayBlockingQueue<BotConnectionFactory>(botAmount);
    while (!accountQueue.isEmpty()) {
      var minecraftAccount = refreshAccount(accountQueue.poll());
      var proxyData = getProxy(proxies).orElse(null);
      factories.add(
        new BotConnectionFactory(
          this,
          targetAddress,
          settingsSource,
          minecraftAccount,
          protocolVersion,
          proxyData,
          attackEventLoopGroup));
    }

    var usedProxies = proxies.stream().filter(ProxyData::hasBots).count();
    if (usedProxies == 0) {
      log.info("Starting attack at server with {} bots", factories.size());
    } else {
      log.info("Starting attack at {} with server bots and {} active proxies", factories.size(), usedProxies);
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
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
          }

          log.debug("Scheduling bot {}", factory.minecraftAccount().lastKnownName());
          scheduler.schedule(
            () -> {
              if (attackLifecycle().isStoppedOrStopping()) {
                return;
              }

              TimeUtil.waitCondition(() -> attackLifecycle().isPaused());

              log.debug("Connecting bot {}", factory.minecraftAccount().lastKnownName());
              var botConnection = factory.prepareConnection();
              storeNewBot(botConnection);

              try {
                botConnection.connect().get();
              } catch (Throwable e) {
                log.error("Error while connecting", e);
              } finally {
                connectSemaphore.release();
              }
            });

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

  public CompletableFuture<?> deleteInstance() {
    return stopAttackPermanently()
      .thenRunAsync(scheduler::shutdown, soulFireServer.scheduler())
      .thenRunAsync(() -> {
        try {
          SFHelpers.deleteDirectory(getInstanceObjectStoragePath());
        } catch (IOException e) {
          log.error("Error while deleting instance storage", e);
        }
      }, scheduler);
  }

  public CompletableFuture<?> shutdownHook() {
    return stopAttackSession().thenRunAsync(scheduler::shutdown, soulFireServer.scheduler());
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

  public CompletableFuture<?> stopAttackSession() {
    return scheduler.runAsync(() -> {
      allBotsConnected.set(false);
      log.info("Disconnecting bots");
      do {
        var eventLoopGroups = new HashSet<EventLoopGroup>();
        var disconnectFuture = new ArrayList<CompletableFuture<?>>();
        botConnections.entrySet().removeIf(entry -> {
          var botConnection = entry.getValue();
          disconnectFuture.add(scheduler.runAsync(botConnection::gracefulDisconnect));
          eventLoopGroups.add(botConnection.session().eventLoopGroup());
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
            eventLoopGroup.shutdownGracefully().get();
          } catch (InterruptedException | ExecutionException e) {
            log.error("Error while shutting down", e);
          }
        }
      } while (!botConnections.isEmpty()); // To make sure really all bots are disconnected

      scriptManager.killAllScripts();

      // Notify plugins of state change
      SoulFireAPI.postEvent(new AttackEndedEvent(this));
    });
  }

  public void addAuditLog(SoulFireUser source, InstanceAuditLogEntity.AuditLogType logType, @Nullable String data) {
    scheduler.runAsync(() -> sessionFactory.inTransaction(session -> {
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

  public List<BotConnection> getConnectedBots() {
    return new ArrayList<>(botConnections.values());
  }

  public void storeNewBot(BotConnection connection) {
    botConnections.put(connection.accountProfileId(), connection);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) return true;
    if (!(object instanceof InstanceManager that)) return false;

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
}
