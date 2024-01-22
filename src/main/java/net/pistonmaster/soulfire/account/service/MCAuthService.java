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
package net.pistonmaster.soulfire.account.service;

import net.pistonmaster.soulfire.account.MinecraftAccount;
import net.pistonmaster.soulfire.proxy.SWProxy;

import java.io.IOException;

public sealed interface MCAuthService<T> permits SWBedrockMicrosoftAuthService, SWEasyMCAuthService, SWJavaMicrosoftAuthService, SWOfflineAuthService, SWTheAlteningAuthService {
    MinecraftAccount login(T data, SWProxy proxyData) throws IOException;

    T createData(String data);

    default MinecraftAccount createDataAndLogin(String data, SWProxy proxyData) throws IOException {
        return login(createData(data), proxyData);
    }
}
