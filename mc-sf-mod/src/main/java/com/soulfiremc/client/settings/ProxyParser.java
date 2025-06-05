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
package com.soulfiremc.client.settings;

import com.soulfiremc.server.proxy.ProxyType;
import com.soulfiremc.server.proxy.SFProxy;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

@Slf4j
public abstract class ProxyParser {
  private static <T> T getIndexOrNull(T[] array, int index) {
    if (index < array.length) {
      return array[index];
    } else {
      return null;
    }
  }

  public static ProxyParser typeParser(ProxyType type) {
    return new ProxyParser() {
      @Override
      public SFProxy parse(String proxyData) {
        var split = proxyData.split(":");

        if (split.length < 2) {
          throw new IllegalArgumentException("Proxy must have at least a host and a port!");
        }

        var host = split[0];
        var port = Integer.parseInt(split[1]);
        var username = getIndexOrNull(split, 2);
        var password = getIndexOrNull(split, 3);

        return new SFProxy(type, new InetSocketAddress(host, port), username, password);
      }
    };
  }

  public static ProxyParser uriParser() {
    return new ProxyParser() {
      @Override
      public SFProxy parse(String proxyData) {
        URI uri;
        try {
          uri = URI.create(proxyData).parseServerAuthority();
        } catch (URISyntaxException e) {
          throw new RuntimeException(e);
        }

        var scheme = uri.getScheme();

        if (scheme == null) {
          throw new IllegalArgumentException("Proxy URI must have a scheme!");
        }

        var proxyType = ProxyType.valueOf(scheme.toUpperCase(Locale.ROOT));
        var userInfo = uri.getUserInfo();
        var host = uri.getHost();
        var port = uri.getPort();

        if (host == null || port == -1) {
          throw new IllegalArgumentException("Proxy URI must have a host and a port!");
        }

        String username = null;
        String password = null;

        if (userInfo != null) {
          var split = userInfo.split(":");
          username = getIndexOrNull(split, 0);
          password = getIndexOrNull(split, 1);
        }

        return new SFProxy(proxyType, new InetSocketAddress(host, port), username, password);
      }
    };
  }

  abstract SFProxy parse(String proxyData);
}
