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
import com.soulfiremc.server.database.ServerConfigEntity;
import com.soulfiremc.server.settings.lib.ServerSettingsImpl;
import com.soulfiremc.server.user.PermissionContext;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;

import javax.inject.Inject;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ServerServiceImpl extends ServerServiceGrpc.ServerServiceImplBase {
  private final SoulFireServer soulFireServer;
  private final SessionFactory sessionFactory;

  @Override
  public void getServerInfo(ServerInfoRequest request, StreamObserver<ServerInfoResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.global(GlobalPermission.READ_SERVER_CONFIG));

    try {
      var configEntity = sessionFactory.fromTransaction(session -> session.find(ServerConfigEntity.class, 1));
      ServerSettingsImpl config;
      if (configEntity == null) {
        config = ServerSettingsImpl.EMPTY;
      } else {
        config = configEntity.settings();
      }

      responseObserver.onNext(ServerInfoResponse.newBuilder()
        .setConfig(config.toProto())
        .build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error getting server info", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void updateServerConfig(ServerUpdateConfigRequest request, StreamObserver<ServerUpdateConfigResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.global(GlobalPermission.UPDATE_SERVER_CONFIG));

    try {
      sessionFactory.inTransaction(session -> {
        var currentConfigEntity = session.find(ServerConfigEntity.class, 1);
        if (currentConfigEntity == null) {
          var newConfigEntity = new ServerConfigEntity();
          newConfigEntity.settings(ServerSettingsImpl.fromProto(request.getConfig()));
          session.persist(newConfigEntity);
        } else {
          currentConfigEntity.settings(ServerSettingsImpl.fromProto(request.getConfig()));
          session.merge(currentConfigEntity);
        }
      });

      soulFireServer.configUpdateHook();
      responseObserver.onNext(ServerUpdateConfigResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error updating server config", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }
}
