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
import net.pistonmaster.serverwrecker.auth.service.BedrockData;
import net.pistonmaster.serverwrecker.auth.service.JavaData;

import java.util.UUID;

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
        return String.format("MinecraftAccount(authType=%s, username=%s, enabled=%s)", authType, username, enabled);
    }

    public boolean isPremiumJava() {
        return accountData != null && accountData instanceof JavaData;
    }

    public boolean isPremiumBedrock() {
        return accountData != null && accountData instanceof BedrockData;
    }

    public UUID getUniqueId() {
        if (accountData instanceof JavaData javaData) {
            return javaData.profileId();
        } else {
            return UUID.randomUUID(); // We are using a bedrock account, the uuid doesn't matter.
        }
    }
}
