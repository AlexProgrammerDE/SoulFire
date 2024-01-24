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
package net.pistonmaster.soulfire.server.protocol;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import net.lenni0451.lambdaevents.LambdaManager;
import net.pistonmaster.soulfire.server.AttackManager;
import net.pistonmaster.soulfire.server.SoulFireServer;
import net.pistonmaster.soulfire.server.api.event.attack.PreBotConnectEvent;
import net.pistonmaster.soulfire.server.protocol.bot.BotControlAPI;
import net.pistonmaster.soulfire.server.protocol.bot.SessionDataManager;
import net.pistonmaster.soulfire.server.protocol.netty.ResolveUtil;
import net.pistonmaster.soulfire.server.protocol.netty.ViaClientSession;
import net.pistonmaster.soulfire.server.settings.lib.SettingsHolder;
import net.pistonmaster.soulfire.server.util.TimeUtil;
import org.slf4j.Logger;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public record BotConnection(UUID connectionId, BotConnectionFactory factory, AttackManager attackManager,
                            SoulFireServer soulFireServer, SettingsHolder settingsHolder,
                            Logger logger, MinecraftProtocol protocol, ViaClientSession session,
                            ResolveUtil.ResolvedAddress resolvedAddress,
                            ExecutorManager executorManager, BotConnectionMeta meta,
                            LambdaManager eventBus) {
    public CompletableFuture<?> connect() {
        return CompletableFuture.runAsync(() -> {
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

    public void tick(long ticks, float partialTicks) {
        session.tick(); // Ensure all packets are handled before ticking
        for (var i = 0; i < ticks; i++) {
            try {
                sessionDataManager().tick();
            } catch (Throwable t) {
                logger.error("Error while ticking bot!", t);
            }
        }
    }

    public GlobalTrafficShapingHandler getTrafficHandler() {
        return session.getFlag(SWProtocolConstants.TRAFFIC_HANDLER);
    }

    public CompletableFuture<?> gracefulDisconnect() {
        return CompletableFuture.runAsync(() -> {
            session.disconnect("Disconnect");

            // Give the server one second to handle the disconnect
            TimeUtil.waitTime(1, TimeUnit.SECONDS);

            // Shut down all executors
            executorManager.shutdownAll();

            // Let threads finish that didn't immediately interrupt
            TimeUtil.waitTime(100, TimeUnit.MILLISECONDS);
        });
    }
}
