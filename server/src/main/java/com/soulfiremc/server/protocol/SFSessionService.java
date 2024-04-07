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

import com.soulfiremc.server.util.UUIDHelper;
import com.soulfiremc.settings.account.AuthType;
import com.soulfiremc.settings.proxy.SFProxy;
import com.soulfiremc.util.GsonInstance;
import com.soulfiremc.util.ReactorHttpHelper;
import io.netty.handler.codec.http.HttpStatusClass;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.netty.ByteBufFlux;

public class SFSessionService {
  private static final URI MOJANG_JOIN_URI =
    URI.create("https://sessionserver.mojang.com/session/minecraft/join");

  @SuppressWarnings("HttpUrlsUsage")
  private static final URI THE_ALTENING_JOIN_URI =
    URI.create("http://sessionserver.thealtening.com/session/minecraft/join");

  private static final URI EASYMC_JOIN_URI =
    URI.create("https://sessionserver.easymc.io/session/minecraft/join");
  private final URI joinEndpoint;
  private final SFProxy proxyData;

  public SFSessionService(AuthType authType, SFProxy proxyData) {
    this.joinEndpoint =
      switch (authType) {
        case MICROSOFT_JAVA -> MOJANG_JOIN_URI;
        case THE_ALTENING -> THE_ALTENING_JOIN_URI;
        case EASY_MC -> EASYMC_JOIN_URI;
        default -> throw new IllegalStateException("Unexpected value: " + authType);
      };
    this.proxyData = proxyData;
  }

  public static String getServerId(String base, PublicKey publicKey, SecretKey secretKey) {
    try {
      var digest = MessageDigest.getInstance("SHA-1");
      digest.update(base.getBytes(StandardCharsets.ISO_8859_1));
      digest.update(secretKey.getEncoded());
      digest.update(publicKey.getEncoded());
      return new BigInteger(digest.digest()).toString(16);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Server ID hash algorithm unavailable.", e);
    }
  }

  public void joinServer(UUID profileId, String authenticationToken, String serverId) {
    ReactorHttpHelper.createReactorClient(proxyData, true)
      .post()
      .uri(joinEndpoint)
      .send(
        ByteBufFlux.fromString(
          Flux.just(
            GsonInstance.GSON.toJson(
              new SFSessionService.JoinServerRequest(
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
