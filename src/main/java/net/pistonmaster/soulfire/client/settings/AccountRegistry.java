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
package net.pistonmaster.soulfire.client.settings;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.pistonmaster.soulfire.account.AuthType;
import net.pistonmaster.soulfire.account.MinecraftAccount;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class AccountRegistry {
    private final List<MinecraftAccount> accounts = new ArrayList<>();
    private final List<Runnable> loadHooks = new ArrayList<>();

    public void loadFromString(String data, AuthType authType) {
        var newAccounts = data.lines()
                .map(String::strip)
                .filter(line -> !line.isBlank())
                .distinct()
                .map(account -> fromStringSingle(account, authType))
                .toList();

        if (newAccounts.isEmpty()) {
            log.warn("No accounts found in the provided data!");
            return;
        }

        this.accounts.addAll(newAccounts);
        callLoadHooks();

        log.info("Loaded {} accounts!", newAccounts.size());
    }

    private MinecraftAccount fromStringSingle(String data, AuthType authType) {
        data = data.trim();

        if (data.isBlank()) {
            throw new IllegalArgumentException("Account cannot be empty!");
        }

        try {
            return authType.authService().createDataAndLogin(data, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<MinecraftAccount> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    public void setAccounts(List<MinecraftAccount> accounts) {
        this.accounts.clear();
        this.accounts.addAll(accounts);
    }

    public void callLoadHooks() {
        loadHooks.forEach(Runnable::run);
    }

    public void addLoadHook(Runnable hook) {
        loadHooks.add(hook);
    }

    public MinecraftAccount getAccount(String username, AuthType authType) {
        return accounts.stream()
                .filter(account -> account.authType() == authType)
                .filter(account -> account.username().equals(username))
                .findFirst()
                .orElse(null);
    }
}
