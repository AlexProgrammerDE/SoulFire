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

import com.soulfiremc.grpc.generated.MinecraftAccountProto;
import com.soulfiremc.settings.account.AuthType;
import com.soulfiremc.settings.account.MinecraftAccount;
import com.soulfiremc.settings.proxy.SFProxy;

import java.util.concurrent.CompletableFuture;

public sealed interface MCAuthService<T>
  permits SFBedrockMicrosoftAuthService,
  SFEasyMCAuthService,
  SFJavaMicrosoftAuthService,
  SFOfflineAuthService,
  SFTheAlteningAuthService {
  static MCAuthService<?> convertService(MinecraftAccountProto.AccountTypeProto service) {
    return switch (service) {
      case MICROSOFT_JAVA -> SFJavaMicrosoftAuthService.INSTANCE;
      case MICROSOFT_BEDROCK -> SFBedrockMicrosoftAuthService.INSTANCE;
      case THE_ALTENING -> SFTheAlteningAuthService.INSTANCE;
      case EASY_MC -> SFEasyMCAuthService.INSTANCE;
      case OFFLINE -> SFOfflineAuthService.INSTANCE;
      case UNRECOGNIZED -> throw new IllegalArgumentException("Unrecognized service");
    };
  }

  static MCAuthService<?> convertService(AuthType service) {
    return switch (service) {
      case MICROSOFT_JAVA -> SFJavaMicrosoftAuthService.INSTANCE;
      case MICROSOFT_BEDROCK -> SFBedrockMicrosoftAuthService.INSTANCE;
      case THE_ALTENING -> SFTheAlteningAuthService.INSTANCE;
      case EASY_MC -> SFEasyMCAuthService.INSTANCE;
      case OFFLINE -> SFOfflineAuthService.INSTANCE;
    };
  }

  CompletableFuture<MinecraftAccount> login(T data, SFProxy proxyData);

  T createData(String data);

  default CompletableFuture<MinecraftAccount> createDataAndLogin(String data, SFProxy proxyData) {
    return login(createData(data), proxyData);
  }

  CompletableFuture<MinecraftAccount> refresh(MinecraftAccount account, SFProxy proxyData);

  boolean isExpired(MinecraftAccount account);

  boolean isExpiredOrOutdated(MinecraftAccount account);
}
