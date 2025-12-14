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
import net.raphimc.minecraftauth.bedrock.BedrockAuthManager;
import net.raphimc.minecraftauth.msa.model.MsaDeviceCode;
import net.raphimc.minecraftauth.msa.service.impl.DeviceCodeMsaAuthService;
import net.raphimc.viabedrock.protocol.data.ProtocolConstants;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public final class MSBedrockDeviceCodeAuthService
  implements MCAuthService<Consumer<MsaDeviceCode>, MSBedrockDeviceCodeAuthService.MSBedrockDeviceCodeAuthData> {
  public static final MSBedrockDeviceCodeAuthService INSTANCE = new MSBedrockDeviceCodeAuthService();

  private MSBedrockDeviceCodeAuthService() {}

  @Override
  public CompletableFuture<MinecraftAccount> login(MSBedrockDeviceCodeAuthData data, @Nullable SFProxy proxyData, Executor executor) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        var authManager = BedrockAuthManager.create(LenniHttpHelper.createLenniMCAuthHttpClient(proxyData), ProtocolConstants.BEDROCK_VERSION_NAME)
          .login(DeviceCodeMsaAuthService::new, data.callback);
        return AuthHelpers.fromBedrockAuthManager(AuthType.MICROSOFT_BEDROCK_DEVICE_CODE, authManager);
      } catch (Exception e) {
        throw new CompletionException(e);
      }
    }, executor);
  }

  @Override
  public MSBedrockDeviceCodeAuthData createData(Consumer<MsaDeviceCode> data) {
    return new MSBedrockDeviceCodeAuthData(data);
  }

  @Override
  public CompletableFuture<MinecraftAccount> refresh(MinecraftAccount account, @Nullable SFProxy proxyData, Executor executor) {
    return CompletableFuture.supplyAsync(() -> {
      var httpClient = LenniHttpHelper.createLenniMCAuthHttpClient(proxyData);
      var authManager = BedrockAuthManager.fromJson(httpClient, ProtocolConstants.BEDROCK_VERSION_NAME, ((BedrockData) account.accountData()).authChain());
      try {
        authManager.getMinecraftCertificateChain().refresh();
        authManager.getRealmsXstsToken().refresh();
        authManager.getPlayFabToken().refresh();
        return AuthHelpers.fromBedrockAuthManager(AuthType.MICROSOFT_BEDROCK_DEVICE_CODE, authManager);
      } catch (Exception e) {
        throw new CompletionException(e);
      }
    }, executor);
  }

  @Override
  public boolean isExpired(MinecraftAccount account) {
    // Bedrock tokens don't have a simple expiry, check the auth chain
    return false;
  }

  @Override
  public boolean isExpiredOrOutdated(MinecraftAccount account) {
    // Bedrock tokens should be refreshed periodically
    return false;
  }

  public record MSBedrockDeviceCodeAuthData(Consumer<MsaDeviceCode> callback) {}
}
