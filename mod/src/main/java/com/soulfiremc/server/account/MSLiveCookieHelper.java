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
import com.soulfiremc.server.proxy.SFProxy;
import com.soulfiremc.server.util.ReactorHttpHelper;
import com.soulfiremc.server.util.structs.GsonInstance;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpStatusClass;
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

/// Utility for exchanging Microsoft Live cookies for a Minecraft-compatible refresh token.
///
/// Supports three cookie input formats:
/// - Netscape cookie jar (tab-separated, e.g. from curl or browser export)
/// - Cookie Editor JSON (array of objects with domain/name/value)
/// - Raw cookie header string (semicolon-separated name=value pairs)
public final class MSLiveCookieHelper {
  private static final String CLIENT_ID = "00000000402b5328";
  private static final String REDIRECT_URI = "https://login.live.com/oauth20_desktop.srf";
  private static final String SCOPE = "service::user.auth.xboxlive.com::MBI_SSL";
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private MSLiveCookieHelper() {}

  /// Parses a cookie input string and exchanges it with Microsoft Live for a refresh token.
  public static String exchangeForRefreshToken(String cookieInput, @Nullable SFProxy proxyData) {
    var cookieHeader = parseCookieHeader(cookieInput);
    var verifier = generatePkceVerifier();
    var challenge = computePkceChallenge(verifier);
    var code = obtainAuthorizationCode(cookieHeader, challenge, proxyData);
    return exchangeCodeForRefreshToken(code, verifier, proxyData);
  }

  // Visible for testing
  static String parseCookieHeader(String cookieInput) {
    var input = stripEnclosingQuotes(cookieInput.strip());
    if (input.isEmpty()) {
      throw new IllegalArgumentException("Empty cookie input");
    }

    if (looksLikeCookieJar(input)) {
      var header = parseCookieJar(input);
      if (header.isEmpty()) {
        throw new IllegalArgumentException("No valid login.live.com cookies found in cookie jar");
      }
      return header;
    }

    if (looksLikeCookieEditorJson(input)) {
      var header = parseCookieEditorJson(input);
      if (header.isEmpty()) {
        throw new IllegalArgumentException("No valid login.live.com cookies found in JSON");
      }
      return header;
    }

    if (looksLikeRawCookieHeader(input)) {
      var header = normalizeCookieHeader(input);
      if (header.isEmpty()) {
        throw new IllegalArgumentException("No usable cookies found in header");
      }
      return header;
    }

    throw new IllegalArgumentException("Unrecognized cookie format");
  }

  private static String obtainAuthorizationCode(String cookieHeader, String challenge, @Nullable SFProxy proxyData) {
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
      .headers(h -> h.set(HttpHeaderNames.COOKIE, cookieHeader))
      .get()
      .uri(authorizeUri)
      .responseSingle((res, content) -> {
        if (res.status().codeClass() != HttpStatusClass.REDIRECTION) {
          return content.asString(StandardCharsets.UTF_8)
            .defaultIfEmpty("")
            .flatMap(_ -> Mono.error(
              new IllegalStateException("Unexpected authorize response: " + res.status().code())
            ));
        }
        return Mono.justOrEmpty(res.responseHeaders().get(HttpHeaderNames.LOCATION));
      })
      .block();

    if (location == null || location.isEmpty()) {
      throw new IllegalStateException("Missing authorize redirect location");
    }

    var redirect = URI.create(location);
    var expectedRedirect = URI.create(REDIRECT_URI);
    if (redirect.getHost() == null
      || !expectedRedirect.getHost().equalsIgnoreCase(redirect.getHost())
      || !expectedRedirect.getPath().equals(redirect.getPath())) {
      throw new IllegalStateException("Unexpected redirect to " + redirect.getHost() + redirect.getPath());
    }

    var params = parseQueryParams(redirect.getRawQuery());
    var error = params.get("error");
    if (error != null && !error.isEmpty()) {
      var description = params.getOrDefault("error_description", "");
      var suffix = description.isEmpty() ? "" : ": " + description;
      throw new IllegalStateException("Authorize error " + error + suffix);
    }

    var code = params.get("code");
    if (code == null || code.isEmpty()) {
      throw new IllegalStateException("Missing authorization code in redirect");
    }

    return code;
  }

  private static String exchangeCodeForRefreshToken(String code, String verifier, @Nullable SFProxy proxyData) {
    var tokenBody =
      "client_id=" + urlEncode(CLIENT_ID)
        + "&redirect_uri=" + urlEncode(REDIRECT_URI)
        + "&scope=" + urlEncode(SCOPE)
        + "&grant_type=authorization_code"
        + "&code=" + urlEncode(code)
        + "&code_verifier=" + urlEncode(verifier);

    var tokenJson = ReactorHttpHelper.createReactorClient(proxyData, false)
      .responseTimeout(Duration.ofSeconds(20))
      .headers(h -> h.set(HttpHeaderNames.CONTENT_TYPE, "application/x-www-form-urlencoded"))
      .post()
      .uri(URI.create("https://login.live.com/oauth20_token.srf"))
      .send(ByteBufFlux.fromString(Mono.just(tokenBody)))
      .responseSingle((res, content) -> {
        if (res.status().codeClass() != HttpStatusClass.SUCCESS) {
          return content.asString(StandardCharsets.UTF_8)
            .defaultIfEmpty("")
            .flatMap(body -> Mono.error(
              new IllegalStateException("Token exchange failed " + res.status().code() + extractOAuthError(body))
            ));
        }
        return content.asString(StandardCharsets.UTF_8);
      })
      .block();

    if (tokenJson == null || tokenJson.isEmpty()) {
      throw new IllegalStateException("Empty token response");
    }

    JsonObject obj;
    try {
      obj = GsonInstance.GSON.fromJson(tokenJson, JsonObject.class);
    } catch (Exception e) {
      throw new IllegalStateException("Invalid token response JSON", e);
    }

    if (obj == null || !obj.has("refresh_token")) {
      throw new IllegalStateException("Missing refresh_token in token response");
    }

    var refreshToken = obj.get("refresh_token").getAsString();
    if (refreshToken == null || refreshToken.isEmpty()) {
      throw new IllegalStateException("Empty refresh_token in token response");
    }

    return refreshToken;
  }

  private static String generatePkceVerifier() {
    var bytes = new byte[32];
    SECURE_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private static String computePkceChallenge(String verifier) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      var hash = digest.digest(verifier.getBytes(StandardCharsets.US_ASCII));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    } catch (Exception e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }

  private static boolean looksLikeCookieJar(String input) {
    return input.contains("\t")
      && (input.contains("__Host-MSAAUTHP") || input.contains("login.live.com"));
  }

  private static boolean looksLikeRawCookieHeader(String input) {
    return input.contains("__Host-MSAAUTHP=")
      || input.contains("MSPRequ=")
      || input.contains("MSPOK=")
      || input.contains("PPLState=");
  }

  private static boolean looksLikeCookieEditorJson(String input) {
    var t = input.strip();
    return t.startsWith("[") && t.endsWith("]")
      && t.contains("\"domain\"") && t.contains("\"name\"") && t.contains("\"value\"");
  }

  private static String parseCookieJar(String cookieJar) {
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
      if (!isLiveDomain(domain)) {
        continue;
      }

      var name = parts[5].strip();
      var value = parts[6].strip();
      if (name.isEmpty() || value.isEmpty()) {
        continue;
      }

      appendCookie(out, name, value);
    }

    return out.toString();
  }

  private static String parseCookieEditorJson(String cookieJson) {
    JsonArray arr;
    try {
      arr = GsonInstance.GSON.fromJson(cookieJson, JsonArray.class);
    } catch (Exception e) {
      return "";
    }
    if (arr == null || arr.isEmpty()) {
      return "";
    }

    var out = new StringBuilder();
    for (var el : arr) {
      if (!el.isJsonObject()) {
        continue;
      }
      var obj = el.getAsJsonObject();
      if (!obj.has("name") || !obj.has("value")) {
        continue;
      }

      var name = obj.get("name").getAsString();
      var value = obj.get("value").getAsString();
      if (name == null || name.isEmpty() || value == null || value.isEmpty()) {
        continue;
      }

      if (obj.has("domain")) {
        try {
          var domain = stripDomain(obj.get("domain").getAsString());
          if (!isLiveDomain(domain)) {
            continue;
          }
        } catch (Exception ignored) {
        }
      }

      appendCookie(out, name.strip(), value.strip());
    }

    return out.toString();
  }

  private static String normalizeCookieHeader(String headerInput) {
    var t = stripEnclosingQuotes(headerInput.strip());
    t = t.replace("\r\n", "\n").replace("\n", "; ");
    t = t.replaceFirst("^(?i)cookie:\\s*", "");

    var out = new StringBuilder();
    for (var piece : t.split(";")) {
      var p = piece.strip();
      if (p.isEmpty()) {
        continue;
      }
      var idx = p.indexOf('=');
      if (idx <= 0) {
        continue;
      }
      var name = p.substring(0, idx).strip();
      var value = p.substring(idx + 1).strip();
      if (name.isEmpty() || value.isEmpty()) {
        continue;
      }
      appendCookie(out, name, value);
    }
    return out.toString();
  }

  private static void appendCookie(StringBuilder out, String name, String value) {
    if (!out.isEmpty()) {
      out.append("; ");
    }
    out.append(name).append('=').append(value);
  }

  private static boolean isLiveDomain(String domain) {
    return domain.equals("login.live.com") || domain.endsWith(".login.live.com");
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
    if (d.startsWith("http://")) {
      d = d.substring("http://".length());
    }
    if (d.startsWith("https://")) {
      d = d.substring("https://".length());
    }
    var slash = d.indexOf('/');
    if (slash >= 0) {
      d = d.substring(0, slash);
    }
    return d.strip();
  }

  private static String extractOAuthError(@Nullable String body) {
    if (body == null || body.isBlank()) {
      return "";
    }

    try {
      var obj = GsonInstance.GSON.fromJson(body.strip(), JsonObject.class);
      if (obj != null && obj.has("error")) {
        var err = obj.get("error").getAsString();
        var desc = obj.has("error_description") ? obj.get("error_description").getAsString() : null;
        if (desc != null && !desc.isEmpty()) {
          return " (%s: %s)".formatted(err, desc.length() > 200 ? desc.substring(0, 200) : desc);
        }
        return " (%s)".formatted(err);
      }
    } catch (Exception ignored) {
    }

    return "";
  }

  private static Map<String, String> parseQueryParams(@Nullable String rawQuery) {
    var out = new HashMap<String, String>();
    if (rawQuery == null || rawQuery.isEmpty()) {
      return out;
    }

    for (var pair : rawQuery.split("&")) {
      if (pair.isEmpty()) {
        continue;
      }
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
}
