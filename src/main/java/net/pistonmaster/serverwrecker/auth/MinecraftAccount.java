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
package net.pistonmaster.serverwrecker.auth;

import net.pistonmaster.serverwrecker.auth.service.AccountData;
import net.pistonmaster.serverwrecker.auth.service.JavaData;

public record MinecraftAccount(AuthType authType, String username, AccountData accountData, boolean enabled) {
    public MinecraftAccount {
        if (username == null) {
            throw new IllegalArgumentException("Username cannot be null!");
        }
    }

    public MinecraftAccount(String username) {
        this(AuthType.OFFLINE, username, null, true);
    }

    @Override
    public String toString() {
        return String.format("JavaAccount(username=%s)", username);
    }

    public boolean isPremiumJava() {
        return accountData instanceof JavaData;
    }
}
