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
import net.pistonmaster.serverwrecker.api.event.UnregisterCleanup;
import net.pistonmaster.serverwrecker.auth.JavaAccount;
import net.pistonmaster.serverwrecker.protocol.netty.ViaClientSession;

import java.util.ArrayList;
import java.util.List;

@Getter
public class BotConnectionMeta {
    private final List<UnregisterCleanup> unregisterCleanups = new ArrayList<>();
    private final JavaAccount javaAccount;
    private final ProtocolState targetState;
    private final SWSessionService sessionService;

    public BotConnectionMeta(JavaAccount javaAccount, ProtocolState targetState) {
        this.javaAccount = javaAccount;
        this.targetState = targetState;
        this.sessionService = javaAccount.isPremium() ? new SWSessionService(javaAccount.authType()) : null;
    }

    public void joinServerId(String serverId, ViaClientSession session) {
        try {
            sessionService.joinServer(javaAccount.profileId(), javaAccount.authToken(), serverId);
        } catch (ServiceUnavailableException e) {
            session.disconnect("Login failed: Authentication service unavailable.", e);
        } catch (InvalidCredentialsException e) {
            session.disconnect("Login failed: Invalid login session.", e);
        } catch (RequestException e) {
            session.disconnect("Login failed: Authentication error: " + e.getMessage(), e);
        }
    }
}
