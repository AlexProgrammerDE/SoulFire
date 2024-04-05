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

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.packetlib.packet.Packet;
import com.soulfiremc.server.AttackManager;
import com.soulfiremc.server.SoulFireServer;
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
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.lenni0451.lambdaevents.LambdaManager;
import org.slf4j.Logger;

public record BotConnection(
  UUID connectionId,
  BotConnectionFactory factory,
  AttackManager attackManager,
  SoulFireServer soulFireServer,
  SettingsHolder settingsHolder,
  Logger logger,
  MinecraftProtocol protocol,
  ViaClientSession session,
  ResolveUtil.ResolvedAddress resolvedAddress,
  ExecutorManager executorManager,
  BotConnectionMeta meta,
  LambdaManager eventBus) {
  public CompletableFuture<?> connect() {
    return CompletableFuture.runAsync(
      () -> {
        attackManager.eventBus().call(new PreBotConnectEvent(this));
        session.connect(true);
      });
  }

  public boolean isOnline() {
    return session.isConnected();
  }

  public SessionDataManager sessionDataManager() {
    return meta.sessionDataManager();
  }

  public BotControlAPI botControl() {
    return meta.botControlAPI();
  }

  public void tick(long ticks) {
    session.tick(); // Ensure all packets are handled before ticking
    for (var i = 0; i < ticks; i++) {
      try {
        var tickHookState = TickHookContext.INSTANCE.get();
        tickHookState.clear();

        eventBus.call(new BotPreTickEvent(this));
        tickHookState.callHooks(TickHookContext.HookType.PRE_TICK);

        sessionDataManager().tick();
        botControl().tick();

        eventBus.call(new BotPostTickEvent(this));
        tickHookState.callHooks(TickHookContext.HookType.POST_TICK);
      } catch (Throwable t) {
        logger.error("Error while ticking bot!", t);
      }
    }
  }

  public GlobalTrafficShapingHandler getTrafficHandler() {
    return session.getFlag(SFProtocolConstants.TRAFFIC_HANDLER);
  }

  public CompletableFuture<?> gracefulDisconnect() {
    return CompletableFuture.runAsync(
      () -> {
        session.disconnect("Disconnect");

        // Give the server one second to handle the disconnect
        TimeUtil.waitTime(1, TimeUnit.SECONDS);

        // Shut down all executors
        executorManager.shutdownAll();

        // Let threads finish that didn't immediately interrupt
        TimeUtil.waitTime(100, TimeUnit.MILLISECONDS);
      });
  }

  public IdentifiedKey getIdentifiedKey() {
    throw new UnsupportedOperationException("Not implemented yet!");
  }

  public void sendPacket(Packet packet) {
    session.send(packet);
  }
}
