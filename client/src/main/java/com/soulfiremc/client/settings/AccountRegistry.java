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
import com.soulfiremc.client.grpc.RPCClient;
import com.soulfiremc.grpc.generated.AuthRequest;
import com.soulfiremc.grpc.generated.MinecraftAccountProto;
import com.soulfiremc.proxy.SFProxy;
import com.soulfiremc.util.EnabledWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class AccountRegistry {
  private final List<EnabledWrapper<MinecraftAccount>> accounts = new ArrayList<>();
  private final List<Runnable> loadHooks = new ArrayList<>();
  private final RPCClient rpcClient;

  public void loadFromString(String data, AuthType authType, SFProxy proxy) {
    var newAccounts =
        data.lines()
            .map(String::strip)
            .filter(line -> !line.isBlank())
            .distinct()
            .map(account -> fromStringSingle(account, authType, proxy))
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

  private MinecraftAccount fromStringSingle(String data, AuthType authType, SFProxy proxy) {
    data = data.trim();

    if (data.isBlank()) {
      throw new IllegalArgumentException("Account cannot be empty!");
    }

    try {
      var request =
          AuthRequest.newBuilder()
              .setService(MinecraftAccountProto.AccountTypeProto.valueOf(authType.name()))
              .setPayload(data);

      if (proxy != null) {
        request.setProxy(proxy.toProto());
      }

      return MinecraftAccount.fromProto(
          rpcClient.mcAuthServiceBlocking().login(request.build()).getAccount());
    } catch (Exception e) {
      log.error("Failed to load account from string", e);
      throw new RuntimeException(e);
    }
  }

  public List<EnabledWrapper<MinecraftAccount>> getAccounts() {
    return Collections.unmodifiableList(accounts);
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
