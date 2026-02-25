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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.soulfiremc.builddata.BuildData;
import com.soulfiremc.server.proxy.SFProxy;
import com.soulfiremc.server.util.LenniHttpHelper;
import com.soulfiremc.server.util.ReactorHttpHelper;
import com.soulfiremc.server.util.structs.GsonInstance;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpStatusClass;
import net.raphimc.minecraftauth.java.JavaAuthManager;
import org.checkerframework.checker.nullness.qual.Nullable;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

public final class MSJavaCookiesAuthService
  implements MCAuthService<String, MSJavaCookiesAuthService.MSJavaCookiesAuthData> {
  public static final MSJavaCookiesAuthService INSTANCE = new MSJavaCookiesAuthService();
  private static final String CLIENT_ID = "00000000402b5328";
  private static final String REDIRECT_URI = "https://login.live.com/oauth20_desktop.srf";
  private static final String SCOPE = "service::user.auth.xboxlive.com::MBI_SSL";
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private MSJavaCookiesAuthService() {}

  @Override
  public CompletableFuture<MinecraftAccount> login(MSJavaCookiesAuthData data, @Nullable SFProxy proxyData, Executor executor) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        var refreshToken = exchangeCookieInputForRefreshToken(data.cookieInput, proxyData);
        var authManager = JavaAuthManager.create(LenniHttpHelper.client(proxyData))
          .login(refreshToken);
        return AuthHelpers.fromJavaAuthManager(AuthType.MICROSOFT_JAVA_REFRESH_TOKEN, authManager, null);
      } catch (Exception e) {
        throw new CompletionException(e);
      }
    }, executor);
  }

  @Override
  public MSJavaCookiesAuthData createData(String data) {
    var t = data.strip();
    if (t.isEmpty()) {
      throw new IllegalArgumentException("Cookie import failed: empty input");
    }
    return new MSJavaCookiesAuthData(t);
  }

  @Override
  public CompletableFuture<MinecraftAccount> refresh(MinecraftAccount account, @Nullable SFProxy proxyData, Executor executor) {
    return MSJavaRefreshTokenAuthService.INSTANCE.refresh(account, proxyData, executor);
  }

  @Override
  public boolean isExpired(MinecraftAccount account) {
    return MSJavaRefreshTokenAuthService.INSTANCE.isExpired(account);
  }

  private static String exchangeCookieInputForRefreshToken(String cookieInput, @Nullable SFProxy proxyData) {
    var cookieHeader = cookieInputToCookieHeader(cookieInput);

    var verifier = generatePkceVerifier();
    var challenge = pkceS256Challenge(verifier);

    var authorizeUri = URI.create(
      "https://login.live.com/oauth20_authorize.srf"
        + "?client_id=" + urlEncode(CLIENT_ID)
        + "&response_type=code"
        + "&response_mode=query"
        + "&redirect_uri=" + urlEncode(REDIRECT_URI)
        + "&scope=" + urlEncode(SCOPE)
        + "&prompt=none"
        + "&code_challenge=" + urlEncode(challenge)
        + "&code_challenge_method=S256"
    );

    var location = ReactorHttpHelper.createReactorClient(proxyData, false)
      .responseTimeout(Duration.ofSeconds(20))
      .followRedirect(false)
      .headers(h -> {
        h.set(HttpHeaderNames.USER_AGENT, "SoulFire/" + BuildData.VERSION);
        h.set(HttpHeaderNames.COOKIE, cookieHeader);
      })
      .get()
      .uri(authorizeUri)
      .responseSingle((res, content) -> {
        if (res.status().codeClass() != HttpStatusClass.REDIRECTION) {
          return content.asString(StandardCharsets.UTF_8)
            .defaultIfEmpty("")
            .flatMap(_ -> Mono.error(
              new IllegalStateException("Cookie import failed: unexpected authorize response " + res.status().code())
            ));
        }
        return Mono.justOrEmpty(res.responseHeaders().get(HttpHeaderNames.LOCATION));
      })
      .block();

    if (location == null || location.isEmpty()) {
      throw new IllegalStateException("Cookie import failed: missing authorize redirect");
    }

    var redirect = URI.create(location);
    var expectedRedirect = URI.create(REDIRECT_URI);
    if (redirect.getHost() == null
      || !expectedRedirect.getHost().equalsIgnoreCase(redirect.getHost())
      || !expectedRedirect.getPath().equals(redirect.getPath())) {
      throw new IllegalStateException("Cookie import failed: unexpected redirect to " + redirect.getHost() + redirect.getPath());
    }
    var params = parseQueryParams(redirect.getRawQuery());
    var error = params.get("error");
    if (error != null && !error.isEmpty()) {
      var description = params.get("error_description");
      throw new IllegalStateException("Cookie import failed: authorize error " + error + textSuffix(description));
    }

    var code = params.get("code");
    if (code == null || code.isEmpty()) {
      throw new IllegalStateException("Cookie import failed: missing authorization code");
    }

    var tokenBody =
      "client_id=" + urlEncode(CLIENT_ID)
        + "&redirect_uri=" + urlEncode(REDIRECT_URI)
        + "&scope=" + urlEncode(SCOPE)
        + "&grant_type=authorization_code"
        + "&code=" + urlEncode(code)
        + "&code_verifier=" + urlEncode(verifier);

    var tokenJson = ReactorHttpHelper.createReactorClient(proxyData, false)
      .responseTimeout(Duration.ofSeconds(20))
      .headers(h -> {
        h.set(HttpHeaderNames.USER_AGENT, "SoulFire/" + BuildData.VERSION);
        h.set(HttpHeaderNames.ACCEPT, "application/json");
        h.set(HttpHeaderNames.CONTENT_TYPE, "application/x-www-form-urlencoded");
      })
      .post()
      .uri(URI.create("https://login.live.com/oauth20_token.srf"))
      .send(ByteBufFlux.fromString(Mono.just(tokenBody)))
      .responseSingle((res, content) -> {
        if (res.status().codeClass() != HttpStatusClass.SUCCESS) {
          return content.asString(StandardCharsets.UTF_8)
            .defaultIfEmpty("")
            .flatMap(body -> Mono.error(
              new IllegalStateException("Cookie import failed: token exchange failed " + res.status().code() + oauthErrorSuffix(body))
            ));
        }
        return content.asString(StandardCharsets.UTF_8);
      })
      .block();

    if (tokenJson == null || tokenJson.isEmpty()) {
      throw new IllegalStateException("Cookie import failed: empty token response");
    }

    JsonObject obj;
    try {
      obj = GsonInstance.GSON.fromJson(tokenJson, JsonObject.class);
    } catch (Exception e) {
      throw new IllegalStateException("Cookie import failed: invalid token JSON");
    }

    if (obj == null || !obj.has("refresh_token")) {
      throw new IllegalStateException("Cookie import failed: missing refresh token");
    }

    var refreshToken = obj.get("refresh_token").getAsString();
    if (refreshToken == null || refreshToken.isEmpty()) {
      throw new IllegalStateException("Cookie import failed: empty refresh token");
    }

    return refreshToken;
  }

  private static String generatePkceVerifier() {
    var bytes = new byte[32];
    SECURE_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private static String pkceS256Challenge(String verifier) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      var hash = digest.digest(verifier.getBytes(StandardCharsets.US_ASCII));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    } catch (Exception e) {
      throw new IllegalStateException("Cookie import failed: PKCE unavailable");
    }
  }

  private static String oauthErrorSuffix(@Nullable String body) {
    if (body == null || body.isEmpty()) return "";
    var t = body.strip();
    if (t.isEmpty()) return "";

    if (t.length() > 2000) {
      t = t.substring(0, 2000);
    }

    try {
      var obj = GsonInstance.GSON.fromJson(t, JsonObject.class);
      if (obj != null && obj.has("error")) {
        var err = obj.get("error").getAsString();
        var desc = obj.has("error_description") ? obj.get("error_description").getAsString() : null;
        if (desc != null && desc.length() > 200) {
          desc = desc.substring(0, 200);
        }
        if (desc != null && !desc.isEmpty()) {
          return " (error: " + err + ", description: " + desc + ")";
        }
        if (err != null && !err.isEmpty()) {
          return " (error: " + err + ")";
        }
      }
    } catch (Exception ignored) {
    }

    return "";
  }

  private static String textSuffix(@Nullable String text) {
    if (text == null || text.isEmpty()) return "";
    var t = text.strip();
    if (t.isEmpty()) return "";
    if (t.length() > 200) {
      t = t.substring(0, 200);
    }
    return " (description: " + t + ")";
  }

  private static String cookieInputToCookieHeader(String cookieInput) {
    var t = stripEnclosingQuotes(cookieInput.strip());
    if (t.isEmpty()) {
      throw new IllegalArgumentException("Cookie import failed: empty input");
    }

    if (looksLikeCookieJar(t)) {
      var cookieHeader = cookieJarToCookieHeader(t);
      if (cookieHeader.isEmpty()) {
        throw new IllegalArgumentException("Cookie import failed: no valid login.live.com cookies found");
      }
      return cookieHeader;
    }

    if (looksLikeCookieEditorJson(t)) {
      var cookieHeader = cookieEditorJsonToCookieHeader(t);
      if (cookieHeader.isEmpty()) {
        throw new IllegalArgumentException("Cookie import failed: no valid login.live.com cookies found");
      }
      return cookieHeader;
    }

    if (looksLikeCookieHeader(t)) {
      var cookieHeader = normalizeCookieHeader(t);
      if (cookieHeader.isEmpty()) {
        throw new IllegalArgumentException("Cookie import failed: no usable cookies found");
      }
      return cookieHeader;
    }

    throw new IllegalArgumentException("Cookie import failed: unrecognized cookie format");
  }

  private static boolean looksLikeCookieJar(String input) {
    return input.contains("\t")
      && (input.contains("__Host-MSAAUTHP") || input.contains("login.live.com"));
  }

  private static boolean looksLikeCookieHeader(String input) {
    return input.contains("__Host-MSAAUTHP=")
      || input.contains("MSPRequ=")
      || input.contains("MSPOK=")
      || input.contains("PPLState=");
  }

  private static boolean looksLikeCookieEditorJson(String input) {
    var t = input.strip();
    if (!(t.startsWith("[") && t.endsWith("]"))) return false;
    return t.contains("\"domain\"") && t.contains("\"name\"") && t.contains("\"value\"");
  }

  private static String cookieJarToCookieHeader(String cookieJar) {
    var out = new StringBuilder();
    for (var line : cookieJar.split("\\R")) {
      var l = line.strip();
      if (l.isEmpty() || l.startsWith("#")) {
        continue;
      }

      var parts = l.split("\t");
      if (parts.length < 7) {
        continue;
      }

      var domain = stripDomain(parts[0].strip());
      if (!(domain.endsWith("login.live.com") || domain.endsWith(".login.live.com"))) {
        continue;
      }

      var name = parts[5].strip();
      var value = parts[6].strip();
      if (name.isEmpty() || value.isEmpty()) {
        continue;
      }

      if (!out.isEmpty()) {
        out.append("; ");
      }
      out.append(name).append('=').append(value);
    }

    return out.toString();
  }

  private static String cookieEditorJsonToCookieHeader(String cookieJson) {
    JsonArray arr;
    try {
      arr = GsonInstance.GSON.fromJson(cookieJson, JsonArray.class);
    } catch (Exception e) {
      return "";
    }
    if (arr == null || arr.isEmpty()) return "";

    var out = new StringBuilder();
    for (var el : arr) {
      if (!el.isJsonObject()) continue;
      var obj = el.getAsJsonObject();
      if (!obj.has("name") || !obj.has("value")) continue;
      var name = obj.get("name").getAsString();
      var value = obj.get("value").getAsString();
      if (name == null || name.isEmpty() || value == null || value.isEmpty()) continue;

      String domain = null;
      if (obj.has("domain")) {
        try {
          domain = obj.get("domain").getAsString();
        } catch (Exception ignored) {
          domain = null;
        }
      }
      if (domain != null && !domain.isEmpty()) {
        domain = stripDomain(domain);
        if (!(domain.endsWith("login.live.com") || domain.endsWith(".login.live.com"))) {
          continue;
        }
      }

      if (!out.isEmpty()) {
        out.append("; ");
      }
      out.append(name.strip()).append('=').append(value.strip());
    }

    return out.toString();
  }

  private static String normalizeCookieHeader(String cookieHeaderInput) {
    var t = stripEnclosingQuotes(cookieHeaderInput.strip());
    t = t.replace("\r\n", "\n");
    t = t.replace("\n", "; ");
    t = t.replaceFirst("^(?i)cookie:\\s*", "");

    var out = new StringBuilder();
    for (var piece : t.split(";")) {
      var p = piece.strip();
      if (p.isEmpty()) continue;
      var idx = p.indexOf('=');
      if (idx <= 0) continue;
      var name = p.substring(0, idx).strip();
      var value = p.substring(idx + 1).strip();
      if (name.isEmpty() || value.isEmpty()) continue;
      if (!out.isEmpty()) {
        out.append("; ");
      }
      out.append(name).append('=').append(value);
    }
    return out.toString();
  }

  private static String stripEnclosingQuotes(String v) {
    var t = v.strip();
    if (t.length() >= 2) {
      var first = t.charAt(0);
      var last = t.charAt(t.length() - 1);
      if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
        return t.substring(1, t.length() - 1).strip();
      }
    }
    return t;
  }

  private static String stripDomain(String domain) {
    var d = domain.strip();
    if (d.startsWith("http://")) d = d.substring("http://".length());
    if (d.startsWith("https://")) d = d.substring("https://".length());
    var slash = d.indexOf('/');
    if (slash >= 0) d = d.substring(0, slash);
    return d.strip();
  }

  private static Map<String, String> parseQueryParams(@Nullable String rawQuery) {
    var out = new HashMap<String, String>();
    if (rawQuery == null || rawQuery.isEmpty()) {
      return out;
    }

    for (var pair : rawQuery.split("&")) {
      if (pair.isEmpty()) continue;
      var idx = pair.indexOf('=');
      var key = idx >= 0 ? pair.substring(0, idx) : pair;
      var value = idx >= 0 ? pair.substring(idx + 1) : "";
      out.put(urlDecode(key), urlDecode(value));
    }
    return out;
  }

  private static String urlEncode(String v) {
    return URLEncoder.encode(v, StandardCharsets.UTF_8);
  }

  private static String urlDecode(String v) {
    return URLDecoder.decode(v, StandardCharsets.UTF_8);
  }

  public record MSJavaCookiesAuthData(String cookieInput) {}
}
