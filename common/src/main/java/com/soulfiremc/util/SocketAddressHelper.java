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

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.UnixDomainSocketAddress;

public class SocketAddressHelper {
  public static final TypeAdapter<SocketAddress> TYPE_ADAPTER =
    new TypeAdapter<>() {
      @Override
      public void write(JsonWriter out, SocketAddress value) throws IOException {
        out.value(serialize(value));
      }

      @Override
      public SocketAddress read(JsonReader in) throws IOException {
        return deserialize(in.nextString());
      }
    };

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
      var host = uri.getHost();
      var port = uri.getPort();
      return new InetSocketAddress(host, port);
    } else if ("unix".equals(uri.getScheme())) {
      var path = uri.getPath();
      return UnixDomainSocketAddress.of(path);
    } else {
      throw new IllegalArgumentException("Unsupported scheme: " + uri.getScheme());
    }
  }
}
