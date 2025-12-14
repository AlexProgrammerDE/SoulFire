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
import com.soulfiremc.server.account.service.OnlineChainJavaData;
import net.raphimc.minecraftauth.bedrock.BedrockAuthManager;
import net.raphimc.minecraftauth.java.JavaAuthManager;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

public final class AuthHelpers {
  private AuthHelpers() {
  }

  public static MinecraftAccount fromBedrockAuthManager(AuthType authType, BedrockAuthManager authManager) {
    try {
      var mcChain = authManager.getMinecraftCertificateChain().getUpToDate();
      var deviceId = authManager.getDeviceId();
      var sessionKeyPair = authManager.getSessionKeyPair();
      var playFabId = authManager.getPlayFabToken().getUpToDate().getPlayFabId();
      return new MinecraftAccount(
        authType,
        mcChain.getIdentityUuid(),
        mcChain.getIdentityDisplayName(),
        new BedrockData(
          mcChain.getMojangJwt(),
          mcChain.getIdentityJwt(),
          (ECPublicKey) sessionKeyPair.getPublic(),
          (ECPrivateKey) sessionKeyPair.getPrivate(),
          deviceId,
          playFabId,
          BedrockAuthManager.toJson(authManager)));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static MinecraftAccount fromJavaAuthManager(AuthType authType, JavaAuthManager authManager) {
    try {
      var mcProfile = authManager.getMinecraftProfile().getUpToDate();
      var mcToken = authManager.getMinecraftToken().getUpToDate();
      return new MinecraftAccount(
        authType,
        mcProfile.getId(),
        mcProfile.getName(),
        new OnlineChainJavaData(
          mcToken.getToken(),
          mcToken.getExpireTimeMs(),
          JavaAuthManager.toJson(authManager)));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
