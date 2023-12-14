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
package net.pistonmaster.serverwrecker.account.service;

import net.pistonmaster.serverwrecker.account.AuthType;
import net.pistonmaster.serverwrecker.account.MinecraftAccount;
import net.pistonmaster.serverwrecker.proxy.SWProxy;

import java.io.IOException;

public final class SWOfflineAuthService implements MCAuthService<SWOfflineAuthService.OfflineAuthData> {
    @Override
    public MinecraftAccount login(OfflineAuthData data, SWProxy proxyData) throws IOException {
        return createAccount(data.username());
    }

    public static MinecraftAccount createAccount(String username) {
        return new MinecraftAccount(AuthType.OFFLINE, username, new OfflineJavaData(username), true);
    }

    @Override
    public OfflineAuthData createData(String data) {
        return new OfflineAuthData(data);
    }

    public record OfflineAuthData(String username) {
    }
}
