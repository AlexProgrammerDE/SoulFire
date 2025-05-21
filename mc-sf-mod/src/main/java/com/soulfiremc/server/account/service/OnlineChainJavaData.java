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
package com.soulfiremc.server.account.service;

import com.google.gson.JsonObject;
import com.google.protobuf.Struct;
import com.google.protobuf.util.JsonFormat;
import com.soulfiremc.grpc.generated.MinecraftAccountProto;
import com.soulfiremc.server.util.structs.GsonInstance;
import lombok.SneakyThrows;

public record OnlineChainJavaData(String authToken, long tokenExpireAt, JsonObject authChain) implements AccountData, OnlineJavaDataLike {
  @SneakyThrows
  public static OnlineChainJavaData fromProto(MinecraftAccountProto.OnlineChainJavaData data) {
    return new OnlineChainJavaData(
      data.getAuthToken(),
      data.getTokenExpireAt(),
      GsonInstance.GSON.fromJson(JsonFormat.printer().print(data.getAuthChain()), JsonObject.class));
  }

  @SneakyThrows
  public MinecraftAccountProto.OnlineChainJavaData toProto() {
    var authChainBuilder = Struct.newBuilder();
    JsonFormat.parser().merge(GsonInstance.GSON.toJson(authChain), authChainBuilder);
    return MinecraftAccountProto.OnlineChainJavaData.newBuilder()
      .setAuthToken(authToken)
      .setTokenExpireAt(tokenExpireAt)
      .setAuthChain(authChainBuilder)
      .build();
  }
}
