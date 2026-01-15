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
import com.soulfiremc.server.database.UserEntity;
import com.soulfiremc.server.user.AuthSystem;
import com.soulfiremc.server.user.PermissionContext;
import com.soulfiremc.server.util.RPCConstants;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
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
      soulFireServer.sessionFactory().inTransaction(session -> {
        var userEntity = new UserEntity();
        userEntity.id(UUID.randomUUID());
        userEntity.username(request.getUsername());
        userEntity.email(request.getEmail());
        userEntity.role(switch (request.getRole()) {
          case ADMIN -> UserEntity.Role.ADMIN;
          case USER -> UserEntity.Role.USER;
          case UNRECOGNIZED -> throw new IllegalArgumentException("Unknown role: " + request.getRole());
        });
        userEntity.minIssuedAt(Instant.now());

        session.persist(userEntity);
      });

      responseObserver.onNext(UserCreateResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error creating user", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
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
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void listUsers(UserListRequest request, StreamObserver<UserListResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.global(GlobalPermission.READ_USER));

    try {
      var users = soulFireServer.sessionFactory().fromTransaction(session -> session.createQuery("from UserEntity", UserEntity.class).list());

      responseObserver.onNext(UserListResponse.newBuilder()
        .addAllUsers(users.stream().map(user -> {
            var result = UserListResponse.User.newBuilder()
              .setId(user.id().toString())
              .setUsername(user.username())
              .setEmail(user.email())
              .setRole(switch (user.role()) {
                case ADMIN -> UserRole.ADMIN;
                case USER -> UserRole.USER;
              })
              .setCreatedAt(Timestamps.fromMillis(user.createdAt().toEpochMilli()))
              .setUpdatedAt(Timestamps.fromMillis(user.updatedAt().toEpochMilli()))
              .setMinIssuedAt(Timestamps.fromMillis(user.minIssuedAt().toEpochMilli()));
            if (user.lastLoginAt() != null) {
              result.setLastLoginAt(Timestamps.fromMillis(user.lastLoginAt().toEpochMilli()));
            }

            return result.build();
          })
          .toList())
        .build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error listing users", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void getUserInfo(UserInfoRequest request, StreamObserver<UserInfoResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.global(GlobalPermission.READ_USER));

    try {
      var userId = UUID.fromString(request.getId());
      var user = soulFireServer.sessionFactory().fromTransaction(session -> session.find(UserEntity.class, userId));
      if (user == null) {
        throw new IllegalArgumentException("User not found: " + userId);
      }

      var result = UserInfoResponse.newBuilder()
        .setUsername(user.username())
        .setEmail(user.email())
        .setRole(switch (user.role()) {
          case ADMIN -> UserRole.ADMIN;
          case USER -> UserRole.USER;
        })
        .setCreatedAt(Timestamps.fromMillis(user.createdAt().toEpochMilli()))
        .setUpdatedAt(Timestamps.fromMillis(user.updatedAt().toEpochMilli()))
        .setMinIssuedAt(Timestamps.fromMillis(user.minIssuedAt().toEpochMilli()));
      if (user.lastLoginAt() != null) {
        result.setLastLoginAt(Timestamps.fromMillis(user.lastLoginAt().toEpochMilli()));
      }

      responseObserver.onNext(result.build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error getting user info", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void invalidateSessions(InvalidateSessionsRequest request, StreamObserver<InvalidateSessionsResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.global(GlobalPermission.INVALIDATE_SESSIONS));

    try {
      var userId = UUID.fromString(request.getId());
      mutateOrThrow(userId);

      soulFireServer.sessionFactory().inTransaction(session -> {
        var user = session.find(UserEntity.class, userId);
        if (user == null) {
          throw new IllegalArgumentException("User not found: " + userId);
        }

        user.minIssuedAt(Instant.now());

        session.merge(user);
      });

      responseObserver.onNext(InvalidateSessionsResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error getting user info", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void updateUser(UpdateUserRequest request, StreamObserver<UpdateUserResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.global(GlobalPermission.UPDATE_USER));

    try {
      var userId = UUID.fromString(request.getId());
      mutateOrThrow(userId);

      soulFireServer.sessionFactory().inTransaction(session -> {
        var user = session.find(UserEntity.class, userId);
        if (user == null) {
          throw new IllegalArgumentException("User not found: " + userId);
        }

        user.username(request.getUsername());
        user.email(request.getEmail());
        user.role(switch (request.getRole()) {
          case ADMIN -> UserEntity.Role.ADMIN;
          case USER -> UserEntity.Role.USER;
          case UNRECOGNIZED -> throw new IllegalArgumentException("Unknown role: " + request.getRole());
        });

        session.merge(user);
      });

      responseObserver.onNext(UpdateUserResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error updating user", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void generateUserAPIToken(GenerateUserAPITokenRequest request, StreamObserver<GenerateUserAPITokenResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.global(GlobalPermission.GENERATE_API_TOKEN));

    try {
      var userId = UUID.fromString(request.getId());
      mutateOrThrow(userId);

      var user = soulFireServer.sessionFactory().fromTransaction(session -> session.find(UserEntity.class, userId));
      if (user == null) {
        throw new IllegalArgumentException("User not found: " + userId);
      }

      var token = soulFireServer.authSystem().generateJWT(user, RPCConstants.API_AUDIENCE);

      responseObserver.onNext(GenerateUserAPITokenResponse.newBuilder()
        .setToken(token)
        .build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error generating user API token", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }
}
