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

import com.github.steveice10.mc.protocol.data.ProtocolState;
import lombok.Getter;
import lombok.Setter;
import net.pistonmaster.soulfire.account.MinecraftAccount;
import net.pistonmaster.soulfire.account.service.OnlineJavaData;
import net.pistonmaster.soulfire.proxy.SWProxy;
import net.pistonmaster.soulfire.server.protocol.bot.BotControlAPI;
import net.pistonmaster.soulfire.server.protocol.bot.SessionDataManager;
import net.pistonmaster.soulfire.server.protocol.netty.ViaClientSession;

import java.io.IOException;

@Getter
public class BotConnectionMeta {
    private final MinecraftAccount minecraftAccount;
    private final ProtocolState targetState;
    private final SWSessionService sessionService;
    @Setter
    private SessionDataManager sessionDataManager;
    @Setter
    private BotControlAPI botControlAPI;

    public BotConnectionMeta(MinecraftAccount minecraftAccount, ProtocolState targetState, SWProxy proxyData) {
        this.minecraftAccount = minecraftAccount;
        this.targetState = targetState;
        this.sessionService = minecraftAccount.isPremiumJava() ? new SWSessionService(minecraftAccount.authType(), proxyData) : null;
    }

    public void joinServerId(String serverId, ViaClientSession session) {
        try {
            var javaData = (OnlineJavaData) minecraftAccount.accountData();
            sessionService.joinServer(javaData.profileId(), javaData.authToken(), serverId);
            session.logger().debug("Successfully sent mojang join request!");
        } catch (IOException e) {
            session.disconnect("Login failed: Authentication error: " + e.getMessage(), e);
        }
    }
}
