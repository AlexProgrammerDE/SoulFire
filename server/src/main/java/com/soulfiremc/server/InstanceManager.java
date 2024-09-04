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
import com.soulfiremc.server.account.SFOfflineAuthService;
import com.soulfiremc.server.api.AttackState;
import com.soulfiremc.server.api.EventBusOwner;
import com.soulfiremc.server.api.event.EventExceptionHandler;
import com.soulfiremc.server.api.event.SoulFireAttackEvent;
import com.soulfiremc.server.api.event.attack.AttackEndedEvent;
import com.soulfiremc.server.api.event.attack.AttackStartEvent;
import com.soulfiremc.server.protocol.BotConnection;
import com.soulfiremc.server.protocol.BotConnectionFactory;
import com.soulfiremc.server.protocol.netty.ResolveUtil;
import com.soulfiremc.server.protocol.netty.SFNettyHelper;
import com.soulfiremc.server.settings.AccountSettings;
import com.soulfiremc.server.settings.BotSettings;
import com.soulfiremc.server.settings.ProxySettings;
import com.soulfiremc.server.settings.lib.SettingsDelegate;
import com.soulfiremc.server.settings.lib.SettingsImpl;
import com.soulfiremc.server.settings.lib.SettingsSource;
import com.soulfiremc.server.util.MathHelper;
import com.soulfiremc.server.util.RandomUtil;
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
  @Setter
  private String friendlyName;
  private final SettingsDelegate settingsSource;
  @Setter
  private AttackState attackState = AttackState.STOPPED;
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

  public InstanceManager(SoulFireServer soulFireServer, UUID id, String friendlyName, SettingsImpl settingsSource) {
    this.id = id;
    this.friendlyName = friendlyName;
    this.logger = LoggerFactory.getLogger("InstanceManager-" + id);
    this.scheduler = new SoulFireScheduler(logger);
    this.soulFireServer = soulFireServer;
    this.settingsSource = new SettingsDelegate(settingsSource);
  }

  public static InstanceManager fromJson(SoulFireServer soulFireServer, JsonElement json) {
    var id = GSON.fromJson(json.getAsJsonObject().get("id"), UUID.class);
    var friendlyName = json.getAsJsonObject().get("friendlyName").getAsString();
    var state = GSON.fromJson(json.getAsJsonObject().get("state"), AttackState.class);
    var settings = SettingsImpl.deserialize(json.getAsJsonObject().get("settings"));

    var instanceManager = new InstanceManager(soulFireServer, id, friendlyName, settings);
    instanceManager.switchToState(state);

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

  public void switchToState(AttackState targetState) {
    switch (targetState) {
      case RUNNING -> {
        switch (attackState) {
          case RUNNING -> {
            // NO-OP
          }
          case PAUSED -> this.attackState = AttackState.RUNNING;
          case STOPPED -> start();
        }
      }
      case PAUSED -> {
        switch (attackState) {
          case RUNNING -> this.attackState = AttackState.PAUSED;
          case PAUSED -> {
            // NO-OP
          }
          case STOPPED -> {
            start();
            this.attackState = AttackState.PAUSED;
          }
        }
      }
      case STOPPED -> {
        switch (attackState) {
          case RUNNING, PAUSED -> stopAttackPermanently();
          case STOPPED -> {
            // NO-OP
          }
        }
      }
    }
  }

  public CompletableFuture<?> start() {
    if (!attackState.isStopped()) {
      throw new IllegalStateException("Attack is already running");
    }

    SoulFireServer.setupLoggingAndVia(settingsSource);

    this.attackState = AttackState.RUNNING;

    var address = settingsSource.get(BotSettings.ADDRESS);
    logger.info("Preparing bot attack at {}", address);

    var botAmount = settingsSource.get(BotSettings.AMOUNT); // How many bots to connect
    var botsPerProxy =
      settingsSource.get(ProxySettings.BOTS_PER_PROXY); // How many bots per proxy are allowed

    var proxies = new ArrayList<>(settingsSource.proxies()
      .stream()
      .map(p -> new ProxyData(p, botsPerProxy, new AtomicInteger(0)))
      .toList());
    var availableProxies = proxies.size();
    {
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
          accounts.add(SFOfflineAuthService.createAccount(String.format(settingsSource.get(AccountSettings.NAME_FORMAT), i + 1)));
        }
      }

      if (settingsSource.get(AccountSettings.SHUFFLE_ACCOUNTS)) {
        Collections.shuffle(accounts);
      }

      accountQueue.addAll(accounts);
    }

    // Prepare an event loop group for the attack
    var attackEventLoopGroup =
      SFNettyHelper.createEventLoopGroup(0, "Attack-%s".formatted(id));

    var protocolVersion = settingsSource.get(BotSettings.PROTOCOL_VERSION, BotSettings.PROTOCOL_VERSION_PARSER);
    var isBedrock = SFVersionConstants.isBedrock(protocolVersion);
    var targetAddress = ResolveUtil.resolveAddress(isBedrock, settingsSource)
      .orElseThrow(() -> new IllegalStateException("Could not resolve address"));

    var factories = new ArrayBlockingQueue<BotConnectionFactory>(botAmount);
    while (!accountQueue.isEmpty()) {
      var minecraftAccount = accountQueue.poll();
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

    eventBus.call(new AttackStartEvent(this));

    var connectSemaphore = new Semaphore(settingsSource.get(BotSettings.CONCURRENT_CONNECTS));
    return CompletableFuture.runAsync(
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
              if (attackState.isStopped()) {
                return;
              }

              TimeUtil.waitCondition(attackState::isPaused);

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
            RandomUtil.getRandomInt(
              settingsSource.get(BotSettings.JOIN_DELAY.min()),
              settingsSource.get(BotSettings.JOIN_DELAY.max())),
            TimeUnit.MILLISECONDS);
        }
      });
  }

  public CompletableFuture<?> stopAttackPermanently() {
    if (attackState.isStopped()) {
      return CompletableFuture.completedFuture(null);
    }

    logger.info("Stopping bot attack");
    this.attackState = AttackState.STOPPED;

    return this.stopAttackSession();
  }

  public CompletableFuture<?> stopAttackSession() {
    return CompletableFuture.runAsync(() -> {
      logger.info("Draining attack executor");
      scheduler.blockNewTasks(true);
      scheduler.drainQueue();

      logger.info("Disconnecting bots");
      do {
        var eventLoopGroups = new HashSet<EventLoopGroup>();
        var disconnectFuture = new ArrayList<CompletableFuture<?>>();
        for (var entry : Map.copyOf(botConnections).entrySet()) {
          disconnectFuture.add(CompletableFuture.runAsync(entry.getValue()::gracefulDisconnect));
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
      eventBus.call(new AttackEndedEvent(this));

      scheduler.blockNewTasks(false);
      logger.info("Attack stopped");
    });
  }

  public InstanceListResponse.Instance toProto() {
    return InstanceListResponse.Instance.newBuilder()
      .setId(id.toString())
      .setFriendlyName(friendlyName)
      .setState(attackState.toProto())
      .build();
  }

  public JsonElement toJson() {
    var json = new JsonObject();

    json.addProperty("id", id.toString());
    json.addProperty("friendlyName", friendlyName);
    json.addProperty("state", attackState.name());
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
