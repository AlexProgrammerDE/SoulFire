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
import com.soulfiremc.server.util.SFHelpers;
import com.soulfiremc.server.util.structs.SFLogAppender;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class LogServiceImpl extends LogsServiceGrpc.LogsServiceImplBase {
  private final Map<UUID, ConnectionMessageSender> subscribers = new ConcurrentHashMap<>();

  @Inject
  public LogServiceImpl() {
    SFLogAppender.INSTANCE.logConsumers().add(this::broadcastMessage);
  }

  public void broadcastMessage(SFLogAppender.SFLogEvent message) {
    for (var sender : subscribers.values()) {
      if (sender.eventPredicate().test(message)) {
        sender.sendMessage(message.id(), message.message());
      }
    }
  }

  public void sendMessage(UUID uuid, String message) {
    var sender = subscribers.get(uuid);
    if (sender != null) {
      sender.sendMessage(UUID.randomUUID().toString(), message);
    }
  }

  @Override
  public void getPrevious(PreviousLogRequest request, StreamObserver<PreviousLogResponse> responseObserver) {
    SFHelpers.mustSupply(() -> switch (request.getScopeCase()) {
      case GLOBAL -> () -> ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.global(GlobalPermission.GLOBAL_SUBSCRIBE_LOGS));
      case INSTANCE -> () -> {
        var instanceId = UUID.fromString(request.getInstance().getInstanceId());
        ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.INSTANCE_SUBSCRIBE_LOGS, instanceId));
      };
      case SCOPE_NOT_SET -> () -> {
        throw new IllegalArgumentException("Scope not set");
      };
    });

    try {
      EventPredicate predicate = switch (request.getScopeCase()) {
        case GLOBAL -> event -> true;
        case INSTANCE -> {
          var instanceId = UUID.fromString(request.getInstance().getInstanceId());
          yield event -> instanceId.equals(event.instanceId());
        }
        case SCOPE_NOT_SET -> event -> false;
      };
      responseObserver.onNext(PreviousLogResponse.newBuilder()
        .addAllMessages(SFLogAppender.INSTANCE.logs().getNewest(request.getCount())
          .stream()
          .filter(predicate::test)
          .map(event -> LogString.newBuilder()
            .setId(event.id())
            .setMessage(event.message())
            .build())
          .toList())
        .build());
      responseObserver.onCompleted();
    } catch (Throwable t) {
      log.error("Error getting previous logs", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @Override
  public void subscribe(LogRequest request, StreamObserver<LogResponse> responseObserver) {
    SFHelpers.mustSupply(() -> switch (request.getScopeCase()) {
      case GLOBAL -> () -> ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.global(GlobalPermission.GLOBAL_SUBSCRIBE_LOGS));
      case INSTANCE -> () -> {
        var instanceId = UUID.fromString(request.getInstance().getInstanceId());
        ServerRPCConstants.USER_CONTEXT_KEY.get().hasPermissionOrThrow(PermissionContext.instance(InstancePermission.INSTANCE_SUBSCRIBE_LOGS, instanceId));
      };
      case SCOPE_NOT_SET -> () -> {
        throw new IllegalArgumentException("Scope not set");
      };
    });

    try {
      EventPredicate predicate = switch (request.getScopeCase()) {
        case GLOBAL -> event -> true;
        case INSTANCE -> {
          var instanceId = UUID.fromString(request.getInstance().getInstanceId());
          yield event -> instanceId.equals(event.instanceId());
        }
        case SCOPE_NOT_SET -> event -> false;
      };
      var sender = new ConnectionMessageSender((ServerCallStreamObserver<LogResponse>) responseObserver, predicate);
      subscribers.put(ServerRPCConstants.USER_CONTEXT_KEY.get().getUniqueId(), sender);
    } catch (Throwable t) {
      log.error("Error subscribing to logs", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @FunctionalInterface
  public interface EventPredicate {
    boolean test(SFLogAppender.SFLogEvent event);
  }

  private record ConnectionMessageSender(ServerCallStreamObserver<LogResponse> responseObserver, EventPredicate eventPredicate) {
    public void sendMessage(String id, String message) {
      if (responseObserver.isCancelled()) {
        return;
      }

      responseObserver.onNext(LogResponse.newBuilder()
        .setMessage(LogString.newBuilder()
          .setId(id)
          .setMessage(message))
        .build());
    }
  }
}
