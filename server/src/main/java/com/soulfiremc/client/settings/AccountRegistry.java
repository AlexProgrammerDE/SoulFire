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
package com.soulfiremc.client.settings;

import com.soulfiremc.account.AuthType;
import com.soulfiremc.account.MinecraftAccount;
import com.soulfiremc.util.EnabledWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class AccountRegistry {
  private final List<EnabledWrapper<MinecraftAccount>> accounts = new ArrayList<>();
  private final List<Runnable> loadHooks = new ArrayList<>();

  public void loadFromString(String data, AuthType authType) {
    var newAccounts =
        data.lines()
            .map(String::strip)
            .filter(line -> !line.isBlank())
            .distinct()
            .map(account -> fromStringSingle(account, authType))
            .map(account -> new EnabledWrapper<>(true, account))
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
    } catch (Exception e) {
      log.error("Failed to load account from string", e);
      throw new RuntimeException(e);
    }
  }

  public List<EnabledWrapper<MinecraftAccount>> getAccounts() {
    return Collections.unmodifiableList(accounts);
  }

  public List<MinecraftAccount> getEnabledAccounts() {
    return accounts.stream().filter(EnabledWrapper::enabled).map(EnabledWrapper::value).toList();
  }

  public void setAccounts(List<EnabledWrapper<MinecraftAccount>> accounts) {
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
        .map(EnabledWrapper::value)
        .filter(account -> account.authType() == authType)
        .filter(account -> account.lastKnownName().equals(username))
        .findFirst()
        .orElse(null);
  }
}
