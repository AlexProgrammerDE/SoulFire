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
package com.soulfiremc.server.protocol;

import com.soulfiremc.server.account.AuthType;
import com.soulfiremc.server.proxy.SFProxy;
import com.soulfiremc.server.util.ReactorHttpHelper;
import com.soulfiremc.server.util.UUIDHelper;
import com.soulfiremc.server.util.structs.GsonInstance;
import io.netty.handler.codec.http.HttpStatusClass;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.netty.ByteBufFlux;

import java.net.URI;
import java.util.UUID;

public class SFSessionService {
  private static final URI MOJANG_JOIN_URI =
    URI.create("https://sessionserver.mojang.com/session/minecraft/join");

  @SuppressWarnings("HttpUrlsUsage")
  private static final URI THE_ALTENING_JOIN_URI =
    URI.create("http://sessionserver.thealtening.com/session/minecraft/join");

  private final URI joinEndpoint;
  private final SFProxy proxyData;

  public SFSessionService(AuthType authType, SFProxy proxyData) {
    this.joinEndpoint =
      switch (authType) {
        case MICROSOFT_JAVA_CREDENTIALS, MICROSOFT_JAVA_DEVICE_CODE, MICROSOFT_JAVA_REFRESH_TOKEN -> MOJANG_JOIN_URI;
        case THE_ALTENING -> THE_ALTENING_JOIN_URI;
        case OFFLINE, MICROSOFT_BEDROCK_CREDENTIALS, MICROSOFT_BEDROCK_DEVICE_CODE -> throw new IllegalArgumentException("Invalid auth type");
      };
    this.proxyData = proxyData;
  }

  public void joinServer(UUID profileId, String authenticationToken, String serverId) {
    ReactorHttpHelper.createReactorClient(proxyData, true)
      .post()
      .uri(joinEndpoint)
      .send(
        ByteBufFlux.fromString(
          Flux.just(
            GsonInstance.GSON.toJson(
              new JoinServerRequest(
                authenticationToken,
                UUIDHelper.convertToNoDashes(profileId),
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
