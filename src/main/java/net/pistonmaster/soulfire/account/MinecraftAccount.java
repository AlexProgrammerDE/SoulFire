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
package net.pistonmaster.soulfire.account;

import lombok.NonNull;
import net.pistonmaster.soulfire.account.service.AccountData;
import net.pistonmaster.soulfire.account.service.BedrockData;
import net.pistonmaster.soulfire.account.service.OnlineJavaData;

import java.util.UUID;

public record MinecraftAccount(@NonNull AuthType authType, @NonNull String username, @NonNull AccountData accountData,
                               boolean enabled) {
    @Override
    public String toString() {
        return String.format("MinecraftAccount(authType=%s, username=%s, enabled=%s)", authType, username, enabled);
    }

    public boolean isPremiumJava() {
        return accountData instanceof OnlineJavaData;
    }

    public boolean isPremiumBedrock() {
        return accountData instanceof BedrockData;
    }

    public UUID uniqueId() {
        return accountData.profileId();
    }
}
