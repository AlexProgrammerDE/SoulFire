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

import com.google.protobuf.util.Timestamps;
import com.soulfiremc.grpc.generated.*;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.database.UserRole;
import com.soulfiremc.server.database.generated.Tables;
import com.soulfiremc.server.user.AuthSystem;
import com.soulfiremc.server.user.PermissionContext;
import com.soulfiremc.server.util.RPCConstants;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.impl.DSL;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public final class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {
  private final SoulFireServer soulFireServer;

  private static void mutateOrThrow(UUID targetUser) {
    if (targetUser.equals(ServerRPCConstants.USER_CONTEXT_KEY.get().getUniqueId())) {
      throw new IllegalArgumentException("Cannot mutate self");
    } else if (targetUser.equals(AuthSystem.ROOT_USER_ID)) {
      throw new IllegalArgumentException("Cannot mutate root user");
    }
  }

  @Override
  public void createUser(UserCreateRequest request, StreamObserver<UserCreateResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.global(GlobalPermission.CREATE_USER));

    try {
      var now = LocalDateTime.now();
      soulFireServer.dsl().transaction(cfg -> {
        var ctx = DSL.using(cfg);
        ctx.insertInto(Tables.USERS)
          .set(Tables.USERS.ID, UUID.randomUUID().toString())
          .set(Tables.USERS.USERNAME, request.getUsername())
          .set(Tables.USERS.EMAIL, request.getEmail())
          .set(Tables.USERS.ROLE, (switch (request.getRole()) {
            case ADMIN -> UserRole.ADMIN;
            case USER -> UserRole.USER;
            case UNRECOGNIZED -> throw new IllegalArgumentException("Unknown role: " + request.getRole());
          }).name())
          .set(Tables.USERS.MIN_ISSUED_AT, now)
          .set(Tables.USERS.CREATED_AT, now)
          .set(Tables.USERS.UPDATED_AT, now)
          .execute();
      });

      responseObserver.onNext(UserCreateResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error creating user", t);
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }

  @Override
  public void deleteUser(UserDeleteRequest request, StreamObserver<UserDeleteResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.global(GlobalPermission.DELETE_USER));

    try {
      var userId = UUID.fromString(request.getId());
      mutateOrThrow(userId);

      soulFireServer.authSystem().deleteUser(userId);

      responseObserver.onNext(UserDeleteResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error deleting user", t);
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }

  @Override
  public void listUsers(UserListRequest request, StreamObserver<UserListResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.global(GlobalPermission.READ_USER));

    try {
      var users = soulFireServer.dsl().selectFrom(Tables.USERS).fetch();

      responseObserver.onNext(UserListResponse.newBuilder()
        .addAllUsers(users.stream().map(user -> {
            var result = UserListResponse.User.newBuilder()
              .setId(user.getId())
              .setUsername(user.getUsername())
              .setEmail(user.getEmail())
              .setRole(switch (UserRole.valueOf(user.getRole())) {
                case ADMIN -> com.soulfiremc.grpc.generated.UserRole.ADMIN;
                case USER -> com.soulfiremc.grpc.generated.UserRole.USER;
              })
              .setCreatedAt(Timestamps.fromMillis(user.getCreatedAt().toInstant(ZoneOffset.UTC).toEpochMilli()))
              .setUpdatedAt(Timestamps.fromMillis(user.getUpdatedAt().toInstant(ZoneOffset.UTC).toEpochMilli()))
              .setMinIssuedAt(Timestamps.fromMillis(user.getMinIssuedAt().toInstant(ZoneOffset.UTC).toEpochMilli()));
            if (user.getLastLoginAt() != null) {
              result.setLastLoginAt(Timestamps.fromMillis(user.getLastLoginAt().toInstant(ZoneOffset.UTC).toEpochMilli()));
            }

            return result.build();
          })
          .toList())
        .build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error listing users", t);
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }

  @Override
  public void getUserInfo(UserInfoRequest request, StreamObserver<UserInfoResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.global(GlobalPermission.READ_USER));

    try {
      var userId = UUID.fromString(request.getId());
      var user = soulFireServer.dsl().selectFrom(Tables.USERS)
        .where(Tables.USERS.ID.eq(userId.toString()))
        .fetchOne();
      if (user == null) {
        throw new IllegalArgumentException("User not found: " + userId);
      }

      var result = UserInfoResponse.newBuilder()
        .setUsername(user.getUsername())
        .setEmail(user.getEmail())
        .setRole(switch (UserRole.valueOf(user.getRole())) {
          case ADMIN -> com.soulfiremc.grpc.generated.UserRole.ADMIN;
          case USER -> com.soulfiremc.grpc.generated.UserRole.USER;
        })
        .setCreatedAt(Timestamps.fromMillis(user.getCreatedAt().toInstant(ZoneOffset.UTC).toEpochMilli()))
        .setUpdatedAt(Timestamps.fromMillis(user.getUpdatedAt().toInstant(ZoneOffset.UTC).toEpochMilli()))
        .setMinIssuedAt(Timestamps.fromMillis(user.getMinIssuedAt().toInstant(ZoneOffset.UTC).toEpochMilli()));
      if (user.getLastLoginAt() != null) {
        result.setLastLoginAt(Timestamps.fromMillis(user.getLastLoginAt().toInstant(ZoneOffset.UTC).toEpochMilli()));
      }

      responseObserver.onNext(result.build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error getting user info", t);
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }

  @Override
  public void invalidateSessions(InvalidateSessionsRequest request, StreamObserver<InvalidateSessionsResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.global(GlobalPermission.INVALIDATE_SESSIONS));

    try {
      var userId = UUID.fromString(request.getId());
      mutateOrThrow(userId);

      var now = LocalDateTime.now();
      soulFireServer.dsl().transaction(cfg -> {
        var ctx = DSL.using(cfg);
        var updated = ctx.update(Tables.USERS)
          .set(Tables.USERS.MIN_ISSUED_AT, now)
          .set(Tables.USERS.UPDATED_AT, now)
          .where(Tables.USERS.ID.eq(userId.toString()))
          .execute();
        if (updated == 0) {
          throw new IllegalArgumentException("User not found: " + userId);
        }
      });

      responseObserver.onNext(InvalidateSessionsResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error getting user info", t);
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }

  @Override
  public void updateUser(UpdateUserRequest request, StreamObserver<UpdateUserResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.global(GlobalPermission.UPDATE_USER));

    try {
      var userId = UUID.fromString(request.getId());
      mutateOrThrow(userId);

      soulFireServer.dsl().transaction(cfg -> {
        var ctx = DSL.using(cfg);
        var updated = ctx.update(Tables.USERS)
          .set(Tables.USERS.USERNAME, request.getUsername())
          .set(Tables.USERS.EMAIL, request.getEmail())
          .set(Tables.USERS.ROLE, (switch (request.getRole()) {
            case ADMIN -> UserRole.ADMIN;
            case USER -> UserRole.USER;
            case UNRECOGNIZED -> throw new IllegalArgumentException("Unknown role: " + request.getRole());
          }).name())
          .set(Tables.USERS.UPDATED_AT, LocalDateTime.now())
          .where(Tables.USERS.ID.eq(userId.toString()))
          .execute();
        if (updated == 0) {
          throw new IllegalArgumentException("User not found: " + userId);
        }
      });

      responseObserver.onNext(UpdateUserResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error updating user", t);
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }

  @Override
  public void generateUserAPIToken(GenerateUserAPITokenRequest request, StreamObserver<GenerateUserAPITokenResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.global(GlobalPermission.GENERATE_API_TOKEN));

    try {
      var userId = UUID.fromString(request.getId());
      mutateOrThrow(userId);

      var user = soulFireServer.authSystem().getUserData(userId).orElseThrow(
        () -> new IllegalArgumentException("User not found: " + userId)
      );

      var token = soulFireServer.authSystem().generateJWT(user, RPCConstants.API_AUDIENCE);

      responseObserver.onNext(GenerateUserAPITokenResponse.newBuilder()
        .setToken(token)
        .build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error generating user API token", t);
      throw Status.INTERNAL.withDescription(t.getMessage()).withCause(t).asRuntimeException();
    }
  }
}
