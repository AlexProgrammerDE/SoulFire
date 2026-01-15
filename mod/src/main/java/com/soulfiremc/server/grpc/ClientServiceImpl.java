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
package com.soulfiremc.server.grpc;

import com.soulfiremc.builddata.BuildData;
import com.soulfiremc.grpc.generated.*;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.database.UserEntity;
import com.soulfiremc.server.settings.server.ServerSettings;
import com.soulfiremc.server.user.PermissionContext;
import com.soulfiremc.server.util.RPCConstants;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;

@Slf4j
@RequiredArgsConstructor
public final class ClientServiceImpl extends ClientServiceGrpc.ClientServiceImplBase {
  private final SoulFireServer soulFireServer;

  private static String buildWebDAVAddress(String baseUrl) {
    if (baseUrl.endsWith("/")) {
      return baseUrl + "webdav";
    } else {
      return baseUrl + "/webdav";
    }
  }

  private static String buildDocsAddress(String baseUrl) {
    if (baseUrl.endsWith("/")) {
      return baseUrl + "docs";
    } else {
      return baseUrl + "/docs";
    }
  }

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

  @Override
  public void getClientData(
    ClientDataRequest request, StreamObserver<ClientDataResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.global(GlobalPermission.READ_CLIENT_DATA));

    try {
      var currentUSer = ServerRPCConstants.USER_CONTEXT_KEY.get();
      var publicAddress = soulFireServer.settingsSource().get(ServerSettings.PUBLIC_ADDRESS);
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
          .setServerInfo(ServerInfo.newBuilder()
            .setVersion(BuildData.VERSION)
            .setCommitHash(BuildData.COMMIT)
            .setBranchName(BuildData.BRANCH)
            .setPublicApiAddress(publicAddress)
            .setPublicWebdavAddress(buildWebDAVAddress(publicAddress))
            .setPublicDocsAddress(buildDocsAddress(publicAddress))
            .build())
          .build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error getting client data", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void generateWebDAVToken(GenerateWebDAVTokenRequest request, StreamObserver<GenerateWebDAVTokenResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.global(GlobalPermission.GENERATE_SELF_WEBDAV_TOKEN));

    try {
      var currentUSer = ServerRPCConstants.USER_CONTEXT_KEY.get();
      responseObserver.onNext(
        GenerateWebDAVTokenResponse.newBuilder()
          .setToken(soulFireServer.authSystem().generateJWT(
            soulFireServer.authSystem().getUserData(currentUSer.getUniqueId()).orElseThrow(),
            RPCConstants.WEBDAV_AUDIENCE
          ))
          .build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error generating WebDAV token", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void generateAPIToken(GenerateAPITokenRequest request, StreamObserver<GenerateAPITokenResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.global(GlobalPermission.GENERATE_SELF_API_TOKEN));

    try {
      var currentUSer = ServerRPCConstants.USER_CONTEXT_KEY.get();
      responseObserver.onNext(
        GenerateAPITokenResponse.newBuilder()
          .setToken(soulFireServer.authSystem().generateJWT(
            soulFireServer.authSystem().getUserData(currentUSer.getUniqueId()).orElseThrow(),
            RPCConstants.API_AUDIENCE
          ))
          .build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error generating API token", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void updateSelfUsername(UpdateSelfUsernameRequest request, StreamObserver<UpdateSelfUsernameResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.global(GlobalPermission.UPDATE_SELF_USERNAME));

    try {
      var userId = ServerRPCConstants.USER_CONTEXT_KEY.get().getUniqueId();
      soulFireServer.sessionFactory().inTransaction(session -> {
        var user = session.find(UserEntity.class, userId);
        if (user == null) {
          throw new IllegalArgumentException("User not found: " + userId);
        }

        user.username(request.getUsername());

        session.merge(user);
      });

      responseObserver.onNext(UpdateSelfUsernameResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error updating self username", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void updateSelfEmail(UpdateSelfEmailRequest request, StreamObserver<UpdateSelfEmailResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.global(GlobalPermission.UPDATE_SELF_EMAIL));

    try {
      var userId = ServerRPCConstants.USER_CONTEXT_KEY.get().getUniqueId();
      soulFireServer.sessionFactory().inTransaction(session -> {
        var user = session.find(UserEntity.class, userId);
        if (user == null) {
          throw new IllegalArgumentException("User not found: " + userId);
        }

        user.email(request.getEmail());

        session.merge(user);
      });

      responseObserver.onNext(UpdateSelfEmailResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error updating self email", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void invalidateSelfSessions(InvalidateSelfSessionsRequest request, StreamObserver<InvalidateSelfSessionsResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.global(GlobalPermission.INVALIDATE_SELF_SESSIONS));

    try {
      var userId = ServerRPCConstants.USER_CONTEXT_KEY.get().getUniqueId();
      soulFireServer.sessionFactory().inTransaction(session -> {
        var user = session.find(UserEntity.class, userId);
        if (user == null) {
          throw new IllegalArgumentException("User not found: " + userId);
        }

        user.minIssuedAt(Instant.now());

        session.merge(user);
      });

      responseObserver.onNext(InvalidateSelfSessionsResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error invalidating self sessions", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }
}
