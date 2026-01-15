/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.account;

import com.soulfiremc.server.account.service.OfflineJavaData;
import com.soulfiremc.server.proxy.SFProxy;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class OfflineAuthService
  implements MCAuthService<String, OfflineAuthService.OfflineAuthData> {
  public static final OfflineAuthService INSTANCE = new OfflineAuthService();

  private OfflineAuthService() {}

  public static MinecraftAccount createAccount(String username) {
    return new MinecraftAccount(
      AuthType.OFFLINE,
      OfflineJavaData.getOfflineUUID(username),
      username,
      new OfflineJavaData());
  }

  @Override
  public CompletableFuture<MinecraftAccount> login(OfflineAuthData data, @Nullable SFProxy proxyData, Executor executor) {
    return CompletableFuture.completedFuture(createAccount(data.username()));
  }

  @Override
  public OfflineAuthData createData(String data) {
    return new OfflineAuthData(data);
  }

  @Override
  public CompletableFuture<MinecraftAccount> refresh(MinecraftAccount account, @Nullable SFProxy proxyData, Executor executor) {
    return CompletableFuture.completedFuture(account);
  }

  @Override
  public boolean isExpired(MinecraftAccount account) {
    return false;
  }

  public record OfflineAuthData(String username) {}
}
