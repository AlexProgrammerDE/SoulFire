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

import com.soulfiremc.grpc.generated.AccountTypeCredentials;
import com.soulfiremc.grpc.generated.AccountTypeDeviceCode;
import com.soulfiremc.grpc.generated.MinecraftAccountProto;
import com.soulfiremc.server.proxy.SFProxy;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCode;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public sealed interface MCAuthService<I, T>
  permits MSBedrockCredentialsAuthService, MSBedrockDeviceCodeAuthService, MSJavaCredentialsAuthService, MSJavaDeviceCodeAuthService, MSJavaRefreshTokenAuthService, OfflineAuthService, TheAlteningAuthService {
  static MCAuthService<String, ?> convertService(AccountTypeCredentials service) {
    return switch (service) {
      case MICROSOFT_JAVA_CREDENTIALS -> MSJavaCredentialsAuthService.INSTANCE;
      case MICROSOFT_BEDROCK_CREDENTIALS -> MSBedrockCredentialsAuthService.INSTANCE;
      case THE_ALTENING -> TheAlteningAuthService.INSTANCE;
      case OFFLINE -> OfflineAuthService.INSTANCE;
      case MICROSOFT_JAVA_REFRESH_TOKEN -> MSJavaRefreshTokenAuthService.INSTANCE;
      case UNRECOGNIZED -> throw new IllegalArgumentException("Unrecognized service");
    };
  }

  static MCAuthService<Consumer<StepMsaDeviceCode.MsaDeviceCode>, ?> convertService(AccountTypeDeviceCode service) {
    return switch (service) {
      case MICROSOFT_JAVA_DEVICE_CODE -> MSJavaDeviceCodeAuthService.INSTANCE;
      case MICROSOFT_BEDROCK_DEVICE_CODE -> MSBedrockDeviceCodeAuthService.INSTANCE;
      case UNRECOGNIZED -> throw new IllegalArgumentException("Unrecognized service");
    };
  }

  static MCAuthService<?, ?> convertService(MinecraftAccountProto.AccountTypeProto service) {
    return switch (service) {
      case MICROSOFT_JAVA_CREDENTIALS -> MSJavaCredentialsAuthService.INSTANCE;
      case MICROSOFT_BEDROCK_CREDENTIALS -> MSBedrockCredentialsAuthService.INSTANCE;
      case THE_ALTENING -> TheAlteningAuthService.INSTANCE;
      case OFFLINE -> OfflineAuthService.INSTANCE;
      case MICROSOFT_JAVA_DEVICE_CODE -> MSJavaDeviceCodeAuthService.INSTANCE;
      case MICROSOFT_BEDROCK_DEVICE_CODE -> MSBedrockDeviceCodeAuthService.INSTANCE;
      case MICROSOFT_JAVA_REFRESH_TOKEN -> MSJavaRefreshTokenAuthService.INSTANCE;
      case UNRECOGNIZED -> throw new IllegalArgumentException("Unrecognized service");
    };
  }

  static MCAuthService<?, ?> convertService(AuthType service) {
    return switch (service) {
      case MICROSOFT_JAVA_CREDENTIALS -> MSJavaCredentialsAuthService.INSTANCE;
      case MICROSOFT_BEDROCK_CREDENTIALS -> MSBedrockCredentialsAuthService.INSTANCE;
      case MICROSOFT_JAVA_DEVICE_CODE -> MSJavaDeviceCodeAuthService.INSTANCE;
      case MICROSOFT_BEDROCK_DEVICE_CODE -> MSBedrockDeviceCodeAuthService.INSTANCE;
      case THE_ALTENING -> TheAlteningAuthService.INSTANCE;
      case OFFLINE -> OfflineAuthService.INSTANCE;
      case MICROSOFT_JAVA_REFRESH_TOKEN -> MSJavaRefreshTokenAuthService.INSTANCE;
    };
  }

  CompletableFuture<MinecraftAccount> login(T data, SFProxy proxyData, Executor executor);

  T createData(I data);

  default CompletableFuture<MinecraftAccount> createDataAndLogin(I data, SFProxy proxyData, Executor executor) {
    return login(createData(data), proxyData, executor);
  }

  CompletableFuture<MinecraftAccount> refresh(MinecraftAccount account, SFProxy proxyData, Executor executor);

  boolean isExpired(MinecraftAccount account);

  boolean isExpiredOrOutdated(MinecraftAccount account);
}
