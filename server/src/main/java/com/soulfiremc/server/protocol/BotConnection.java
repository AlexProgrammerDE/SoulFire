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
import com.soulfiremc.server.account.MinecraftAccount;
import com.soulfiremc.server.account.service.OnlineJavaDataLike;
import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.api.event.bot.BotPostTickEvent;
import com.soulfiremc.server.api.event.bot.BotPreTickEvent;
import com.soulfiremc.server.api.event.bot.PreBotConnectEvent;
import com.soulfiremc.server.api.metadata.MetadataHolder;
import com.soulfiremc.server.protocol.bot.BotControlAPI;
import com.soulfiremc.server.protocol.bot.SessionDataManager;
import com.soulfiremc.server.protocol.bot.container.InventoryManager;
import com.soulfiremc.server.protocol.bot.state.ControlState;
import com.soulfiremc.server.protocol.netty.ResolveUtil;
import com.soulfiremc.server.protocol.netty.ViaClientSession;
import com.soulfiremc.server.proxy.SFProxy;
import com.soulfiremc.server.settings.lib.InstanceSettingsSource;
import com.soulfiremc.server.util.structs.SFLogAppender;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntry;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundClientTickEndPacket;
import org.slf4j.Logger;
import org.slf4j.MDC;

import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

@Getter
public final class BotConnection {
  public static final ThreadLocal<BotConnection> CURRENT = new ThreadLocal<>();
  private final List<Runnable> shutdownHooks = new CopyOnWriteArrayList<>();
  private final Queue<Runnable> preTickHooks = new ConcurrentLinkedQueue<>();
  private final MetadataHolder metadata = new MetadataHolder();
  private final ControlState controlState = new ControlState();
  private final InventoryManager inventoryManager;
  private final SoulFireScheduler scheduler;
  private final BotConnectionFactory factory;
  private final InstanceManager instanceManager;
  private final InstanceSettingsSource settingsSource;
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
  private final SoulFireScheduler.RunnableWrapper runnableWrapper;
  private final Object shutdownLock = new Object();
  private boolean explicitlyShutdown = false;
  private boolean running = true;
  @Setter
  private boolean pause = false;

  public BotConnection(
    BotConnectionFactory factory,
    InstanceManager instanceManager,
    InstanceSettingsSource settingsSource,
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
    this.settingsSource = settingsSource;
    this.logger = logger;
    this.minecraftAccount = minecraftAccount;
    this.accountProfileId = minecraftAccount.profileId();
    this.accountName = minecraftAccount.lastKnownName();
    this.runnableWrapper = instanceManager.runnableWrapper().with(runnable -> () -> {
      CURRENT.set(this);
      try (var ignored = MDC.putCloseable(SFLogAppender.SF_BOT_ACCOUNT_ID, this.accountProfileId.toString())) {
        runnable.run();
      } finally {
        CURRENT.remove();
      }
    });
    this.scheduler = new SoulFireScheduler(logger, runnableWrapper);
    this.protocol = protocol;
    this.resolvedAddress = resolvedAddress;
    this.targetState = targetState;
    this.protocolVersion = protocolVersion;
    this.sessionService =
      minecraftAccount.isPremiumJava()
        ? new SFSessionService(minecraftAccount.authType(), proxyData)
        : null;
    this.session = new ViaClientSession(
      resolvedAddress.resolvedAddress(), logger, protocol, proxyData, eventLoopGroup, this);
    this.dataManager = new SessionDataManager(this);
    this.inventoryManager = new InventoryManager(this);
    this.botControl = new BotControlAPI(this);

    // Start the tick loop
    scheduler.scheduleWithFixedDelay(this::tickLoop, 0, 1, TimeUnit.MILLISECONDS);
  }

  public CompletableFuture<?> connect() {
    return scheduler.runAsync(
      () -> {
        SoulFireAPI.postEvent(new PreBotConnectEvent(this));
        session.connect(true);
      });
  }

  public boolean isOnline() {
    return session.isConnected();
  }

  private void tickLoop() {
    if (!running) {
      return;
    }

    if (session.isDisconnected()) {
      wasDisconnected();
      return;
    }

    runTick();
  }

  public void runTick() {
    try {
      session.tick(); // Ensure all packets are handled before ticking

      while (!preTickHooks.isEmpty()) {
        preTickHooks.poll().run();
      }

      var tickTimer = dataManager.tickTimer();
      var ticks = tickTimer.advanceTime(System.nanoTime() / 1000000L, true);
      for (var i = 0; i < Math.min(10, ticks); i++) {
        this.tick();
      }

      tickTimer.updatePauseState(this.pause);
      tickTimer.updateFrozenState(!dataManager.isLevelRunningNormally());
    } catch (Throwable t) {
      logger.error("Error while ticking bot!", t);
    }
  }

  public void tick() {
    SoulFireAPI.postEvent(new BotPreTickEvent(this));

    botControl.tick();
    dataManager.tick();

    if (protocol.getOutboundState() == ProtocolState.GAME) {
      sendPacket(ServerboundClientTickEndPacket.INSTANCE);
    }

    SoulFireAPI.postEvent(new BotPostTickEvent(this));
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

      explicitlyShutdown = true;

      // Run all shutdown hooks
      shutdownHooks.forEach(Runnable::run);

      session.disconnect(Component.translatable("multiplayer.status.quitting"));

      // Shut down all executors
      scheduler.shutdown();
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

  public void joinServerId(String serverId) {
    try {
      var javaData = (OnlineJavaDataLike) minecraftAccount.accountData();
      sessionService.joinServer(accountProfileId, javaData.authToken(), serverId);
      logger.debug("Successfully sent mojang join request!");
    } catch (Exception e) {
      session.disconnect(Component.translatable("disconnect.loginFailedInfo", e.getMessage()), e);
    }
  }

  public void sendPacket(Packet packet) {
    session.send(packet);
  }

  public Optional<PlayerListEntry> getEntityProfile(UUID uuid) {
    return Optional.ofNullable(dataManager.playerListState().entries().get(uuid));
  }
}
