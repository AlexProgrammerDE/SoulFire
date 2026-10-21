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
import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.database.UserRole;
import com.soulfiremc.server.database.generated.Tables;
import com.soulfiremc.server.database.generated.tables.records.PluginPermissionGrantsRecord;
import com.soulfiremc.server.user.AuthSystem;
import com.soulfiremc.server.user.PermissionContext;
import com.soulfiremc.server.user.PluginPermissionGrantScope;
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

    UserProfileValidation.requireUsername(request.getUsername());
    UserProfileValidation.requireEmail(request.getEmail());

    try {
      var userId = UUID.randomUUID();
      var now = LocalDateTime.now(ZoneOffset.UTC);
      soulFireServer.dsl().transaction(cfg -> {
        var ctx = DSL.using(cfg);
        ctx.insertInto(Tables.USERS)
          .set(Tables.USERS.ID, userId.toString())
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

      responseObserver.onNext(UserCreateResponse.newBuilder().setId(userId.toString()).build());
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

      var now = LocalDateTime.now(ZoneOffset.UTC);
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

    UserProfileValidation.requireUsername(request.getUsername());
    UserProfileValidation.requireEmail(request.getEmail());

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
          .set(Tables.USERS.UPDATED_AT, LocalDateTime.now(ZoneOffset.UTC))
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

  @Override
  public void listUserPluginPermissionGrants(
    ListUserPluginPermissionGrantsRequest request,
    StreamObserver<ListUserPluginPermissionGrantsResponse> responseObserver
  ) {
    ServerRPCConstants.USER_CONTEXT_KEY.get()
      .hasPermissionOrThrow(PermissionContext.global(GlobalPermission.READ_USER));
    try {
      var userId = UUID.fromString(request.getUserId());
      requireUser(userId);
      var grants = soulFireServer.dsl()
        .selectFrom(Tables.PLUGIN_PERMISSION_GRANTS)
        .where(Tables.PLUGIN_PERMISSION_GRANTS.USER_ID.eq(userId.toString()))
        .orderBy(
          Tables.PLUGIN_PERMISSION_GRANTS.PERMISSION_ID,
          Tables.PLUGIN_PERMISSION_GRANTS.SCOPE,
          Tables.PLUGIN_PERMISSION_GRANTS.RESOURCE_ID
        )
        .fetch()
        .map(UserServiceImpl::toPluginPermissionGrant);
      responseObserver.onNext(ListUserPluginPermissionGrantsResponse.newBuilder()
        .addAllGrants(grants)
        .build());
      responseObserver.onCompleted();
    } catch (IllegalArgumentException e) {
      throw Status.INVALID_ARGUMENT.withDescription(e.getMessage()).withCause(e).asRuntimeException();
    }
  }

  @Override
  public void setUserPluginPermissionGrant(
    SetUserPluginPermissionGrantRequest request,
    StreamObserver<UserPluginPermissionGrant> responseObserver
  ) {
    ServerRPCConstants.USER_CONTEXT_KEY.get()
      .hasPermissionOrThrow(PermissionContext.global(GlobalPermission.UPDATE_USER));
    try {
      var userId = UUID.fromString(request.getUserId());
      mutateOrThrow(userId);
      requireUser(userId);
      var permission = SoulFireAPI.pluginApis()
        .findPermission(request.getPermissionId())
        .orElseThrow(() -> new IllegalArgumentException(
          "Plugin permission is not registered: " + request.getPermissionId()));
      var target = PluginPermissionGrantScope.fromRequest(
        request.getScope(),
        request.hasResourceId() ? java.util.Optional.of(request.getResourceId()) : java.util.Optional.empty()
      );
      var declaredScope = PluginPermissionGrantScope.toProto(permission.definition().scope());
      if (declaredScope != request.getScope()) {
        throw new IllegalArgumentException(
          "Permission %s uses scope %s, not %s".formatted(
            permission.id(),
            declaredScope,
            request.getScope()
          ));
      }

      var now = LocalDateTime.now(ZoneOffset.UTC);
      soulFireServer.dsl().transaction(configuration -> {
        var ctx = DSL.using(configuration);
        var table = Tables.PLUGIN_PERMISSION_GRANTS;
        var existing = ctx.selectFrom(table)
          .where(table.USER_ID.eq(userId.toString()))
          .and(table.PERMISSION_ID.eq(permission.id()))
          .and(table.SCOPE.eq(target.scope()))
          .and(table.RESOURCE_ID.eq(target.resourceId()))
          .fetchOne();
        if (existing == null) {
          ctx.insertInto(table)
            .set(table.USER_ID, userId.toString())
            .set(table.PERMISSION_ID, permission.id())
            .set(table.SCOPE, target.scope())
            .set(table.RESOURCE_ID, target.resourceId())
            .set(table.GRANTED, request.getGranted())
            .set(table.CREATED_AT, now)
            .set(table.UPDATED_AT, now)
            .execute();
        } else {
          ctx.update(table)
            .set(table.GRANTED, request.getGranted())
            .set(table.UPDATED_AT, now)
            .where(table.USER_ID.eq(userId.toString()))
            .and(table.PERMISSION_ID.eq(permission.id()))
            .and(table.SCOPE.eq(target.scope()))
            .and(table.RESOURCE_ID.eq(target.resourceId()))
            .execute();
        }
      });
      var result = soulFireServer.dsl().selectFrom(Tables.PLUGIN_PERMISSION_GRANTS)
        .where(Tables.PLUGIN_PERMISSION_GRANTS.USER_ID.eq(userId.toString()))
        .and(Tables.PLUGIN_PERMISSION_GRANTS.PERMISSION_ID.eq(permission.id()))
        .and(Tables.PLUGIN_PERMISSION_GRANTS.SCOPE.eq(target.scope()))
        .and(Tables.PLUGIN_PERMISSION_GRANTS.RESOURCE_ID.eq(target.resourceId()))
        .fetchOne();
      if (result == null) {
        throw new IllegalStateException("Plugin permission grant was not persisted");
      }
      responseObserver.onNext(toPluginPermissionGrant(result));
      responseObserver.onCompleted();
    } catch (IllegalArgumentException e) {
      throw Status.INVALID_ARGUMENT.withDescription(e.getMessage()).withCause(e).asRuntimeException();
    }
  }

  @Override
  public void deleteUserPluginPermissionGrant(
    DeleteUserPluginPermissionGrantRequest request,
    StreamObserver<DeleteUserPluginPermissionGrantResponse> responseObserver
  ) {
    ServerRPCConstants.USER_CONTEXT_KEY.get()
      .hasPermissionOrThrow(PermissionContext.global(GlobalPermission.UPDATE_USER));
    try {
      var userId = UUID.fromString(request.getUserId());
      mutateOrThrow(userId);
      requireUser(userId);
      var target = PluginPermissionGrantScope.fromRequest(
        request.getScope(),
        request.hasResourceId() ? java.util.Optional.of(request.getResourceId()) : java.util.Optional.empty()
      );
      soulFireServer.dsl().deleteFrom(Tables.PLUGIN_PERMISSION_GRANTS)
        .where(Tables.PLUGIN_PERMISSION_GRANTS.USER_ID.eq(userId.toString()))
        .and(Tables.PLUGIN_PERMISSION_GRANTS.PERMISSION_ID.eq(request.getPermissionId()))
        .and(Tables.PLUGIN_PERMISSION_GRANTS.SCOPE.eq(target.scope()))
        .and(Tables.PLUGIN_PERMISSION_GRANTS.RESOURCE_ID.eq(target.resourceId()))
        .execute();
      responseObserver.onNext(DeleteUserPluginPermissionGrantResponse.getDefaultInstance());
      responseObserver.onCompleted();
    } catch (IllegalArgumentException e) {
      throw Status.INVALID_ARGUMENT.withDescription(e.getMessage()).withCause(e).asRuntimeException();
    }
  }

  private void requireUser(UUID userId) {
    if (soulFireServer.authSystem().getUserData(userId).isEmpty()) {
      throw new IllegalArgumentException("User not found: " + userId);
    }
  }

  private static UserPluginPermissionGrant toPluginPermissionGrant(
    PluginPermissionGrantsRecord record
  ) {
    var result = UserPluginPermissionGrant.newBuilder()
      .setUserId(record.getUserId())
      .setPermissionId(record.getPermissionId())
      .setScope(PluginPermissionGrantScope.toProto(record.getScope()))
      .setGranted(record.getGranted())
      .setActive(SoulFireAPI.pluginApis().findPermission(record.getPermissionId()).isPresent())
      .setCreatedAt(Timestamps.fromMillis(
        record.getCreatedAt().toInstant(ZoneOffset.UTC).toEpochMilli()))
      .setUpdatedAt(Timestamps.fromMillis(
        record.getUpdatedAt().toInstant(ZoneOffset.UTC).toEpochMilli()));
    if (!record.getResourceId().isEmpty()) {
      result.setResourceId(record.getResourceId());
    }
    return result.build();
  }
}
