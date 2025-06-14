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
import com.soulfiremc.server.util.KeyHelper;
import com.soulfiremc.server.util.structs.GsonInstance;
import lombok.SneakyThrows;

import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.UUID;

public record BedrockData(
  String mojangJwt,
  String identityJwt,
  ECPublicKey publicKey,
  ECPrivateKey privateKey,
  UUID deviceId,
  String playFabId,
  JsonObject authChain)
  implements AccountData {
  @SneakyThrows
  public static BedrockData fromProto(MinecraftAccountProto.BedrockData data) {
    return new BedrockData(
      data.getMojangJwt(),
      data.getIdentityJwt(),
      KeyHelper.decodeBase64PublicKey(data.getPublicKey()),
      KeyHelper.decodeBase64PrivateKey(data.getPrivateKey()),
      UUID.fromString(data.getDeviceId()),
      data.getPlayFabId(),
      GsonInstance.GSON.fromJson(JsonFormat.printer().print(data.getAuthChain()), JsonObject.class));
  }

  @SneakyThrows
  public MinecraftAccountProto.BedrockData toProto() {
    var authChainBuilder = Struct.newBuilder();
    JsonFormat.parser().merge(GsonInstance.GSON.toJson(authChain), authChainBuilder);
    return MinecraftAccountProto.BedrockData.newBuilder()
      .setMojangJwt(mojangJwt)
      .setIdentityJwt(identityJwt)
      .setPublicKey(KeyHelper.encodeBase64Key(publicKey))
      .setPrivateKey(KeyHelper.encodeBase64Key(privateKey))
      .setDeviceId(deviceId.toString())
      .setPlayFabId(playFabId)
      .setAuthChain(authChainBuilder.build())
      .build();
  }
}
