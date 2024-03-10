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
package com.soulfiremc.util;

import com.soulfiremc.builddata.BuildData;
import com.soulfiremc.proxy.SFProxy;
import java.time.Duration;
import reactor.netty.transport.ProxyProvider;

public class ReactorHttpHelper {
  private ReactorHttpHelper() {}

  public static reactor.netty.http.client.HttpClient createReactorClient(
      SFProxy proxyData, boolean withBody) {
    var base =
        reactor.netty.http.client.HttpClient.create()
            .responseTimeout(Duration.ofSeconds(5))
            .headers(
                h -> {
                  h.add("Accept", "application/json");
                  if (withBody) {
                    h.add("Content-Type", "application/json");
                  }

                  h.add("Accept-Language", "en-US,en");
                  h.add("User-Agent", "SoulFire/" + BuildData.VERSION);
                });

    return proxyData == null
        ? base
        : base.proxy(
        p -> {
          var spec =
              p.type(
                      switch (proxyData.type()) {
                        case HTTP -> ProxyProvider.Proxy.HTTP;
                        case SOCKS4 -> ProxyProvider.Proxy.SOCKS4;
                        case SOCKS5 -> ProxyProvider.Proxy.SOCKS5;
                      })
                  .host(proxyData.host())
                  .port(proxyData.port())
                  .nonProxyHosts("localhost")
                  .connectTimeoutMillis(20_000);

          if (proxyData.username() != null) {
            spec.username(proxyData.username());
          }

          if (proxyData.password() != null) {
            spec.password(s -> proxyData.password());
          }
        });
  }
}
