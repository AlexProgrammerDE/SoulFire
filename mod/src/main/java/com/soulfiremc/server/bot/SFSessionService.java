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
package com.soulfiremc.server.bot;

import com.soulfiremc.server.account.service.BedrockData;
import com.soulfiremc.server.account.service.OfflineJavaData;
import com.soulfiremc.server.account.service.OnlineChainJavaData;
import com.soulfiremc.server.util.ReactorHttpHelper;
import com.soulfiremc.server.util.structs.GsonInstance;
import com.soulfiremc.shared.UUIDHelper;
import io.netty.handler.codec.http.HttpStatusClass;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.netty.ByteBufFlux;

import java.net.URI;

@RequiredArgsConstructor
public final class SFSessionService {
  private static final URI MOJANG_JOIN_URI =
    URI.create("https://sessionserver.mojang.com/session/minecraft/join");
  private final BotConnection botConnection;

  public void joinServer(String serverId) {
    var account = botConnection.minecraftAccount();
    var joinEndpoint = switch (account.authType()) {
      case MICROSOFT_JAVA_CREDENTIALS, MICROSOFT_JAVA_DEVICE_CODE, MICROSOFT_JAVA_REFRESH_TOKEN -> MOJANG_JOIN_URI;
      case OFFLINE, MICROSOFT_BEDROCK_CREDENTIALS, MICROSOFT_BEDROCK_DEVICE_CODE -> throw new IllegalArgumentException("Server does not support auth type: " + account.authType());
    };
    var authenticationToken = switch (account.accountData()) {
      case OnlineChainJavaData onlineChainJavaData -> onlineChainJavaData.authToken();
      case OfflineJavaData ignored -> throw new IllegalArgumentException("Invalid auth type: " + account.authType());
      case BedrockData ignored -> throw new IllegalArgumentException("Invalid auth type: " + account.authType());
    };

    ReactorHttpHelper.createReactorClient(botConnection.proxy(), true)
      .post()
      .uri(joinEndpoint)
      .send(
        ByteBufFlux.fromString(
          Flux.just(
            GsonInstance.GSON.toJson(
              new JoinServerRequest(
                authenticationToken,
                UUIDHelper.convertToNoDashes(botConnection.accountProfileId()),
                serverId)))))
      .responseSingle(
        (res, content) -> {
          if (res.status().codeClass() != HttpStatusClass.SUCCESS) {
            throw new RuntimeException("Failed to join server: " + res.status().code());
          }

          return content.asString();
        })
      .block();
  }

  @SuppressWarnings("unused") // Used by GSON
  @AllArgsConstructor
  private static class JoinServerRequest {
    private String accessToken;
    private String selectedProfile;
    private String serverId;
  }
}
