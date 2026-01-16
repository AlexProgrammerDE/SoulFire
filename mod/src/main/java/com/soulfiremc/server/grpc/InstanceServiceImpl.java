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
import com.soulfiremc.server.api.AttackLifecycle;
import com.soulfiremc.server.database.InstanceAuditLogEntity;
import com.soulfiremc.server.database.InstanceEntity;
import com.soulfiremc.server.settings.lib.InstanceSettingsImpl;
import com.soulfiremc.server.user.PermissionContext;
import com.soulfiremc.server.util.SFHelpers;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public final class InstanceServiceImpl extends InstanceServiceGrpc.InstanceServiceImplBase {
  private final SoulFireServer soulFireServer;

  private Collection<InstancePermissionState> getInstancePermissions(UUID instanceId) {
    var user = ServerRPCConstants.USER_CONTEXT_KEY.get();
    return Arrays.stream(InstancePermission.values())
      .filter(permission -> permission != InstancePermission.UNRECOGNIZED)
      .map(permission -> InstancePermissionState.newBuilder()
        .setInstancePermission(permission)
        .setGranted(user.hasPermission(PermissionContext.instance(permission, instanceId)))
        .build())
      .toList();
  }

  @Override
  public void createInstance(InstanceCreateRequest request, StreamObserver<InstanceCreateResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.global(GlobalPermission.CREATE_INSTANCE));

    try {
      var id = soulFireServer.createInstance(request.getFriendlyName(), ServerRPCConstants.USER_CONTEXT_KEY.get());
      responseObserver.onNext(InstanceCreateResponse.newBuilder().setId(id.toString()).build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error creating instance", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void deleteInstance(InstanceDeleteRequest request, StreamObserver<InstanceDeleteResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.DELETE_INSTANCE, instanceId));

    try {
      var optionalDeletion = soulFireServer.deleteInstance(instanceId);
      if (optionalDeletion.isEmpty()) {
        throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)));
      }

      optionalDeletion.get().join();
      responseObserver.onNext(InstanceDeleteResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error deleting instance", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void listInstances(InstanceListRequest request, StreamObserver<InstanceListResponse> responseObserver) {
    try {
      responseObserver.onNext(InstanceListResponse.newBuilder()
        .addAllInstances(soulFireServer.sessionFactory().fromTransaction(session -> session.createQuery("FROM InstanceEntity", InstanceEntity.class).list()).stream()
          .filter(instance -> ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermission(PermissionContext.instance(InstancePermission.READ_INSTANCE, instance.id())))
          .map(instance -> InstanceListResponse.Instance.newBuilder()
            .setId(instance.id().toString())
            .setFriendlyName(instance.friendlyName())
            .setIcon(instance.icon())
            .setState(instance.attackLifecycle().toProto())
            .addAllInstancePermissions(getInstancePermissions(instance.id()))
            .build())
          .toList())
        .build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error listing instance", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void getInstanceInfo(InstanceInfoRequest request, StreamObserver<InstanceInfoResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.READ_INSTANCE, instanceId));

    try {
      var instanceEntity = soulFireServer.sessionFactory().fromTransaction(session -> session.find(InstanceEntity.class, instanceId));
      if (instanceEntity == null) {
        throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)));
      }

      var optionalInstance = soulFireServer.getInstance(instanceId);
      if (optionalInstance.isEmpty()) {
        throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)));
      }

      var instance = optionalInstance.get();
      responseObserver.onNext(InstanceInfoResponse.newBuilder()
        .setFriendlyName(instanceEntity.friendlyName())
        .setIcon(instanceEntity.icon())
        .setConfig(instanceEntity.settings().toProto())
        .setState(instanceEntity.attackLifecycle().toProto())
        .addAllInstancePermissions(getInstancePermissions(instanceId))
        .addAllInstanceSettings(instance.instanceSettingsRegistry().exportSettingsMeta())
        .build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error getting instance info", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void updateInstanceMeta(InstanceUpdateMetaRequest request, StreamObserver<InstanceUpdateMetaResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.UPDATE_INSTANCE_META, instanceId));

    try {
      soulFireServer.sessionFactory().inTransaction(session -> {
        var instanceEntity = session.find(InstanceEntity.class, instanceId);
        if (instanceEntity == null) {
          throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)));
        }

        SFHelpers.mustSupply(() -> switch (request.getMetaCase()) {
          case FRIENDLY_NAME -> () -> instanceEntity.friendlyName(request.getFriendlyName());
          case ICON -> () -> instanceEntity.icon(request.getIcon());
          case META_NOT_SET -> throw new IllegalStateException("Unknown meta type");
        });

        session.merge(instanceEntity);
      });

      responseObserver.onNext(InstanceUpdateMetaResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error updating instance state", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void updateInstanceConfig(InstanceUpdateConfigRequest request, StreamObserver<InstanceUpdateConfigResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.UPDATE_INSTANCE_CONFIG, instanceId));

    try {
      soulFireServer.sessionFactory().inTransaction(session -> {
        var instanceEntity = session.find(InstanceEntity.class, instanceId);
        if (instanceEntity == null) {
          throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)));
        }

        instanceEntity.settings(InstanceSettingsImpl.Stem.fromProto(request.getConfig()));

        session.merge(instanceEntity);
      });

      responseObserver.onNext(InstanceUpdateConfigResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error updating instance state", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void changeInstanceState(InstanceStateChangeRequest request, StreamObserver<InstanceStateChangeResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.CHANGE_INSTANCE_STATE, instanceId));

    try {
      var optionalInstance = soulFireServer.getInstance(instanceId);
      if (optionalInstance.isEmpty()) {
        throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)));
      }

      var instance = optionalInstance.get();
      instance.switchToState(ServerRPCConstants.USER_CONTEXT_KEY.get(), AttackLifecycle.fromProto(request.getState())).join();
      responseObserver.onNext(InstanceStateChangeResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error changing instance state", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void getAuditLog(InstanceAuditLogRequest request, StreamObserver<InstanceAuditLogResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.READ_INSTANCE_AUDIT_LOGS, instanceId));

    try {
      var auditLogs = soulFireServer.sessionFactory().fromTransaction(session -> {
        var instanceEntity = session.find(InstanceEntity.class, instanceId);
        if (instanceEntity == null) {
          throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)));
        }

        return session.createQuery("FROM InstanceAuditLogEntity WHERE instance = :instance ORDER BY createdAt DESC", InstanceAuditLogEntity.class)
          .setParameter("instance", instanceEntity)
          .list();
      });

      var responseBuilder = InstanceAuditLogResponse.newBuilder();
      for (var log : auditLogs) {
        responseBuilder.addEntry(InstanceAuditLogResponse.AuditLogEntry.newBuilder()
          .setId(log.id().toString())
          .setUser(InstanceUser.newBuilder()
            .setId(log.user().id().toString())
            .setUsername(log.user().username())
            .setEmail(log.user().email())
            .build())
          .setType(switch (log.type()) {
            case EXECUTE_COMMAND -> InstanceAuditLogResponse.AuditLogEntryType.EXECUTE_COMMAND;
            case START_ATTACK -> InstanceAuditLogResponse.AuditLogEntryType.START_ATTACK;
            case PAUSE_ATTACK -> InstanceAuditLogResponse.AuditLogEntryType.PAUSE_ATTACK;
            case RESUME_ATTACK -> InstanceAuditLogResponse.AuditLogEntryType.RESUME_ATTACK;
            case STOP_ATTACK -> InstanceAuditLogResponse.AuditLogEntryType.STOP_ATTACK;
          })
          .setTimestamp(Timestamps.fromMillis(log.createdAt().toEpochMilli()))
          .setData(log.data() != null ? log.data() : "")
          .build());
      }

      responseObserver.onNext(responseBuilder.build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error getting audit logs", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }
}
