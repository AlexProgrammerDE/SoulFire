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

import com.soulfiremc.grpc.generated.InstanceCreateRequest;
import com.soulfiremc.grpc.generated.InstanceCreateResponse;
import com.soulfiremc.grpc.generated.InstanceDeleteRequest;
import com.soulfiremc.grpc.generated.InstanceDeleteResponse;
import com.soulfiremc.grpc.generated.InstanceInfoRequest;
import com.soulfiremc.grpc.generated.InstanceInfoResponse;
import com.soulfiremc.grpc.generated.InstanceListRequest;
import com.soulfiremc.grpc.generated.InstanceListResponse;
import com.soulfiremc.grpc.generated.InstanceServiceGrpc;
import com.soulfiremc.grpc.generated.InstanceStateChangeRequest;
import com.soulfiremc.grpc.generated.InstanceStateChangeResponse;
import com.soulfiremc.grpc.generated.InstanceUpdateConfigRequest;
import com.soulfiremc.grpc.generated.InstanceUpdateConfigResponse;
import com.soulfiremc.grpc.generated.InstanceUpdateFriendlyNameRequest;
import com.soulfiremc.grpc.generated.InstanceUpdateFriendlyNameResponse;
import com.soulfiremc.server.InstanceManager;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.api.AttackState;
import com.soulfiremc.server.settings.lib.SettingsHolder;
import com.soulfiremc.server.user.Permissions;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.UUID;
import javax.inject.Inject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class InstanceServiceImpl extends InstanceServiceGrpc.InstanceServiceImplBase {
  private final SoulFireServer soulFireServer;

  @Override
  public void createInstance(InstanceCreateRequest request, StreamObserver<InstanceCreateResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(Permissions.CREATE_INSTANCES);

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
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(Permissions.DELETE_INSTANCES);
    var instanceId = UUID.fromString(request.getId());

    try {
      soulFireServer.deleteInstance(instanceId).join();
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
        .addAllInstances(soulFireServer.instances().values().stream().map(InstanceManager::toProto).toList())
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

    try {
      var instance = soulFireServer.getInstance(instanceId);
      responseObserver.onNext(InstanceInfoResponse.newBuilder()
        .setFriendlyName(instance.friendlyName())
        .setConfig(instance.settingsHolder().toProto())
        .setState(instance.attackState().toProto())
        .build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error getting instance info", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void updateInstanceFriendlyName(InstanceUpdateFriendlyNameRequest request, StreamObserver<InstanceUpdateFriendlyNameResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(Permissions.UPDATE_INSTANCES);
    var instanceId = UUID.fromString(request.getId());

    try {
      var instance = soulFireServer.getInstance(instanceId);
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
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(Permissions.UPDATE_INSTANCES);
    var instanceId = UUID.fromString(request.getId());

    try {
      var instance = soulFireServer.getInstance(instanceId);
      instance.settingsHolder(SettingsHolder.fromProto(request.getConfig()));

      responseObserver.onNext(InstanceUpdateConfigResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error updating instance state", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void changeInstanceState(InstanceStateChangeRequest request, StreamObserver<InstanceStateChangeResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(Permissions.CHANGE_INSTANCE_STATE);
    var instanceId = UUID.fromString(request.getId());

    try {
      var instance = soulFireServer.getInstance(instanceId);
      instance.switchToState(AttackState.fromProto(request.getState()));
      responseObserver.onNext(InstanceStateChangeResponse.newBuilder().build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error changing instance state", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }
}
