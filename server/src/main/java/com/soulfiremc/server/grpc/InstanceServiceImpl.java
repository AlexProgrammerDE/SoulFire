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
import com.soulfiremc.server.InstanceManager;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.api.AttackLifecycle;
import com.soulfiremc.server.settings.lib.SettingsImpl;
import com.soulfiremc.server.user.Permission;
import com.soulfiremc.server.user.Permissions;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class InstanceServiceImpl extends InstanceServiceGrpc.InstanceServiceImplBase {
  private final SoulFireServer soulFireServer;

  private Collection<PermissionMessage> getInstancePermissions(UUID instanceId) {
    var user = ServerRPCConstants.USER_CONTEXT_KEY.get();
    return Permissions.VALUES.stream().flatMap(p -> p instanceof Permission.Instance instance ? Stream.of(instance) : Stream.empty()).map(permission -> PermissionMessage.newBuilder()
        .setId(permission.id())
        .setDescription(permission.description())
        .setGranted(user.hasPermission(permission.context(instanceId)))
        .build())
      .toList();
  }

  @Override
  public void createInstance(InstanceCreateRequest request, StreamObserver<InstanceCreateResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(Permissions.CREATE_INSTANCE.context());

    try {
      var id = soulFireServer.createInstance(request.getFriendlyName());
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
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(Permissions.DELETE_INSTANCE.context(instanceId));

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
        .addAllInstances(soulFireServer.instances().values().stream()
          .filter(instance -> ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermission(Permissions.READ_INSTANCE.context(instance.id())))
          .map(InstanceManager::toProto)
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
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(Permissions.READ_INSTANCE.context(instanceId));

    try {
      var optionalInstance = soulFireServer.getInstance(instanceId);
      if (optionalInstance.isEmpty()) {
        throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)));
      }

      var instance = optionalInstance.get();
      responseObserver.onNext(InstanceInfoResponse.newBuilder()
        .setFriendlyName(instance.friendlyName())
        .setConfig(instance.settingsSource().source().toProto())
        .setState(instance.attackLifecycle().toProto())
        .addAllInstancePermissions(getInstancePermissions(instanceId))
        .build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error getting instance info", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void updateInstanceFriendlyName(InstanceUpdateFriendlyNameRequest request, StreamObserver<InstanceUpdateFriendlyNameResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(Permissions.UPDATE_INSTANCE.context(instanceId));

    try {
      var optionalInstance = soulFireServer.getInstance(instanceId);
      if (optionalInstance.isEmpty()) {
        throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)));
      }

      var instance = optionalInstance.get();
      instance.friendlyName(request.getFriendlyName());

      responseObserver.onNext(InstanceUpdateFriendlyNameResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error updating instance state", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void updateInstanceConfig(InstanceUpdateConfigRequest request, StreamObserver<InstanceUpdateConfigResponse> responseObserver) {
    var instanceId = UUID.fromString(request.getId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(Permissions.UPDATE_INSTANCE.context(instanceId));

    try {
      var optionalInstance = soulFireServer.getInstance(instanceId);
      if (optionalInstance.isEmpty()) {
        throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)));
      }

      var instance = optionalInstance.get();
      instance.settingsSource().source(SettingsImpl.fromProto(request.getConfig()));

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
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(Permissions.CHANGE_INSTANCE_STATE.context(instanceId));

    try {
      var optionalInstance = soulFireServer.getInstance(instanceId);
      if (optionalInstance.isEmpty()) {
        throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)));
      }

      var instance = optionalInstance.get();
      instance.switchToState(AttackLifecycle.fromProto(request.getState())).join();
      responseObserver.onNext(InstanceStateChangeResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error changing instance state", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }
}
