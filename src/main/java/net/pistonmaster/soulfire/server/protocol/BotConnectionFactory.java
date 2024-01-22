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
import com.github.steveice10.mc.protocol.data.ProtocolState;
import io.netty.channel.EventLoopGroup;
import net.lenni0451.lambdaevents.LambdaManager;
import net.lenni0451.lambdaevents.generator.ASMGenerator;
import net.pistonmaster.soulfire.account.MinecraftAccount;
import net.pistonmaster.soulfire.proxy.SWProxy;
import net.pistonmaster.soulfire.server.AttackManager;
import net.pistonmaster.soulfire.server.api.event.EventExceptionHandler;
import net.pistonmaster.soulfire.server.api.event.SoulFireBotEvent;
import net.pistonmaster.soulfire.server.api.event.attack.BotConnectionInitEvent;
import net.pistonmaster.soulfire.server.protocol.bot.BotControlAPI;
import net.pistonmaster.soulfire.server.protocol.bot.SessionDataManager;
import net.pistonmaster.soulfire.server.protocol.netty.ResolveUtil;
import net.pistonmaster.soulfire.server.protocol.netty.ViaClientSession;
import net.pistonmaster.soulfire.server.settings.BotSettings;
import net.pistonmaster.soulfire.server.settings.lib.SettingsHolder;
import org.slf4j.Logger;

import java.util.UUID;

public record BotConnectionFactory(AttackManager attackManager, ResolveUtil.ResolvedAddress resolvedAddress,
                                   SettingsHolder settingsHolder, Logger logger,
                                   MinecraftProtocol protocol, MinecraftAccount minecraftAccount,
                                   SWProxy proxyData, EventLoopGroup eventLoopGroup) {
    public BotConnection prepareConnection() {
        return prepareConnectionInternal(ProtocolState.LOGIN);
    }

    public BotConnection prepareConnectionInternal(ProtocolState targetState) {
        var meta = new BotConnectionMeta(minecraftAccount, targetState, proxyData);
        var session = new ViaClientSession(resolvedAddress.resolvedAddress(), logger, protocol, proxyData, settingsHolder, eventLoopGroup, meta);
        var botConnection = new BotConnection(UUID.randomUUID(), this, attackManager, attackManager.soulFireServer(),
                settingsHolder, logger, protocol, session, resolvedAddress, new ExecutorManager("SoulFire-Attack-" + attackManager.id()), meta,
                LambdaManager.basic(new ASMGenerator())
                        .setExceptionHandler(EventExceptionHandler.INSTANCE)
                        .setEventFilter((c, h) -> {
                            if (SoulFireBotEvent.class.isAssignableFrom(c)) {
                                return true;
                            } else {
                                throw new IllegalStateException("This event handler only accepts bot events");
                            }
                        }));

        var sessionDataManager = new SessionDataManager(botConnection);
        session.meta().sessionDataManager(sessionDataManager);
        session.meta().botControlAPI(new BotControlAPI(sessionDataManager));

        session.setConnectTimeout(settingsHolder.get(BotSettings.CONNECT_TIMEOUT));
        session.setReadTimeout(settingsHolder.get(BotSettings.READ_TIMEOUT));
        session.setWriteTimeout(settingsHolder.get(BotSettings.WRITE_TIMEOUT));

        session.addListener(new SWBaseListener(botConnection, targetState));
        session.addListener(new SWSessionListener(sessionDataManager, botConnection));

        attackManager.eventBus().call(new BotConnectionInitEvent(botConnection));

        return botConnection;
    }
}
