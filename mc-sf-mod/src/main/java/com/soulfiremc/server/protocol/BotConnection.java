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
import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.api.event.bot.PreBotConnectEvent;
import com.soulfiremc.server.api.metadata.MetadataHolder;
import com.soulfiremc.server.proxy.SFProxy;
import com.soulfiremc.server.settings.lib.InstanceSettingsSource;
import com.soulfiremc.server.util.SFHelpers;
import com.soulfiremc.server.util.log4j.SFLogAppender;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import io.netty.channel.EventLoopGroup;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Getter
public final class BotConnection {
  public static final ThreadLocal<BotConnection> CURRENT = new ThreadLocal<>();
  private final List<Runnable> shutdownHooks = new CopyOnWriteArrayList<>();
  private final Queue<Runnable> preTickHooks = new ConcurrentLinkedQueue<>();
  private final MetadataHolder metadata = new MetadataHolder();
  private final SoulFireScheduler scheduler;
  private final BotConnectionFactory factory;
  private final InstanceManager instanceManager;
  private final InstanceSettingsSource settingsSource;
  private final MinecraftAccount minecraftAccount;
  private final UUID accountProfileId;
  private final String accountName;
  private final ProtocolVersion protocolVersion;
  private final SFSessionService sessionService;
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
    MinecraftAccount minecraftAccount,
    ProtocolVersion protocolVersion,
    @Nullable
    SFProxy proxyData,
    EventLoopGroup eventLoopGroup) {
    this.factory = factory;
    this.instanceManager = instanceManager;
    this.settingsSource = settingsSource;
    this.minecraftAccount = minecraftAccount;
    this.accountProfileId = minecraftAccount.profileId();
    this.accountName = minecraftAccount.lastKnownName();
    this.runnableWrapper = instanceManager.runnableWrapper().with(new BotRunnableWrapper(this));
    this.scheduler = new SoulFireScheduler(runnableWrapper);
    this.protocolVersion = protocolVersion;
    this.sessionService =
      minecraftAccount.isPremiumJava()
        ? new SFSessionService(minecraftAccount.authType(), proxyData)
        : null;
  }

  public CompletableFuture<?> connect() {
    return scheduler.runAsync(
      () -> {
        SoulFireAPI.postEvent(new PreBotConnectEvent(this));

      });
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

  private record BotRunnableWrapper(BotConnection botConnection) implements SoulFireScheduler.RunnableWrapper {
    @Override
    public Runnable wrap(Runnable runnable) {
      return () -> {
        try (
          var ignored1 = SFHelpers.smartThreadLocalCloseable(CURRENT, botConnection);
          var ignored2 = SFHelpers.smartMDCCloseable(SFLogAppender.SF_BOT_ACCOUNT_ID, botConnection.accountProfileId().toString());
          var ignored3 = SFHelpers.smartMDCCloseable(SFLogAppender.SF_BOT_ACCOUNT_NAME, botConnection.accountName())) {
          runnable.run();
        }
      };
    }
  }
}
