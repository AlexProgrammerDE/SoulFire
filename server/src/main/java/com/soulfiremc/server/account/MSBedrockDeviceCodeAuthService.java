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

import com.soulfiremc.server.account.service.BedrockData;
import com.soulfiremc.server.proxy.SFProxy;
import com.soulfiremc.server.util.LenniHttpHelper;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCode;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public final class MSBedrockDeviceCodeAuthService
  implements MCAuthService<Consumer<StepMsaDeviceCode.MsaDeviceCode>, MSBedrockDeviceCodeAuthService.MSBedrockDeviceCodeAuthData> {
  public static final MSBedrockDeviceCodeAuthService INSTANCE = new MSBedrockDeviceCodeAuthService();

  private MSBedrockDeviceCodeAuthService() {}

  @Override
  public CompletableFuture<MinecraftAccount> login(MSBedrockDeviceCodeAuthData data, SFProxy proxyData, Executor executor) {
    return CompletableFuture.supplyAsync(() -> {
      var flow = MinecraftAuth.BEDROCK_DEVICE_CODE_LOGIN;
      try {
        return AuthHelpers.fromFullBedrockSession(AuthType.MICROSOFT_BEDROCK_DEVICE_CODE, flow, flow.getFromInput(
          LenniHttpHelper.createLenniMCAuthHttpClient(proxyData),
          new StepMsaDeviceCode.MsaDeviceCodeCallback(data.callback)));
      } catch (Exception e) {
        throw new CompletionException(e);
      }
    }, executor);
  }

  @Override
  public MSBedrockDeviceCodeAuthData createData(Consumer<StepMsaDeviceCode.MsaDeviceCode> data) {
    return new MSBedrockDeviceCodeAuthData(data);
  }

  @Override
  public CompletableFuture<MinecraftAccount> refresh(MinecraftAccount account, SFProxy proxyData, Executor executor) {
    return CompletableFuture.supplyAsync(() -> {
      var flow = MinecraftAuth.BEDROCK_DEVICE_CODE_LOGIN;
      var fullBedrockSession = flow.fromJson(((BedrockData) account.accountData()).authChain());
      try {
        return AuthHelpers.fromFullBedrockSession(AuthType.MICROSOFT_BEDROCK_DEVICE_CODE, flow, flow.refresh(
          LenniHttpHelper.createLenniMCAuthHttpClient(proxyData),
          fullBedrockSession));
      } catch (Exception e) {
        throw new CompletionException(e);
      }
    }, executor);
  }

  @Override
  public boolean isExpired(MinecraftAccount account) {
    var flow = MinecraftAuth.BEDROCK_DEVICE_CODE_LOGIN;
    return flow.fromJson(((BedrockData) account.accountData()).authChain()).isExpired();
  }

  @Override
  public boolean isExpiredOrOutdated(MinecraftAccount account) {
    var flow = MinecraftAuth.BEDROCK_DEVICE_CODE_LOGIN;
    return flow.fromJson(((BedrockData) account.accountData()).authChain()).isExpiredOrOutdated();
  }

  public record MSBedrockDeviceCodeAuthData(Consumer<StepMsaDeviceCode.MsaDeviceCode> callback) {}
}
