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

import com.soulfiremc.server.account.SFOfflineAuthService;
import com.soulfiremc.server.api.AttackState;
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
import com.soulfiremc.server.settings.lib.SettingsHolder;
import com.soulfiremc.server.util.RandomUtil;
import com.soulfiremc.server.util.TimeUtil;
import com.soulfiremc.server.viaversion.SFVersionConstants;
import com.soulfiremc.settings.account.MinecraftAccount;
import com.soulfiremc.settings.proxy.SFProxy;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.protocol.version.VersionType;
import io.netty.channel.EventLoopGroup;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.lenni0451.lambdaevents.LambdaManager;
import net.lenni0451.lambdaevents.generator.ASMGenerator;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class AttackManager {
  private static final AtomicInteger ID_COUNTER = new AtomicInteger();
  private final int id = ID_COUNTER.getAndIncrement();
  private final Logger logger = LoggerFactory.getLogger("AttackManager-" + id);
  private final SoulFireScheduler scheduler = new SoulFireScheduler(logger);
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
  private final SettingsHolder settingsHolder;
  @Setter
  private AttackState attackState = AttackState.STOPPED;

  private static MinecraftAccount getAccount(
    SettingsHolder settingsHolder, List<MinecraftAccount> accounts, int botId) {
    if (accounts.isEmpty()) {
      return SFOfflineAuthService.createAccount(
        String.format(settingsHolder.get(AccountSettings.NAME_FORMAT), botId));
    }

    return accounts.removeFirst();
  }

  private static Optional<SFProxy> getProxy(
    int accountsPerProxy, Object2IntMap<SFProxy> proxyUseMap) {
    if (proxyUseMap.isEmpty()) {
      return Optional.empty(); // No proxies available
    }

    var selectedProxy =
      proxyUseMap.object2IntEntrySet().stream()
        .filter(entry -> accountsPerProxy == -1 || entry.getIntValue() < accountsPerProxy)
        .min(Comparator.comparingInt(Map.Entry::getValue))
        .orElseThrow(
          () -> new IllegalStateException("No proxies available!")); // Should never happen

    // Always present
    selectedProxy.setValue(selectedProxy.getIntValue() + 1);

    return Optional.of(selectedProxy.getKey());
  }

  public CompletableFuture<?> start() {
    if (!attackState.isStopped()) {
      throw new IllegalStateException("Attack is already running");
    }

    SoulFireServer.setupLoggingAndVia(settingsHolder);

    this.attackState = AttackState.RUNNING;

    var address = settingsHolder.get(BotSettings.ADDRESS);
    logger.info("Preparing bot attack at {}", address);

    var botAmount = settingsHolder.get(BotSettings.AMOUNT); // How many bots to connect
    var botsPerProxy =
      settingsHolder.get(ProxySettings.BOTS_PER_PROXY); // How many bots per proxy are allowed

    var proxies = new ArrayList<>(settingsHolder.proxies());
    var availableProxiesCount = proxies.size(); // How many proxies are available?
    var maxBots =
      botsPerProxy > 0
        ? botsPerProxy * availableProxiesCount
        : botAmount; // How many bots can be used at max

    if (botAmount > maxBots) {
      logger.warn("You have specified {} bots, but only {} are available.", botAmount, maxBots);
      logger.warn(
        "You need {} more proxies to run this amount of bots.",
        (botAmount - maxBots) / botsPerProxy);
      logger.warn("Continuing with {} bots.", maxBots);
      botAmount = maxBots;
    }

    var accounts = new ArrayList<>(settingsHolder.accounts());
    var availableAccounts = accounts.size();

    if (availableAccounts > 0 && botAmount > availableAccounts) {
      logger.warn(
        "You have specified {} bots, but only {} accounts are available.",
        botAmount,
        availableAccounts);
      logger.warn("Continuing with {} bots.", availableAccounts);
      botAmount = availableAccounts;
    }

    if (settingsHolder.get(AccountSettings.SHUFFLE_ACCOUNTS)) {
      Collections.shuffle(accounts);
    }

    var proxyUseMap = new Object2IntOpenHashMap<SFProxy>();
    for (var proxy : proxies) {
      proxyUseMap.put(proxy, 0);
    }

    // Prepare an event loop group for the attack
    var attackEventLoopGroup =
      SFNettyHelper.createEventLoopGroup(0, "Attack-%d".formatted(id));

    var protocolVersion =
      settingsHolder.get(BotSettings.PROTOCOL_VERSION, s -> {
        var split = s.split("\\|");
        if (split.length == 1) {
          return ProtocolVersion.getClosest(split[0]);
        }

        return ProtocolVersion.getProtocol(VersionType.valueOf(split[0]), Integer.parseInt(split[1]));
      });
    var isBedrock = SFVersionConstants.isBedrock(protocolVersion);
    var targetAddress = ResolveUtil.resolveAddress(isBedrock, settingsHolder);

    var factories = new ArrayBlockingQueue<BotConnectionFactory>(botAmount);
    for (var botId = 1; botId <= botAmount; botId++) {
      var proxyData = getProxy(botsPerProxy, proxyUseMap).orElse(null);
      var minecraftAccount = getAccount(settingsHolder, accounts, botId);

      // AuthData will be used internally instead of the MCProtocol data
      var protocol = new MinecraftProtocol();

      // Make sure this options is set to false, otherwise it will cause issues with ViaVersion
      protocol.setUseDefaultListeners(false);

      factories.add(
        new BotConnectionFactory(
          this,
          UUID.randomUUID(),
          targetAddress.orElseThrow(
            () -> new IllegalStateException("Could not resolve address")),
          settingsHolder,
          LoggerFactory.getLogger(minecraftAccount.lastKnownName()),
          protocol,
          minecraftAccount,
          protocolVersion,
          proxyData,
          attackEventLoopGroup));
    }

    if (availableProxiesCount == 0) {
      logger.info("Starting attack at {} with {} bots", address, factories.size());
    } else {
      logger.info(
        "Starting attack at {} with {} bots and {} proxies",
        address,
        factories.size(),
        availableProxiesCount);
    }

    eventBus.call(new AttackStartEvent(this));

    var connectSemaphore = new Semaphore(settingsHolder.get(BotSettings.CONCURRENT_CONNECTS));
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
              settingsHolder.get(BotSettings.JOIN_DELAY.min()),
              settingsHolder.get(BotSettings.JOIN_DELAY.max())),
            TimeUnit.MILLISECONDS);
        }
      });
  }

  public CompletableFuture<?> stop() {
    if (attackState.isStopped()) {
      return CompletableFuture.completedFuture(null);
    }

    logger.info("Stopping bot attack");
    this.attackState = AttackState.STOPPED;

    return CompletableFuture.runAsync(this::stopInternal);
  }

  private void stopInternal() {
    logger.info("Shutting down attack executor");
    scheduler.shutdown();

    logger.info("Disconnecting bots");
    do {
      var eventLoopGroups = new HashSet<EventLoopGroup>();
      var disconnectFuture = new ArrayList<CompletableFuture<?>>();
      for (var entry : Map.copyOf(botConnections).entrySet()) {
        disconnectFuture.add(entry.getValue().gracefulDisconnect());
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

    logger.info("Attack stopped");
  }
}
