/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.protocol;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.ProtocolState;
import io.netty.channel.EventLoopGroup;
import net.kyori.event.EventBus;
import net.pistonmaster.serverwrecker.AttackManager;
import net.pistonmaster.serverwrecker.api.event.ServerWreckerBotEvent;
import net.pistonmaster.serverwrecker.api.event.attack.BotConnectionInitEvent;
import net.pistonmaster.serverwrecker.auth.MinecraftAccount;
import net.pistonmaster.serverwrecker.protocol.bot.BotControlAPI;
import net.pistonmaster.serverwrecker.protocol.bot.SessionDataManager;
import net.pistonmaster.serverwrecker.protocol.netty.ViaClientSession;
import net.pistonmaster.serverwrecker.proxy.SWProxy;
import net.pistonmaster.serverwrecker.settings.BotSettings;
import net.pistonmaster.serverwrecker.settings.lib.SettingsHolder;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.util.UUID;

public record BotConnectionFactory(AttackManager attackManager, InetSocketAddress targetAddress,
                                   SettingsHolder settingsHolder, Logger logger,
                                   MinecraftProtocol protocol, MinecraftAccount minecraftAccount,
                                   SWProxy proxyData, EventLoopGroup eventLoopGroup) {
    public BotConnection prepareConnection() {
        return prepareConnectionInternal(ProtocolState.LOGIN);
    }

    public BotConnection prepareConnectionInternal(ProtocolState targetState) {
        var botSettings = settingsHolder.get(BotSettings.class);
        var meta = new BotConnectionMeta(minecraftAccount, targetState);
        var session = new ViaClientSession(targetAddress, logger, protocol, proxyData, settingsHolder, eventLoopGroup, meta);
        var botConnection = new BotConnection(UUID.randomUUID(), this, attackManager, attackManager.getServerWrecker(),
                settingsHolder, logger, protocol, session, new ExecutorManager("ServerWrecker-Attack-" + attackManager.getId()), meta,
                EventBus.create(ServerWreckerBotEvent.class));

        var sessionDataManager = new SessionDataManager(botConnection);
        session.getMeta().setSessionDataManager(sessionDataManager);
        session.getMeta().setBotControlAPI(new BotControlAPI(sessionDataManager, sessionDataManager.getBotMovementManager()));

        session.setConnectTimeout(botSettings.connectTimeout());
        session.setReadTimeout(botSettings.readTimeout());
        session.setWriteTimeout(botSettings.writeTimeout());

        session.addListener(new SWBaseListener(botConnection, targetState));
        session.addListener(new SWSessionListener(sessionDataManager, botConnection));

        attackManager.getEventBus().post(new BotConnectionInitEvent(botConnection));

        return botConnection;
    }
}
