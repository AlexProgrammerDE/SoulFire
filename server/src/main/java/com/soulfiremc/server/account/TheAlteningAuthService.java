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

import com.soulfiremc.server.account.service.OnlineSimpleJavaData;
import com.soulfiremc.server.proxy.SFProxy;
import com.soulfiremc.server.util.ReactorHttpHelper;
import com.soulfiremc.server.util.UUIDHelper;
import com.soulfiremc.server.util.structs.GsonInstance;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.netty.ByteBufFlux;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class TheAlteningAuthService
  implements MCAuthService<String, TheAlteningAuthService.TheAlteningAuthData> {
  public static final TheAlteningAuthService INSTANCE = new TheAlteningAuthService();
  @SuppressWarnings("HttpUrlsUsage") // The Altening doesn't support encrypted HTTPS
  private static final URI AUTHENTICATE_ENDPOINT =
    URI.create("http://authserver.thealtening.com/authenticate");

  private static final String PASSWORD =
    "SoulFireIsCool"; // Password doesn't matter for The Altening

  private TheAlteningAuthService() {}

  @Override
  public CompletableFuture<MinecraftAccount> login(TheAlteningAuthData data, SFProxy proxyData, Executor executor) {
    var request = new AuthenticationRequest(data.altToken, PASSWORD, UUID.randomUUID().toString());
    return ReactorHttpHelper.createReactorClient(proxyData, true)
      .post()
      .uri(AUTHENTICATE_ENDPOINT)
      .send(ByteBufFlux.fromString(Flux.just(GsonInstance.GSON.toJson(request))))
      .responseSingle(
        (res, content) ->
          content
            .asString()
            .map(
              responseText -> {
                var response =
                  GsonInstance.GSON.fromJson(
                    responseText, AuthenticateRefreshResponse.class);

                return new MinecraftAccount(
                  AuthType.THE_ALTENING,
                  UUIDHelper.convertToDashed(response.selectedProfile().id()),
                  response.selectedProfile().name(),
                  new OnlineSimpleJavaData(response.accessToken(), -1));
              }))
      .toFuture();
  }

  @Override
  public TheAlteningAuthData createData(String data) {
    return new TheAlteningAuthData(data);
  }

  @Override
  public CompletableFuture<MinecraftAccount> refresh(MinecraftAccount account, SFProxy proxyData, Executor executor) {
    // TODO: Figure out The Altening refreshing
    return CompletableFuture.completedFuture(account);
  }

  @Override
  public boolean isExpired(MinecraftAccount account) {
    return false;
  }

  @Override
  public boolean isExpiredOrOutdated(MinecraftAccount account) {
    return false;
  }

  public record TheAlteningAuthData(String altToken) {}

  private record Agent(String name, int version) {}

  @SuppressWarnings("unused") // Used by GSON
  @RequiredArgsConstructor
  private static class AuthenticationRequest {
    private final Agent agent = new Agent("Minecraft", 1);
    private final String username;
    private final String password;
    private final String clientToken;
    private final boolean requestUser = true;
  }

  @SuppressWarnings("unused") // Used by GSON
  @Getter
  private static class AuthenticateRefreshResponse {
    private String accessToken;
    private ResponseGameProfile selectedProfile;
  }

  @SuppressWarnings("unused") // Used by GSON
  @Getter
  private static class ResponseGameProfile {
    private String id;
    private String name;
  }
}
