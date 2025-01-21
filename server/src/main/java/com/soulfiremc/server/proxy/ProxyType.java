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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.geysermc.mcprotocollib.network.ProxyInfo;

@Getter
@RequiredArgsConstructor
public enum ProxyType {
  HTTP(false),
  SOCKS4(false),
  SOCKS5(true);

  private final boolean udpSupport;

  public ProxyInfo.Type toMCPLType() {
    return switch (this) {
      case HTTP -> ProxyInfo.Type.HTTP;
      case SOCKS4 -> ProxyInfo.Type.SOCKS4;
      case SOCKS5 -> ProxyInfo.Type.SOCKS5;
    };
  }
}
