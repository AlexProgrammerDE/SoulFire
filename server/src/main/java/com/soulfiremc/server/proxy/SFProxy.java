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
package com.soulfiremc.server.proxy;

import com.soulfiremc.grpc.generated.ProxyProto;
import com.soulfiremc.server.util.SocketAddressHelper;
import lombok.NonNull;
import org.geysermc.mcprotocollib.network.ProxyInfo;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public record SFProxy(
  @NonNull ProxyType type,
  @NonNull SocketAddress address,
  @Nullable String username,
  @Nullable String password) {
  public SFProxy {
    if (type == ProxyType.SOCKS4 && password != null) {
      throw new IllegalArgumentException("SOCKS4 does not support passwords!");
    } else if (username != null && username.isBlank()) {
      // Sanitize empty strings
      username = null;
    } else if (password != null && password.isBlank()) {
      // Sanitize empty strings
      password = null;
    }

    if (username == null && password != null) {
      throw new IllegalArgumentException("Username must be set if password is set!");
    }

    if (address instanceof InetSocketAddress inetSocketAddress && inetSocketAddress.isUnresolved()) {
      throw new IllegalArgumentException("Address must be resolved!");
    }
  }

  public static SFProxy fromProto(ProxyProto proto) {
    return new SFProxy(
      ProxyType.valueOf(proto.getType().name()),
      SocketAddressHelper.deserialize(proto.getAddress()),
      proto.hasUsername() ? proto.getUsername() : null,
      proto.hasPassword() ? proto.getPassword() : null);
  }

  public SocketAddress getSocketAddress() {
    return address;
  }

  public ProxyProto toProto() {
    var builder =
      ProxyProto.newBuilder()
        .setType(ProxyProto.Type.valueOf(type.name()))
        .setAddress(SocketAddressHelper.serialize(address));

    if (username != null) {
      builder.setUsername(username);
    }

    if (password != null) {
      builder.setPassword(password);
    }

    return builder.build();
  }

  public ProxyInfo toMCPLProxy() {
    return new ProxyInfo(
      type.toMCPLType(),
      address,
      username,
      password);
  }
}
