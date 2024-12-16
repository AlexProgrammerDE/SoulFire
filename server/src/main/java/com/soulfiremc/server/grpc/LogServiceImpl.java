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
import com.soulfiremc.server.user.PermissionContext;
import com.soulfiremc.server.util.structs.SFLogAppender;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class LogServiceImpl extends LogsServiceGrpc.LogsServiceImplBase {
  private static final Map<UUID, ConnectionMessageSender> subscribers = new ConcurrentHashMap<>();

  static {
    SFLogAppender.INSTANCE.logConsumers().add(LogServiceImpl::broadcastMessage);
  }

  public static void broadcastMessage(String message) {
    for (var sender : subscribers.values()) {
      sender.sendMessage(message);
    }
  }

  public static void sendMessage(UUID uuid, String message) {
    var sender = subscribers.get(uuid);
    if (sender != null) {
      sender.sendMessage(message);
    }
  }

  @Override
  public void getPrevious(PreviousLogRequest request, StreamObserver<PreviousLogResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.global(GlobalPermission.SUBSCRIBE_LOGS));

    try {
      responseObserver.onNext(PreviousLogResponse.newBuilder()
        .addAllMessages(SFLogAppender.INSTANCE.logs().getNewest(request.getCount()))
        .build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error getting previous logs", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void subscribe(LogRequest request, StreamObserver<LogResponse> responseObserver) {
    ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.global(GlobalPermission.SUBSCRIBE_LOGS));

    try {
      var sender = new ConnectionMessageSender((ServerCallStreamObserver<LogResponse>) responseObserver);
      subscribers.put(ServerRPCConstants.USER_CONTEXT_KEY.get().getUniqueId(), sender);
    } catch (Throwable t) {
      log.error("Error subscribing to logs", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  private record ConnectionMessageSender(ServerCallStreamObserver<LogResponse> responseObserver) {
    public void sendMessage(String message) {
      if (responseObserver.isCancelled()) {
        return;
      }

      responseObserver.onNext(LogResponse.newBuilder().setMessage(message).build());
    }
  }
}
