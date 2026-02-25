/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.account;

import com.google.gson.JsonObject;
import com.soulfiremc.server.account.service.OnlineChainJavaData;
import com.soulfiremc.server.proxy.SFProxy;
import com.soulfiremc.server.util.structs.GsonInstance;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

public final class MSJavaAccessTokenAuthService
  implements MCAuthService<String, MSJavaAccessTokenAuthService.MSJavaAccessTokenAuthData> {
  public static final MSJavaAccessTokenAuthService INSTANCE = new MSJavaAccessTokenAuthService();
  private static final String PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";
  private static final long EXPIRY_SKEW_SECONDS = 30;

  private MSJavaAccessTokenAuthService() {}

  @Override
  public CompletableFuture<MinecraftAccount> login(MSJavaAccessTokenAuthData data, @Nullable SFProxy proxyData, Executor executor) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        var profileJson = fetchProfile(data.accessToken);

        var id = profileJson.get("id").getAsString();
        // Convert from undashed to dashed UUID format
        var profileId = UUID.fromString(
          id.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
            "$1-$2-$3-$4-$5"));
        var name = profileJson.get("name").getAsString();

        // We don't have a full auth chain, so we store minimal data.
        // The access token is stored so it can be used for server joining.
        var authChain = new JsonObject();
        authChain.addProperty("_saveVersion", 1);
        authChain.addProperty("_accessToken", data.accessToken);
        authChain.addProperty("_profileId", profileId.toString());
        authChain.addProperty("_profileName", name);
        authChain.addProperty("_accessTokenOnly", true);

        return new MinecraftAccount(
          AuthType.MICROSOFT_JAVA_ACCESS_TOKEN,
          profileId,
          name,
          new OnlineChainJavaData(authChain),
          Map.of(),
          Map.of());
      } catch (Exception e) {
        throw new CompletionException(e);
      }
    }, executor);
  }

  private JsonObject fetchProfile(String accessToken) throws Exception {
    var url = URI.create(PROFILE_URL).toURL();
    var connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("GET");
    connection.setRequestProperty("Authorization", "Bearer " + accessToken);
    connection.setRequestProperty("Accept", "application/json");
    connection.setConnectTimeout(10_000);
    connection.setReadTimeout(10_000);

    var statusCode = connection.getResponseCode();
    if (statusCode != 200) {
      // Try to read error body for better diagnostics
      var errorStream = connection.getErrorStream();
      var errorBody = "";
      if (errorStream != null) {
        try (var reader = new BufferedReader(new InputStreamReader(errorStream, StandardCharsets.UTF_8))) {
          errorBody = reader.lines().collect(Collectors.joining("\n"));
        }
      }
      throw new IllegalStateException("Failed to fetch Minecraft profile: HTTP " + statusCode + " " + errorBody);
    }

    try (var reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
      var body = reader.lines().collect(Collectors.joining("\n"));
      return GsonInstance.GSON.fromJson(body, JsonObject.class);
    } finally {
      connection.disconnect();
    }
  }

  @Override
  public MSJavaAccessTokenAuthData createData(String data) {
    return new MSJavaAccessTokenAuthData(data.strip());
  }

  @Override
  public CompletableFuture<MinecraftAccount> refresh(MinecraftAccount account, @Nullable SFProxy proxyData, Executor executor) {
    return CompletableFuture.failedFuture(
      new UnsupportedOperationException("Access token accounts cannot be refreshed. Please re-import the account with a new access token."));
  }

  @Override
  public boolean isExpired(MinecraftAccount account) {
    var accessToken = ((OnlineChainJavaData) account.accountData()).getAccessToken(null);
    var expSeconds = getJwtExpSeconds(accessToken);
    if (expSeconds == null) {
      return false;
    }
    var nowSeconds = System.currentTimeMillis() / 1000;
    return nowSeconds >= (expSeconds - EXPIRY_SKEW_SECONDS);
  }

  private static @Nullable Long getJwtExpSeconds(String accessToken) {
    var parts = accessToken.split("\\.");
    if (parts.length < 2) {
      return null;
    }

    try {
      var payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
      var payload = GsonInstance.GSON.fromJson(payloadJson, JsonObject.class);
      if (payload == null || !payload.has("exp")) {
        return null;
      }
      return payload.get("exp").getAsLong();
    } catch (Exception _) {
      return null;
    }
  }

  public record MSJavaAccessTokenAuthData(String accessToken) {}
}
