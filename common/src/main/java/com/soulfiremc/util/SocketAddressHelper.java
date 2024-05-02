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

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.UnixDomainSocketAddress;

public class SocketAddressHelper {
  private SocketAddressHelper() {}

  public static String serialize(SocketAddress address) {
    if (address instanceof InetSocketAddress inetSocketAddress) {
      return "inet://%s:%d".formatted(inetSocketAddress.getHostString(), inetSocketAddress.getPort());
    } else if (address instanceof UnixDomainSocketAddress unixSocketAddress) {
      return "unix://%s".formatted(unixSocketAddress.getPath());
    } else {
      throw new IllegalArgumentException("Unsupported address type: " + address.getClass().getName());
    }
  }

  public static SocketAddress deserialize(String uriString) {
    var uri = URI.create(uriString);
    if ("inet".equals(uri.getScheme())) {
      var parts = uri.getSchemeSpecificPart().split(":");
      var host = parts[0];
      var port = Integer.parseInt(parts[1]);
      return new InetSocketAddress(host, port);
    } else if ("unix".equals(uri.getScheme())) {
      var path = uri.getSchemeSpecificPart();
      return UnixDomainSocketAddress.of(path);
    } else {
      throw new IllegalArgumentException("Unsupported scheme: " + uri.getScheme());
    }
  }
}
