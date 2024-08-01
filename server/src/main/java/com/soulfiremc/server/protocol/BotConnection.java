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
package com.soulfiremc.server.protocol;

import com.soulfiremc.server.InstanceManager;
import com.soulfiremc.server.SoulFireScheduler;
import com.soulfiremc.server.api.EventBusOwner;
import com.soulfiremc.server.api.event.EventExceptionHandler;
import com.soulfiremc.server.api.event.SoulFireBotEvent;
import com.soulfiremc.server.api.event.attack.PreBotConnectEvent;
import com.soulfiremc.server.api.event.bot.BotPostTickEvent;
import com.soulfiremc.server.api.event.bot.BotPreTickEvent;
import com.soulfiremc.server.protocol.bot.BotControlAPI;
import com.soulfiremc.server.protocol.bot.SessionDataManager;
import com.soulfiremc.server.protocol.bot.state.TickHookContext;
import com.soulfiremc.server.protocol.netty.ResolveUtil;
import com.soulfiremc.server.protocol.netty.ViaClientSession;
import com.soulfiremc.server.settings.lib.SettingsHolder;
import com.soulfiremc.server.util.TimeUtil;
import com.soulfiremc.settings.account.MinecraftAccount;
import com.soulfiremc.settings.account.service.OnlineJavaDataLike;
import com.soulfiremc.settings.proxy.SFProxy;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.lenni0451.lambdaevents.LambdaManager;
import net.lenni0451.lambdaevents.generator.ASMGenerator;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.slf4j.Logger;
import org.slf4j.MDC;

@Getter
public final class BotConnection implements EventBusOwner<SoulFireBotEvent> {
  public static final ThreadLocal<BotConnection> CURRENT = new ThreadLocal<>();
  private final UUID connectionId = UUID.randomUUID();
  private final LambdaManager eventBus = LambdaManager.basic(new ASMGenerator())
    .setExceptionHandler(EventExceptionHandler.INSTANCE)
    .setEventFilter(
      (c, h) -> {
        if (SoulFireBotEvent.class.isAssignableFrom(c)) {
          return true;
        } else {
          throw new IllegalStateException(
            "This event handler only accepts bot events");
        }
      });
  private final List<Runnable> shutdownHooks = new CopyOnWriteArrayList<>();
  private final Queue<Runnable> preTickHooks = new ConcurrentLinkedQueue<>();
  private final SoulFireScheduler scheduler;
  private final BotConnectionFactory factory;
  private final InstanceManager instanceManager;
  private final SettingsHolder settingsHolder;
  private final Logger logger;
  private final MinecraftProtocol protocol;
  private final ViaClientSession session;
  private final ResolveUtil.ResolvedAddress resolvedAddress;
  private final MinecraftAccount minecraftAccount;
  private final UUID accountProfileId;
  private final String accountName;
  private final ProtocolState targetState;
  private final ProtocolVersion protocolVersion;
  private final SFSessionService sessionService;
  private final SessionDataManager dataManager;
  private final BotControlAPI botControl;
  private final Object shutdownLock = new Object();
  private boolean running = true;

  public BotConnection(
    BotConnectionFactory factory,
    InstanceManager instanceManager,
    SettingsHolder settingsHolder,
    Logger logger,
    MinecraftProtocol protocol,
    ResolveUtil.ResolvedAddress resolvedAddress,
    MinecraftAccount minecraftAccount,
    ProtocolState targetState,
    ProtocolVersion protocolVersion,
    SFProxy proxyData,
    EventLoopGroup eventLoopGroup) {
    this.factory = factory;
    this.instanceManager = instanceManager;
    this.settingsHolder = settingsHolder;
    this.logger = logger;
    this.scheduler = new SoulFireScheduler(logger, runnable -> () -> {
      CURRENT.set(this);
      try {
        runnable.run();
      } finally {
        CURRENT.remove();
      }
    });
    this.protocol = protocol;
    this.resolvedAddress = resolvedAddress;
    this.minecraftAccount = minecraftAccount;
    this.accountProfileId = minecraftAccount.profileId();
    this.accountName = minecraftAccount.lastKnownName();
    this.targetState = targetState;
    this.protocolVersion = protocolVersion;
    this.sessionService =
      minecraftAccount.isPremiumJava()
        ? new SFSessionService(minecraftAccount.authType(), proxyData)
        : null;
    this.session = new ViaClientSession(
      resolvedAddress.resolvedAddress(), logger, protocol, proxyData, eventLoopGroup, this);
    this.dataManager = new SessionDataManager(this);
    this.botControl = new BotControlAPI(this, dataManager);

    // Start the tick loop
    scheduler.schedule(this::tickLoop);
  }

  public CompletableFuture<?> connect() {
    return CompletableFuture.runAsync(
      () -> {
        instanceManager.eventBus().call(new PreBotConnectEvent(this));
        session.connect(true);
      });
  }

  public boolean isOnline() {
    return session.isConnected();
  }

  private void tickLoop() {
    MDC.put("connectionId", connectionId.toString());
    MDC.put("botName", accountName);
    MDC.put("botUuid", accountProfileId.toString());

    while (this.running) {
      var tickTimer = dataManager.tickTimer();
      var ticks = tickTimer.advanceTime(System.currentTimeMillis());

      if (session.isDisconnected()) {
        wasDisconnected();
        break;
      }

      try {
        tick(ticks);
      } catch (Throwable t) {
        logger.error("Exception ticking bot", t);
      }
    }
  }

  public void tick(int ticks) {
    try {
      session.tick(); // Ensure all packets are handled before ticking

      while (!preTickHooks.isEmpty()) {
        preTickHooks.poll().run();
      }

      for (var i = 0L; i < Math.min(ticks, 10); i++) {
        var tickHookState = TickHookContext.INSTANCE.get();
        tickHookState.clear();

        eventBus.call(new BotPreTickEvent(this));
        tickHookState.callHooks(TickHookContext.HookType.PRE_TICK);

        dataManager.tick();
        botControl.tick();

        eventBus.call(new BotPostTickEvent(this));
        tickHookState.callHooks(TickHookContext.HookType.POST_TICK);
      }
    } catch (Throwable t) {
      logger.error("Error while ticking bot!", t);
    }
  }

  public GlobalTrafficShapingHandler trafficHandler() {
    return session.getFlag(SFProtocolConstants.TRAFFIC_HANDLER);
  }

  public void gracefulDisconnect() {
    synchronized (shutdownLock) {
      if (!running) {
        return;
      }

      running = false;

      // Run all shutdown hooks
      shutdownHooks.forEach(Runnable::run);

      session.disconnect(Component.translatable("multiplayer.status.quitting"));

      // Give the server one second to handle the disconnect
      TimeUtil.waitTime(1, TimeUnit.SECONDS);

      // Shut down all executors
      scheduler.shutdown();

      // Let threads finish that didn't immediately interrupt
      TimeUtil.waitTime(100, TimeUnit.MILLISECONDS);
    }
  }

  public void wasDisconnected() {
    synchronized (shutdownLock) {
      if (!running) {
        return;
      }

      running = false;

      // Run all shutdown hooks
      shutdownHooks.forEach(Runnable::run);

      // Shut down all executors
      scheduler.shutdown();
    }
  }

  public IdentifiedKey identifiedKey() {
    throw new UnsupportedOperationException("Not implemented yet!");
  }

  public void joinServerId(String serverId, ViaClientSession session) {
    try {
      var javaData = (OnlineJavaDataLike) minecraftAccount.accountData();
      sessionService.joinServer(accountProfileId, javaData.authToken(), serverId);
      session.logger().debug("Successfully sent mojang join request!");
    } catch (Exception e) {
      session.disconnect(Component.translatable("disconnect.loginFailedInfo", e.getMessage()), e);
    }
  }

  public void sendPacket(Packet packet) {
    session.send(packet);
  }
}
