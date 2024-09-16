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

import com.soulfiremc.server.util.LenniHttpHelper;
import com.soulfiremc.settings.account.MinecraftAccount;
import com.soulfiremc.settings.account.service.OnlineChainJavaData;
import com.soulfiremc.settings.proxy.SFProxy;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCode;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class MSJavaDeviceCodeAuthService
  implements MCAuthService<Consumer<StepMsaDeviceCode.MsaDeviceCode>, MSJavaDeviceCodeAuthService.MSJavaDeviceCodeAuthData> {
  public static final MSJavaDeviceCodeAuthService INSTANCE = new MSJavaDeviceCodeAuthService();

  private MSJavaDeviceCodeAuthService() {}

  @Override
  public CompletableFuture<MinecraftAccount> login(MSJavaDeviceCodeAuthService.MSJavaDeviceCodeAuthData data, SFProxy proxyData) {
    return CompletableFuture.supplyAsync(() -> {
      var flow = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN;
      try {
        return AuthHelpers.fromFullJavaSession(flow, flow.getFromInput(
          LenniHttpHelper.createLenniMCAuthHttpClient(proxyData),
          new StepMsaDeviceCode.MsaDeviceCodeCallback(data.callback)));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Override
  public MSJavaDeviceCodeAuthService.MSJavaDeviceCodeAuthData createData(Consumer<StepMsaDeviceCode.MsaDeviceCode> data) {
    return new MSJavaDeviceCodeAuthService.MSJavaDeviceCodeAuthData(data);
  }

  @Override
  public CompletableFuture<MinecraftAccount> refresh(MinecraftAccount account, SFProxy proxyData) {
    return CompletableFuture.supplyAsync(() -> {
      var flow = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN;
      var fullJavaSession = flow.fromJson(((OnlineChainJavaData) account.accountData()).authChain());
      try {
        return AuthHelpers.fromFullJavaSession(flow, flow.refresh(
          LenniHttpHelper.createLenniMCAuthHttpClient(proxyData),
          fullJavaSession));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Override
  public boolean isExpired(MinecraftAccount account) {
    var flow = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN;
    return flow.fromJson(((OnlineChainJavaData) account.accountData()).authChain()).isExpired();
  }

  @Override
  public boolean isExpiredOrOutdated(MinecraftAccount account) {
    var flow = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN;
    return flow.fromJson(((OnlineChainJavaData) account.accountData()).authChain()).isExpiredOrOutdated();
  }

  public record MSJavaDeviceCodeAuthData(Consumer<StepMsaDeviceCode.MsaDeviceCode> callback) {}
}
