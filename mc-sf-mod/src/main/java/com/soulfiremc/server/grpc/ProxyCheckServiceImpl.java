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

import com.soulfiremc.grpc.generated.InstancePermission;
import com.soulfiremc.grpc.generated.ProxyCheckRequest;
import com.soulfiremc.grpc.generated.ProxyCheckResponse;
import com.soulfiremc.grpc.generated.ProxyCheckServiceGrpc;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.user.PermissionContext;
import com.soulfiremc.server.util.structs.CancellationCollector;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public final class ProxyCheckServiceImpl extends ProxyCheckServiceGrpc.ProxyCheckServiceImplBase {
  private final SoulFireServer soulFireServer;

  @Override
  public void check(
    ProxyCheckRequest request, StreamObserver<ProxyCheckResponse> casted) {
    var responseObserver = (ServerCallStreamObserver<ProxyCheckResponse>) casted;
    var instanceId = UUID.fromString(request.getInstanceId());
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.CHECK_PROXY, instanceId));
    var optionalInstance = soulFireServer.getInstance(instanceId);
    if (optionalInstance.isEmpty()) {
      throw new StatusRuntimeException(Status.NOT_FOUND.withDescription("Instance '%s' not found".formatted(instanceId)));
    }

    var instance = optionalInstance.get();
    var settingsSource = instance.settingsSource();

    var cancellationCollector = new CancellationCollector(responseObserver);
    try {
      // TODO
    } catch (Throwable t) {
      log.error("Error checking proxy", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }
}
