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
package net.pistonmaster.soulfire.proxy;

import lombok.NonNull;

import java.net.InetSocketAddress;

public record SWProxy(@NonNull ProxyType type,
                      @NonNull String host, int port,
                      String username, String password,
                      boolean enabled) {
    public SWProxy {
        if (type == ProxyType.SOCKS4 && password != null) {
            throw new IllegalArgumentException("SOCKS4 does not support passwords!");
        } else if (username == null && password != null) {
            throw new IllegalArgumentException("Username must be set if password is set!");
        } else if (username != null && username.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank! (Should be null)");
        } else if (password != null && password.isBlank()) {
            throw new IllegalArgumentException("Password must not be blank! (Should be null)");
        }
    }

    public InetSocketAddress getInetSocketAddress() {
        return new InetSocketAddress(host, port);
    }
}
