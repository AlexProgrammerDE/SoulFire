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
    return switch (address) {
      case InetSocketAddress inet -> "inet://%s:%d".formatted(inet.getHostString(), inet.getPort());
      case UnixDomainSocketAddress unix -> "unix://%s".formatted(unix.getPath());
      default -> throw new IllegalArgumentException("Unsupported address type: " + address.getClass().getName());
    };
  }

  public static SocketAddress deserialize(String uriString) {
    var uri = URI.create(uriString);
    return switch (uri.getScheme()) {
      case "inet" -> {
        var host = uri.getHost();
        var port = uri.getPort();
        yield new InetSocketAddress(host, port);
      }
      case "unix" -> {
        var path = uri.getPath();
        yield UnixDomainSocketAddress.of(path);
      }
      default -> throw new IllegalArgumentException("Unsupported scheme: " + uri.getScheme());
    };
  }
}
