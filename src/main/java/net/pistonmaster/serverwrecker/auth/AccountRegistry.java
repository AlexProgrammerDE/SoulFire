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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public class AccountRegistry {
    public static final Logger LOGGER = LoggerFactory.getLogger(AccountRegistry.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final List<MinecraftAccount> accounts = new ArrayList<>();
    private final List<Runnable> loadHooks = new ArrayList<>();

    public void loadFromString(String file, AuthType authType) {
        var newAccounts = new ArrayList<MinecraftAccount>();

        file.lines()
                .filter(line -> !line.isBlank())
                .distinct()
                .map(account -> fromString(account, authType))
                .forEach(newAccounts::add);

        if (newAccounts.isEmpty()) {
            LOGGER.warn("No accounts found in the provided file!");
            return;
        }

        this.accounts.addAll(newAccounts);
        LOGGER.info("Loaded {} accounts!", newAccounts.size());

        callLoadHooks();
    }

    private MinecraftAccount fromString(String account, AuthType authType) {
        account = account.trim();

        if (account.isBlank()) {
            throw new IllegalArgumentException("Account cannot be empty!");
        }

        try {
            return authType.getAuthService().createDataAndLogin(account, null);
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
