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
package com.soulfiremc.server.account;

import com.soulfiremc.server.account.service.OnlineChainJavaData;
import com.soulfiremc.server.proxy.SFProxy;
import com.soulfiremc.server.util.LenniHttpHelper;
import net.raphimc.minecraftauth.java.JavaAuthManager;
import net.raphimc.minecraftauth.msa.model.MsaToken;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

public final class MSJavaRefreshTokenAuthService
  implements MCAuthService<String, MSJavaRefreshTokenAuthService.MSJavaRefreshTokenAuthData> {
  public static final MSJavaRefreshTokenAuthService INSTANCE = new MSJavaRefreshTokenAuthService();

  private MSJavaRefreshTokenAuthService() {}

  @Override
  public CompletableFuture<MinecraftAccount> login(MSJavaRefreshTokenAuthData data, @Nullable SFProxy proxyData, Executor executor) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        var authManager = JavaAuthManager.create(LenniHttpHelper.createLenniMCAuthHttpClient(proxyData))
          .login(data.refreshToken);
        return AuthHelpers.fromJavaAuthManager(AuthType.MICROSOFT_JAVA_REFRESH_TOKEN, authManager);
      } catch (Exception e) {
        throw new CompletionException(e);
      }
    }, executor);
  }

  @Override
  public MSJavaRefreshTokenAuthData createData(String data) {
    return new MSJavaRefreshTokenAuthData(data.strip());
  }

  @Override
  public CompletableFuture<MinecraftAccount> refresh(MinecraftAccount account, @Nullable SFProxy proxyData, Executor executor) {
    return CompletableFuture.supplyAsync(() -> {
      var httpClient = LenniHttpHelper.createLenniMCAuthHttpClient(proxyData);
      var authManager = JavaAuthManager.fromJson(httpClient, ((OnlineChainJavaData) account.accountData()).authChain());
      try {
        authManager.getMinecraftToken().refresh();
        authManager.getMinecraftProfile().refresh();
        return AuthHelpers.fromJavaAuthManager(AuthType.MICROSOFT_JAVA_REFRESH_TOKEN, authManager);
      } catch (Exception e) {
        throw new CompletionException(e);
      }
    }, executor);
  }

  @Override
  public boolean isExpired(MinecraftAccount account) {
    return ((OnlineChainJavaData) account.accountData()).tokenExpireAt() < System.currentTimeMillis();
  }

  @Override
  public boolean isExpiredOrOutdated(MinecraftAccount account) {
    // Consider outdated if within 5 minutes of expiration
    return ((OnlineChainJavaData) account.accountData()).tokenExpireAt() < System.currentTimeMillis() + 5 * 60 * 1000;
  }

  public record MSJavaRefreshTokenAuthData(String refreshToken) {}
}
