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
import net.lenni0451.lambdaevents.LambdaManager;
import net.lenni0451.lambdaevents.generator.ASMGenerator;
import net.pistonmaster.serverwrecker.AttackManager;
import net.pistonmaster.serverwrecker.api.event.EventExceptionHandler;
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
        var meta = new BotConnectionMeta(minecraftAccount, targetState, proxyData);
        var session = new ViaClientSession(targetAddress, logger, protocol, proxyData, settingsHolder, eventLoopGroup, meta);
        var botConnection = new BotConnection(UUID.randomUUID(), this, attackManager, attackManager.getServerWreckerServer(),
                settingsHolder, logger, protocol, session, new ExecutorManager("ServerWrecker-Attack-" + attackManager.getId()), meta,
                LambdaManager.basic(new ASMGenerator())
                        .setExceptionHandler(EventExceptionHandler.INSTANCE)
                        .setEventFilter((c, h) -> {
                            if (ServerWreckerBotEvent.class.isAssignableFrom(c)) {
                                return true;
                            } else {
                                throw new IllegalStateException("This event handler only accepts bot events");
                            }
                        }));

        var sessionDataManager = new SessionDataManager(botConnection);
        session.getMeta().setSessionDataManager(sessionDataManager);
        session.getMeta().setBotControlAPI(new BotControlAPI(sessionDataManager, sessionDataManager.getBotMovementManager()));

        session.setConnectTimeout(settingsHolder.get(BotSettings.CONNECT_TIMEOUT));
        session.setReadTimeout(settingsHolder.get(BotSettings.READ_TIMEOUT));
        session.setWriteTimeout(settingsHolder.get(BotSettings.WRITE_TIMEOUT));

        session.addListener(new SWBaseListener(botConnection, targetState));
        session.addListener(new SWSessionListener(sessionDataManager, botConnection));

        attackManager.getEventBus().call(new BotConnectionInitEvent(botConnection));

        return botConnection;
    }
}
