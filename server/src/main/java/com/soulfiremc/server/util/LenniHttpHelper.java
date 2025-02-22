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
package com.soulfiremc.server.util;

import com.soulfiremc.server.proxy.SFProxy;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;
import net.lenni0451.commons.httpclient.HttpClient;
import net.lenni0451.commons.httpclient.HttpResponse;
import net.lenni0451.commons.httpclient.executor.RequestExecutor;
import net.lenni0451.commons.httpclient.requests.HttpContentRequest;
import net.lenni0451.commons.httpclient.requests.HttpRequest;
import net.lenni0451.commons.httpclient.utils.HttpRequestUtils;
import net.lenni0451.commons.httpclient.utils.URLWrapper;
import net.raphimc.minecraftauth.MinecraftAuth;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class LenniHttpHelper {
  private LenniHttpHelper() {}

  public static HttpClient createLenniMCAuthHttpClient(SFProxy proxyData) {
    return MinecraftAuth.createHttpClient()
      .setExecutor(client -> new ReactorLenniExecutor(proxyData, client));
  }

  private static Map<String, List<String>> getAsMap(HttpHeaders headers) {
    return headers.entries().stream()
      .collect(
        Collectors.groupingBy(
          Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
  }

  private static class ReactorLenniExecutor extends RequestExecutor {
    private final SFProxy proxyData;

    public ReactorLenniExecutor(SFProxy proxyData, HttpClient httpClient) {
      super(httpClient);
      this.proxyData = proxyData;
    }

    @NotNull
    @Override
    public HttpResponse execute(@NotNull HttpRequest httpRequest) throws IOException {
      var cookieManager = getCookieManager(httpRequest);
      try {
        log.debug("Executing request: {}", httpRequest.getURL());
        var requestHeaders = getHeaders(httpRequest, cookieManager);

        var base =
          ReactorHttpHelper.createReactorClient(proxyData, false)
            .followRedirect(
              switch (httpRequest.getFollowRedirects()) {
                case NOT_SET -> client.isFollowRedirects();
                case FOLLOW -> true;
                case IGNORE -> false;
              })
            .responseTimeout(Duration.ofMillis(client.getReadTimeout()))
            .headers(h -> requestHeaders.forEach((k, v) -> h.set(k, String.join("; ", v))))
            .request(HttpMethod.valueOf(httpRequest.getMethod()))
            .uri(httpRequest.getURL().toURI());

        var receiver = httpRequest instanceof HttpContentRequest contentRequest
          ? base.send(ByteBufFlux.fromInbound(Flux.just(Objects.requireNonNull(contentRequest.getContent()).getAsBytes())))
          : base;

        return receiver
          .responseSingle(
            (res, content) -> {
              try {
                var code = res.status().code();
                var url = new URLWrapper(Objects.requireNonNull(res.resourceUrl()));
                var urlObj = url.toURL();
                var responseHeaders = getAsMap(res.responseHeaders());

                HttpRequestUtils.updateCookies(cookieManager, url.toURL(), responseHeaders);

                return content
                  .asByteArray()
                  .mapNotNull(bytes -> new HttpResponse(urlObj, code, bytes, responseHeaders))
                  .switchIfEmpty(
                    Mono.just(new HttpResponse(urlObj, code, (byte[]) null, responseHeaders)));
              } catch (Exception e) {
                log.error("Error while handling response", e);
                return Mono.error(e);
              }
            })
          .blockOptional()
          .orElseThrow();
      } catch (Exception e) {
        throw new IOException(e);
      }
    }
  }
}
