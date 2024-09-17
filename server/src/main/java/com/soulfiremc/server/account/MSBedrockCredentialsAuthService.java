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
import com.soulfiremc.settings.account.AuthType;
import com.soulfiremc.settings.account.MinecraftAccount;
import com.soulfiremc.settings.account.service.BedrockData;
import com.soulfiremc.settings.proxy.SFProxy;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.msa.StepCredentialsMsaCode;
import org.apache.commons.validator.routines.EmailValidator;

import java.util.concurrent.CompletableFuture;

public final class MSBedrockCredentialsAuthService
  implements MCAuthService<String, MSBedrockCredentialsAuthService.MSBedrockCredentialsAuthData> {
  public static final MSBedrockCredentialsAuthService INSTANCE = new MSBedrockCredentialsAuthService();

  private MSBedrockCredentialsAuthService() {}

  @Override
  public CompletableFuture<MinecraftAccount> login(MSBedrockCredentialsAuthData data, SFProxy proxyData) {
    return CompletableFuture.supplyAsync(() -> {
      var flow = MinecraftAuth.BEDROCK_CREDENTIALS_LOGIN;
      try {
        return AuthHelpers.fromFullBedrockSession(AuthType.MICROSOFT_BEDROCK_CREDENTIALS, flow, flow.getFromInput(
          LenniHttpHelper.createLenniMCAuthHttpClient(proxyData),
          new StepCredentialsMsaCode.MsaCredentials(data.email, data.password)));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Override
  public MSBedrockCredentialsAuthData createData(String data) {
    var split = data.split(":");

    if (split.length != 2) {
      throw new IllegalArgumentException("Invalid data!");
    }

    var email = split[0].trim();
    var password = split[1].trim();
    if (!EmailValidator.getInstance().isValid(email)) {
      throw new IllegalArgumentException("Invalid email!");
    }

    return new MSBedrockCredentialsAuthData(email, password);
  }

  @Override
  public CompletableFuture<MinecraftAccount> refresh(MinecraftAccount account, SFProxy proxyData) {
    return CompletableFuture.supplyAsync(() -> {
      var flow = MinecraftAuth.BEDROCK_CREDENTIALS_LOGIN;
      var fullBedrockSession = flow.fromJson(((BedrockData) account.accountData()).authChain());
      try {
        return AuthHelpers.fromFullBedrockSession(AuthType.MICROSOFT_BEDROCK_CREDENTIALS, flow, flow.refresh(
          LenniHttpHelper.createLenniMCAuthHttpClient(proxyData),
          fullBedrockSession));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Override
  public boolean isExpired(MinecraftAccount account) {
    var flow = MinecraftAuth.BEDROCK_CREDENTIALS_LOGIN;
    return flow.fromJson(((BedrockData) account.accountData()).authChain()).isExpired();
  }

  @Override
  public boolean isExpiredOrOutdated(MinecraftAccount account) {
    var flow = MinecraftAuth.BEDROCK_CREDENTIALS_LOGIN;
    return flow.fromJson(((BedrockData) account.accountData()).authChain()).isExpiredOrOutdated();
  }

  public record MSBedrockCredentialsAuthData(String email, String password) {}
}