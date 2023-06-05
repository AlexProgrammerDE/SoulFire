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
import com.github.steveice10.packetlib.BuiltinFlags;
import com.github.steveice10.packetlib.ProxyInfo;
import net.pistonmaster.serverwrecker.ServerWrecker;
import net.pistonmaster.serverwrecker.api.ServerWreckerAPI;
import net.pistonmaster.serverwrecker.api.event.UnregisterCleanup;
import net.pistonmaster.serverwrecker.api.event.bot.PreBotConnectEvent;
import net.pistonmaster.serverwrecker.auth.JavaAccount;
import net.pistonmaster.serverwrecker.common.NullHelper;
import net.pistonmaster.serverwrecker.protocol.bot.SessionDataManager;
import net.pistonmaster.serverwrecker.protocol.netty.ViaClientSession;
import net.pistonmaster.serverwrecker.proxy.SWProxy;
import net.pistonmaster.serverwrecker.settings.BotSettings;
import net.pistonmaster.serverwrecker.settings.DevSettings;
import net.pistonmaster.serverwrecker.settings.lib.SettingsHolder;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

public record BotConnectionFactory(ServerWrecker serverWrecker, InetSocketAddress targetAddress,
                                   SettingsHolder settingsHolder, Logger logger,
                                   MinecraftProtocol protocol, JavaAccount javaAccount,
                                   SWProxy proxyData) {
    public CompletableFuture<BotConnection> connect() {
        return CompletableFuture.supplyAsync(this::connectInternal);
    }

    public BotConnection connectInternal() {
        BotSettings botSettings = settingsHolder.get(BotSettings.class);
        ViaClientSession session = new ViaClientSession(targetAddress, protocol,
                NullHelper.nullOrApply(proxyData,
                        data -> new ProxyInfo(ProxyInfo.Type.valueOf(data.type().name()), data.address(), data.username(), data.password())),
                settingsHolder);
        BotConnection botConnection = new BotConnection(this, serverWrecker, settingsHolder, logger, protocol, session, new BotConnectionMeta(javaAccount));
        session.setPostDisconnectHook(() -> botConnection.meta().getUnregisterCleanups().forEach(UnregisterCleanup::cleanup));

        SessionDataManager sessionDataManager = new SessionDataManager(botConnection);
        session.setFlag(SWProtocolConstants.SESSION_DATA_MANAGER, sessionDataManager);

        DevSettings devSettings = settingsHolder.get(DevSettings.class);
        session.setFlag(BuiltinFlags.PRINT_DEBUG, devSettings.debug());

        session.setConnectTimeout(botSettings.connectTimeout());
        session.setReadTimeout(botSettings.readTimeout());
        session.setWriteTimeout(botSettings.writeTimeout());

        session.addListener(new SWBaseListener(botConnection, ProtocolState.LOGIN));
        session.addListener(new SWSessionListener(sessionDataManager, botConnection));

        ServerWreckerAPI.postEvent(new PreBotConnectEvent(botConnection));

        session.connect(true);

        return botConnection;
    }
}
