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
package com.soulfiremc.server.util.structs;

import com.google.common.net.HostAndPort;

import java.net.IDN;

public record ServerAddress(HostAndPort hostAndPort) {
  public static ServerAddress fromStringDefaultPort(String address, int defaultPort) {
    return new ServerAddress(HostAndPort.fromString(address).withDefaultPort(defaultPort));
  }

  public static ServerAddress fromStringAndPort(String host, int port) {
    return new ServerAddress(HostAndPort.fromParts(host, port));
  }

  public String host() {
    try {
      return IDN.toASCII(hostAndPort.getHost());
    } catch (IllegalArgumentException e) {
      return "";
    }
  }

  public int port() {
    return hostAndPort.getPort();
  }
}
