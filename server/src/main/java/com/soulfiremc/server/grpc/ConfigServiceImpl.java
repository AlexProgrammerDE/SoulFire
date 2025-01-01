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
package com.soulfiremc.server.grpc;

import com.soulfiremc.grpc.generated.*;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.api.Plugin;
import com.soulfiremc.server.api.PluginInfo;
import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.user.PermissionContext;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collection;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ConfigServiceImpl extends ConfigServiceGrpc.ConfigServiceImplBase {
  private final SoulFireServer soulFireServer;

  private Collection<GlobalPermissionState> getGlobalPermissions() {
    var user = ServerRPCConstants.USER_CONTEXT_KEY.get();
    return Arrays.stream(GlobalPermission.values())
      .filter(permission -> permission != GlobalPermission.UNRECOGNIZED)
      .map(permission -> GlobalPermissionState.newBuilder()
        .setGlobalPermission(permission)
        .setGranted(user.hasPermission(PermissionContext.global(permission)))
        .build())
      .toList();
  }

  private Collection<ServerPlugin> getPlugins() {
    return SoulFireAPI.getServerExtensions().stream()
      .map(Plugin::pluginInfo)
      .map(PluginInfo::toProto)
      .toList();
  }

  @Override
  public void getClientData(
    ClientDataRequest request, StreamObserver<ClientDataResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.global(GlobalPermission.READ_CLIENT_DATA));

    try {
      var currentUSer = ServerRPCConstants.USER_CONTEXT_KEY.get();
      responseObserver.onNext(
        ClientDataResponse.newBuilder()
          .setId(currentUSer.getUniqueId().toString())
          .setUsername(currentUSer.getUsername())
          .setEmail(currentUSer.getEmail())
          .setRole(switch (currentUSer.getRole()) {
            case ADMIN -> UserRole.ADMIN;
            case USER -> UserRole.USER;
          })
          .addAllServerPermissions(getGlobalPermissions())
          .addAllPlugins(getPlugins())
          .addAllServerSettings(soulFireServer.serverSettingsRegistry().exportSettingsMeta())
          .addAllInstanceSettings(soulFireServer.instanceSettingsRegistry().exportSettingsMeta())
          .build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error getting client data", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }
}
