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

import com.soulfiremc.client.grpc.RPCClient;
import com.soulfiremc.grpc.generated.AuthRequest;
import com.soulfiremc.grpc.generated.MinecraftAccountProto;
import com.soulfiremc.settings.account.AuthType;
import com.soulfiremc.settings.account.MinecraftAccount;
import com.soulfiremc.settings.proxy.SFProxy;
import com.soulfiremc.util.EnabledWrapper;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenCustomHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSortedSet;
import it.unimi.dsi.fastutil.objects.ObjectSortedSets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import javax.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class AccountRegistry {
  private final ObjectSortedSet<EnabledWrapper<MinecraftAccount>> accounts =
    new ObjectLinkedOpenCustomHashSet<>(
      new Hash.Strategy<>() {
        @Override
        public int hashCode(EnabledWrapper<MinecraftAccount> obj) {
          if (obj == null) {
            return 0;
          }

          return obj.value().profileId().hashCode();
        }

        @Override
        public boolean equals(
          EnabledWrapper<MinecraftAccount> obj1, EnabledWrapper<MinecraftAccount> obj2) {
          if (obj1 == null || obj2 == null) {
            return false;
          }

          return obj1.value().profileId().equals(obj2.value().profileId());
        }
      });
  private final List<Runnable> loadHooks = new ArrayList<>();
  private final RPCClient rpcClient;

  public void loadFromString(String data, AuthType authType, SFProxy proxy) {
    try {
      var newAccounts =
        data.lines()
          .map(String::strip)
          .filter(Predicate.not(String::isBlank))
          .distinct()
          .map(account -> fromStringSingle(account, authType, proxy))
          .map(EnabledWrapper::defaultTrue)
          .toList();

      if (newAccounts.isEmpty()) {
        log.warn("No accounts found in the provided data!");
        return;
      }

      this.accounts.addAll(newAccounts);
      callLoadHooks();

      log.info("Loaded {} accounts!", newAccounts.size());
    } catch (Exception e) {
      log.error("Failed to load accounts from string!", e);
    }
  }

  private MinecraftAccount fromStringSingle(String data, AuthType authType, SFProxy proxy) {
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

  public Collection<EnabledWrapper<MinecraftAccount>> accounts() {
    return ObjectSortedSets.unmodifiable(accounts);
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

  public Optional<MinecraftAccount> getAccount(UUID profileId) {
    return accounts.stream()
      .map(EnabledWrapper::value)
      .filter(account -> account.profileId().equals(profileId))
      .findFirst();
  }
}
