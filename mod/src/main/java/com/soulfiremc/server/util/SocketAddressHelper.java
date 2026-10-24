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
package com.soulfiremc.server.util;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnixDomainSocketAddress;

public final class SocketAddressHelper {
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
      case InetSocketAddress inet -> {
        var host = inet.getHostString();
        if (host.contains(":") && !host.startsWith("[")) {
          host = "[" + host + "]";
        }
        yield "inet://%s:%d".formatted(host, inet.getPort());
      }
      case UnixDomainSocketAddress unix -> {
        try {
          yield new URI("unix", "", unix.getPath().toString(), null, null).toASCIIString();
        } catch (URISyntaxException e) {
          throw new IllegalArgumentException("Invalid Unix socket address", e);
        }
      }
      default -> throw new IllegalArgumentException("Unsupported address type: " + address.getClass().getName());
    };
  }

  public static SocketAddress deserialize(String uriString) {
    var uri = URI.create(uriString);
    return switch (uri.getScheme()) {
      case null -> throw new IllegalArgumentException("Address must include an inet:// or unix:// scheme");
      case "inet" -> {
        var host = uri.getHost();
        var port = uri.getPort();
        if (host == null || port == -1) {
          throw new IllegalArgumentException("Invalid inet address, expected format 'inet://host:port' but got: " + uriString);
        }
        yield new InetSocketAddress(host, port);
      }
      case "unix" -> {
        var path = uri.getPath();
        if (path == null) {
          throw new IllegalArgumentException("Invalid unix address, expected format 'unix:///path' but got: " + uriString);
        }
        yield UnixDomainSocketAddress.of(path);
      }
      default -> throw new IllegalArgumentException("Unsupported scheme: " + uri.getScheme());
    };
  }
}
