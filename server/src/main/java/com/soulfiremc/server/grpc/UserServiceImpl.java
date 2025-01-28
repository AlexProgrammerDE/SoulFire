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
import com.soulfiremc.server.database.UserEntity;
import com.soulfiremc.server.user.PermissionContext;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;

import javax.inject.Inject;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {
  private final SoulFireServer soulFireServer;
  private final SessionFactory sessionFactory;

  @Override
  public void createUser(UserCreateRequest request, StreamObserver<UserCreateResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.global(GlobalPermission.CREATE_USER));

    try {
      sessionFactory.inTransaction(session -> {
        var userEntity = new UserEntity();
        userEntity.username(request.getUsername());
        userEntity.email(request.getEmail());
        userEntity.role(switch (request.getRole()) {
          case ADMIN -> UserEntity.Role.ADMIN;
          case USER -> UserEntity.Role.USER;
          default -> throw new IllegalArgumentException("Unknown role: " + request.getRole());
        });

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
      var uuid = UUID.fromString(request.getId());
      if (uuid.equals(ServerRPCConstants.USER_CONTEXT_KEY.get().getUniqueId())) {
        throw new IllegalArgumentException("Cannot delete self");
      } else if (uuid.equals(soulFireServer.authSystem().rootUserId())) {
        throw new IllegalArgumentException("Cannot delete root user");
      }

      sessionFactory.inTransaction(s -> s.createMutationQuery("DELETE FROM UserEntity WHERE id = :id")
        .setParameter("id", uuid)
        .executeUpdate());

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
      var users = sessionFactory.fromTransaction(session -> session.createQuery("from UserEntity", UserEntity.class).list());

      responseObserver.onNext(UserListResponse.newBuilder()
        .addAllUsers(users.stream().map(user -> UserListResponse.User.newBuilder()
            .setId(user.id().toString())
            .setUsername(user.username())
            .setEmail(user.email())
            .setRole(switch (user.role()) {
              case ADMIN -> UserRole.ADMIN;
              case USER -> UserRole.USER;
            })
            .build())
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
      var user = sessionFactory.fromTransaction(session -> session.find(UserEntity.class, request.getId()));
      if (user == null) {
        throw new IllegalArgumentException("User not found: " + request.getId());
      }

      responseObserver.onNext(UserInfoResponse.newBuilder()
        .setUsername(user.username())
        .setEmail(user.email())
        .setRole(switch (user.role()) {
          case ADMIN -> UserRole.ADMIN;
          case USER -> UserRole.USER;
        })
        .build());
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
      sessionFactory.inTransaction(session -> {
        var user = session.find(UserEntity.class, request.getId());
        if (user == null) {
          throw new IllegalArgumentException("User not found: " + request.getId());
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
}
