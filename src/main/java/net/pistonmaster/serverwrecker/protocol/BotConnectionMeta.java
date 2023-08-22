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

import com.github.steveice10.mc.auth.exception.request.InvalidCredentialsException;
import com.github.steveice10.mc.auth.exception.request.RequestException;
import com.github.steveice10.mc.auth.exception.request.ServiceUnavailableException;
import com.github.steveice10.mc.protocol.data.ProtocolState;
import lombok.Getter;
import lombok.Setter;
import net.pistonmaster.serverwrecker.auth.MinecraftAccount;
import net.pistonmaster.serverwrecker.auth.service.JavaData;
import net.pistonmaster.serverwrecker.protocol.bot.BotControlAPI;
import net.pistonmaster.serverwrecker.protocol.bot.SessionDataManager;
import net.pistonmaster.serverwrecker.protocol.netty.ViaClientSession;

@Getter
public class BotConnectionMeta {
    private final MinecraftAccount minecraftAccount;
    private final ProtocolState targetState;
    private final SWSessionService sessionService;
    @Setter
    private SessionDataManager sessionDataManager;
    @Setter
    private BotControlAPI botControlAPI;

    public BotConnectionMeta(MinecraftAccount minecraftAccount, ProtocolState targetState) {
        this.minecraftAccount = minecraftAccount;
        this.targetState = targetState;
        this.sessionService = minecraftAccount.isPremiumJava() ? new SWSessionService(minecraftAccount.authType()) : null;
    }

    public void joinServerId(String serverId, ViaClientSession session) {
        try {
            JavaData javaData = (JavaData) minecraftAccount.accountData();
            sessionService.joinServer(javaData.profileId(), javaData.authToken(), serverId);
            session.getLogger().info("Successfully sent mojang join request!");
        } catch (ServiceUnavailableException e) {
            session.disconnect("Login failed: Authentication service unavailable.", e);
        } catch (InvalidCredentialsException e) {
            session.disconnect("Login failed: Invalid login session.", e);
        } catch (RequestException e) {
            session.disconnect("Login failed: Authentication error: " + e.getMessage(), e);
        }
    }
}
