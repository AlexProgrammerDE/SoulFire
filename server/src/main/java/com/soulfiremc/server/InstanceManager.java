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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.soulfiremc.grpc.generated.InstanceListResponse;
import com.soulfiremc.server.account.MCAuthService;
import com.soulfiremc.server.account.OfflineAuthService;
import com.soulfiremc.server.api.AttackLifecycle;
import com.soulfiremc.server.api.EventBusOwner;
import com.soulfiremc.server.api.event.EventExceptionHandler;
import com.soulfiremc.server.api.event.SoulFireAttackEvent;
import com.soulfiremc.server.api.event.attack.AttackEndedEvent;
import com.soulfiremc.server.api.event.attack.AttackStartEvent;
import com.soulfiremc.server.api.event.attack.AttackTickEvent;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.protocol.BotConnectionFactory;
import com.soulfiremc.server.protocol.netty.ResolveUtil;
import com.soulfiremc.server.protocol.netty.SFNettyHelper;
import com.soulfiremc.server.settings.AccountSettings;
import com.soulfiremc.server.settings.BotSettings;
import com.soulfiremc.server.settings.ProxySettings;
import com.soulfiremc.server.settings.lib.SettingsDelegate;
import com.soulfiremc.server.settings.lib.SettingsImpl;
import com.soulfiremc.server.util.MathHelper;
import com.soulfiremc.server.util.TimeUtil;
import com.soulfiremc.server.viaversion.SFVersionConstants;
import com.soulfiremc.settings.account.MinecraftAccount;
import com.soulfiremc.settings.proxy.SFProxy;
import io.netty.channel.EventLoopGroup;
import lombok.Getter;
import lombok.Setter;
import net.lenni0451.lambdaevents.LambdaManager;
import net.lenni0451.lambdaevents.generator.ASMGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class InstanceManager implements EventBusOwner<SoulFireAttackEvent> {
  private static final Gson GSON = new Gson();
  private final UUID id;
  private final Logger logger;
  private final SoulFireScheduler scheduler;
  private final SettingsDelegate settingsSource;
  private final LambdaManager eventBus =
    LambdaManager.basic(new ASMGenerator())
      .setExceptionHandler(EventExceptionHandler.INSTANCE)
      .setEventFilter(
        (c, h) -> {
          if (SoulFireAttackEvent.class.isAssignableFrom(c)) {
            return true;
          } else {
            throw new IllegalStateException("This event handler only accepts attack events");
          }
        });
  private final Map<UUID, BotConnection> botConnections = new ConcurrentHashMap<>();
  private final SoulFireServer soulFireServer;
  @Setter
  private String friendlyName;
  @Setter
  private AttackLifecycle attackLifecycle = AttackLifecycle.STOPPED;

  public InstanceManager(SoulFireServer soulFireServer, UUID id, String friendlyName, SettingsImpl settingsSource) {
    this.id = id;
    this.friendlyName = friendlyName;
    this.logger = LoggerFactory.getLogger("InstanceManager-" + id);
    this.scheduler = new SoulFireScheduler(logger);
    this.soulFireServer = soulFireServer;
    this.settingsSource = new SettingsDelegate(settingsSource);

    this.scheduler.scheduleWithFixedDelay(this::tick, 0, 500, TimeUnit.MILLISECONDS);
    this.scheduler.scheduleWithFixedDelay(this::refreshExpiredAccounts, 0, 1, TimeUnit.HOURS);
  }

  public static InstanceManager fromJson(SoulFireServer soulFireServer, JsonElement json) {
    var id = GSON.fromJson(json.getAsJsonObject().get("id"), UUID.class);
    var friendlyName = json.getAsJsonObject().get("friendlyName").getAsString();
    var state = GSON.fromJson(json.getAsJsonObject().get("state"), AttackLifecycle.class);
    var settings = SettingsImpl.deserialize(json.getAsJsonObject().get("settings"));

    var instanceManager = new InstanceManager(soulFireServer, id, friendlyName, settings);
    if (settings.get(BotSettings.RESTORE_ON_REBOOT)) {
      instanceManager.switchToState(state);
    }

    return instanceManager;
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
    if (attackLifecycle.isTicking()) {
      this.postEvent(new AttackTickEvent(this));
    }
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
          logger.info("Refreshing expired accounts");
        }

        accounts.add(authService.refresh(account, null).join());
        refreshed++;
      } else {
        accounts.add(account);
      }
    }

    if (refreshed > 0) {
      logger.info("Refreshed {} accounts", refreshed);
      settingsSource.source(settingsSource.source().withAccounts(accounts));
    }
  }

  private MinecraftAccount refreshAccount(MinecraftAccount account) {
    var authService = MCAuthService.convertService(account.authType());
    if (!authService.isExpiredOrOutdated(account)) {
      return account;
    }

    logger.info("Account {} is expired or outdated, refreshing before connecting", account.lastKnownName());
    var refreshedAccount = authService.refresh(account, null).join();
    var accounts = new ArrayList<>(settingsSource.accounts());
    accounts.set(accounts.indexOf(account), refreshedAccount);
    settingsSource.source(settingsSource.source().withAccounts(accounts));

    return refreshedAccount;
  }

  public CompletableFuture<?> switchToState(AttackLifecycle targetState) {
    return switch (targetState) {
      case STARTING, RUNNING -> switch (attackLifecycle) {
        case STARTING, RUNNING, STOPPING -> CompletableFuture.completedFuture(null);
        case PAUSED -> CompletableFuture.runAsync(() -> this.attackLifecycle = AttackLifecycle.RUNNING, scheduler);
        case STOPPED -> CompletableFuture.runAsync(this::start, scheduler);
      };
      case PAUSED -> switch (attackLifecycle) {
        case STARTING, RUNNING -> CompletableFuture.runAsync(() -> this.attackLifecycle = AttackLifecycle.PAUSED, scheduler);
        case STOPPING, PAUSED -> CompletableFuture.completedFuture(null);
        case STOPPED -> CompletableFuture.runAsync(() -> {
          start();
          this.attackLifecycle = AttackLifecycle.PAUSED;
        }, scheduler);
      };
      case STOPPING, STOPPED -> switch (attackLifecycle) {
        case STARTING, RUNNING, PAUSED -> stopAttackPermanently();
        case STOPPING, STOPPED -> CompletableFuture.completedFuture(null);
      };
    };
  }

  private void start() {
    if (!attackLifecycle.isFullyStopped()) {
      throw new IllegalStateException("Another attack is still running");
    }

    SoulFireServer.setupLoggingAndVia(settingsSource);

    this.attackLifecycle = AttackLifecycle.STARTING;

    var address = settingsSource.get(BotSettings.ADDRESS);
    logger.info("Preparing bot attack at {}", address);

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
        logger.info("No proxies provided, attack will be performed without proxies");
      } else {
        var maxBots = MathHelper.sumCapOverflow(proxies.stream().mapToInt(ProxyData::availableBots));
        if (botAmount > maxBots) {
          logger.warn("You have requested {} bots, but only {} are possible with the current amount of proxies.", botAmount, maxBots);
          logger.warn("Continuing with {} bots.", maxBots);
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
          logger.warn(
            "You have requested {} bots, but only {} are possible with the current amount of accounts.",
            botAmount,
            availableAccounts);
          logger.warn("Continuing with {} bots.", availableAccounts);
          botAmount = availableAccounts;
        }
      } else {
        logger.info("No custom accounts provided, generating offline accounts based on name format");
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
      SFNettyHelper.createEventLoopGroup("Attack-%s".formatted(id));

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
          LoggerFactory.getLogger(minecraftAccount.lastKnownName()),
          minecraftAccount,
          protocolVersion,
          proxyData,
          attackEventLoopGroup));
    }

    var usedProxies = proxies.stream().filter(ProxyData::hasBots).count();
    if (usedProxies == 0) {
      logger.info("Starting attack at {} with {} bots", address, factories.size());
    } else {
      logger.info(
        "Starting attack at {} with {} bots and {} active proxies",
        address,
        factories.size(),
        usedProxies);
    }

    postEvent(new AttackStartEvent(this));

    var connectSemaphore = new Semaphore(settingsSource.get(BotSettings.CONCURRENT_CONNECTS));
    scheduler.schedule(
      () -> {
        while (!factories.isEmpty()) {
          var factory = factories.poll();
          if (factory == null) {
            break;
          }

          try {
            connectSemaphore.acquire();
          } catch (InterruptedException e) {
            logger.error("Error while waiting for connection slot", e);
            break;
          }

          logger.debug("Scheduling bot {}", factory.minecraftAccount().lastKnownName());
          scheduler.schedule(
            () -> {
              if (attackLifecycle.isStoppedOrStopping()) {
                return;
              }

              TimeUtil.waitCondition(attackLifecycle::isPaused);

              logger.debug("Connecting bot {}", factory.minecraftAccount().lastKnownName());
              var botConnection = factory.prepareConnection();
              botConnections.put(botConnection.connectionId(), botConnection);

              try {
                botConnection.connect().get();
              } catch (Throwable e) {
                logger.error("Error while connecting", e);
              } finally {
                connectSemaphore.release();
              }
            });

          TimeUtil.waitTime(
            settingsSource.getRandom(BotSettings.JOIN_DELAY).getAsLong(),
            TimeUnit.MILLISECONDS);
        }

        if (this.attackLifecycle == AttackLifecycle.STARTING) {
          this.attackLifecycle = AttackLifecycle.RUNNING;
        }
      });
  }

  public CompletableFuture<?> deleteInstance() {
    return stopAttackPermanently().thenRun(scheduler::shutdown);
  }

  public CompletableFuture<?> shutdownHook() {
    return stopAttackSession().thenRun(scheduler::shutdown);
  }

  public CompletableFuture<?> stopAttackPermanently() {
    if (attackLifecycle.isStoppedOrStopping()) {
      return CompletableFuture.completedFuture(null);
    }

    logger.info("Stopping bot attack");
    this.attackLifecycle = AttackLifecycle.STOPPING;

    return this.stopAttackSession()
      .thenRun(() -> {
        this.attackLifecycle = AttackLifecycle.STOPPED;
        logger.info("Attack stopped");
      });
  }

  public CompletableFuture<?> stopAttackSession() {
    return CompletableFuture.runAsync(() -> {
      logger.info("Disconnecting bots");
      do {
        var eventLoopGroups = new HashSet<EventLoopGroup>();
        var disconnectFuture = new ArrayList<CompletableFuture<?>>();
        for (var entry : Map.copyOf(botConnections).entrySet()) {
          disconnectFuture.add(CompletableFuture.runAsync(entry.getValue()::gracefulDisconnect, scheduler));
          eventLoopGroups.add(entry.getValue().session().eventLoopGroup());
          botConnections.remove(entry.getKey());
        }

        logger.info("Waiting for all bots to fully disconnect");
        for (var future : disconnectFuture) {
          try {
            future.get();
          } catch (InterruptedException | ExecutionException e) {
            logger.error("Error while shutting down", e);
          }
        }

        logger.info("Shutting down attack event loop groups");
        for (var eventLoopGroup : eventLoopGroups) {
          try {
            eventLoopGroup.shutdownGracefully().get();
          } catch (InterruptedException | ExecutionException e) {
            logger.error("Error while shutting down", e);
          }
        }
      } while (!botConnections.isEmpty()); // To make sure really all bots are disconnected

      // Notify plugins of state change
      postEvent(new AttackEndedEvent(this));
    }, scheduler);
  }

  public InstanceListResponse.Instance toProto() {
    return InstanceListResponse.Instance.newBuilder()
      .setId(id.toString())
      .setFriendlyName(friendlyName)
      .setState(attackLifecycle.toProto())
      .build();
  }

  public JsonElement toJson() {
    var json = new JsonObject();

    json.addProperty("id", id.toString());
    json.addProperty("friendlyName", friendlyName);
    json.addProperty("state", attackLifecycle.name());
    json.add("settings", settingsSource.source().serializeToTree());

    return json;
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
