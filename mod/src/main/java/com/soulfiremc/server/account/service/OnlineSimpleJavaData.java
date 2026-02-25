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
package com.soulfiremc.server.account.service;

import com.soulfiremc.grpc.generated.MinecraftAccountProto;

/// Account data for Java Edition accounts authenticated with a raw Minecraft access token.
/// Unlike OnlineChainJavaData, this does not store a full authentication chain and cannot
/// be refreshed. The token is used as-is until it expires.
public record OnlineSimpleJavaData(String accessToken, long expireTimeMs) implements AccountData {
  public static OnlineSimpleJavaData fromProto(MinecraftAccountProto.OnlineSimpleJavaData data) {
    return new OnlineSimpleJavaData(data.getAccessToken(), data.getExpireTimeMs());
  }

  public MinecraftAccountProto.OnlineSimpleJavaData toProto() {
    return MinecraftAccountProto.OnlineSimpleJavaData.newBuilder()
      .setAccessToken(accessToken)
      .setExpireTimeMs(expireTimeMs)
      .build();
  }
}
