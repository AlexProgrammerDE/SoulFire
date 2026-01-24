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

import com.google.common.base.Supplier;
import com.google.protobuf.util.Timestamps;
import com.soulfiremc.grpc.generated.*;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.user.PermissionContext;
import com.soulfiremc.server.user.SoulFireUser;
import com.soulfiremc.server.util.structs.CachedLazyObject;
import com.soulfiremc.shared.SFLogAppender;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public final class LogServiceImpl extends LogsServiceGrpc.LogsServiceImplBase {
  private final SoulFireServer soulFireServer;

  private static LogString fromEvent(SFLogAppender.SFLogEvent event, boolean personal) {
    var builder = LogString.newBuilder()
      .setId(event.id())
      .setMessage(event.message())
      .setPersonal(personal)
      .setTimestamp(Timestamps.fromMillis(event.timestamp()));

    if (event.instanceId() != null) {
      builder.setInstanceId(event.instanceId().toString());
    }

    if (event.instanceName() != null) {
      builder.setInstanceName(event.instanceName());
    }

    if (event.botAccountId() != null) {
      builder.setBotAccountId(event.botAccountId().toString());
    }

    if (event.botAccountName() != null) {
      builder.setBotAccountName(event.botAccountName());
    }

    if (event.scriptId() != null) {
      builder.setScriptId(event.scriptId().toString());
    }

    if (event.loggerName() != null) {
      builder.setLoggerName(event.loggerName());
    }

    if (event.level() != null) {
      builder.setLevel(event.level());
    }

    return builder.build();
  }

  @Override
  public void getPrevious(PreviousLogRequest request, StreamObserver<PreviousLogResponse> responseObserver) {
    validateScopeAccess(request.getScope());

    try {
      var predicate = eventPredicate(request.getScope());
      responseObserver.onNext(PreviousLogResponse.newBuilder()
        .addAllMessages(SFLogAppender.INSTANCE.logs().getNewest(request.getCount())
          .stream()
          .filter(log -> predicate.test(log, false))
          .map(e -> fromEvent(e, false))
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
    validateScopeAccess(request.getScope());

    try {
      var userId = ServerRPCConstants.USER_CONTEXT_KEY.get().getUniqueId();
      var issuedAt = ServerRPCConstants.USER_CONTEXT_KEY.get().getIssuedAt();
      var user = new CachedLazyObject<>((Supplier<Optional<SoulFireUser>>)
        () -> soulFireServer.authSystem().authenticateBySubject(userId, issuedAt), 1, TimeUnit.SECONDS);
      var predicate = eventPredicate(request.getScope());
      new StateHolder.ConnectionMessageSender(
        soulFireServer.logStateHolder(),
        ServerRPCConstants.USER_CONTEXT_KEY.get().getUniqueId(),
        (ServerCallStreamObserver<LogResponse>) responseObserver,
        (event, personal) -> user.get().filter(soulFireUser -> hasScopeAccess(soulFireUser, request.getScope())
          && predicate.test(event, personal)).isPresent()
      );
    } catch (Throwable t) {
      log.error("Error subscribing to logs", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  private void validateScopeAccess(LogScope scope) {
    if (!hasScopeAccess(ServerRPCConstants.USER_CONTEXT_KEY.get(), scope)) {
      throw new StatusRuntimeException(
        Status.PERMISSION_DENIED.withDescription("You do not have permission to access this resource"));
    }
  }

  private boolean hasScopeAccess(SoulFireUser user, LogScope scope) {
    return switch (scope.getScopeCase()) {
      case GLOBAL, GLOBAL_SCRIPT -> user.hasPermission(PermissionContext.global(GlobalPermission.GLOBAL_SUBSCRIBE_LOGS));
      case INSTANCE -> {
        var instanceId = UUID.fromString(scope.getInstance().getInstanceId());
        yield user.hasPermission(PermissionContext.instance(InstancePermission.INSTANCE_SUBSCRIBE_LOGS, instanceId));
      }
      case BOT -> {
        var instanceId = UUID.fromString(scope.getBot().getInstanceId());
        yield user.hasPermission(PermissionContext.instance(InstancePermission.INSTANCE_SUBSCRIBE_LOGS, instanceId));
      }
      case INSTANCE_SCRIPT -> {
        var instanceId = UUID.fromString(scope.getInstanceScript().getInstanceId());
        yield user.hasPermission(PermissionContext.instance(InstancePermission.INSTANCE_SUBSCRIBE_LOGS, instanceId));
      }
      case PERSONAL -> true;
      case SCOPE_NOT_SET -> throw new IllegalArgumentException("Scope not set");
    };
  }

  private EventPredicate eventPredicate(LogScope scope) {
    return switch (scope.getScopeCase()) {
      case GLOBAL -> (_, personal) -> !personal;
      case GLOBAL_SCRIPT -> {
        var scriptId = UUID.fromString(scope.getGlobalScript().getScriptId());
        yield (event, personal) -> !personal && scriptId.equals(event.scriptId());
      }
      case INSTANCE -> {
        var instanceId = UUID.fromString(scope.getInstance().getInstanceId());
        yield (event, personal) -> !personal && instanceId.equals(event.instanceId());
      }
      case BOT -> {
        var instanceId = UUID.fromString(scope.getBot().getInstanceId());
        var botId = UUID.fromString(scope.getBot().getBotId());
        yield (event, personal) -> !personal && instanceId.equals(event.instanceId()) && botId.equals(event.botAccountId());
      }
      case INSTANCE_SCRIPT -> {
        var instanceId = UUID.fromString(scope.getInstanceScript().getInstanceId());
        var scriptId = UUID.fromString(scope.getInstanceScript().getScriptId());
        yield (event, personal) -> !personal && instanceId.equals(event.instanceId()) && scriptId.equals(event.scriptId());
      }
      case PERSONAL -> (_, personal) -> personal;
      case SCOPE_NOT_SET -> (_, _) -> false;
    };
  }

  @FunctionalInterface
  public interface EventPredicate {
    boolean test(SFLogAppender.SFLogEvent event, boolean personal);
  }

  public static class StateHolder {
    private final Map<UUID, ConnectionMessageSender> subscribers = new ConcurrentHashMap<>();

    public StateHolder() {
      SFLogAppender.INSTANCE.logConsumers().add(this::broadcastLogMessage);
    }

    public void sendPersonalMessage(UUID uuid, String message) {
      var messageEvent = new SFLogAppender.SFLogEvent(
        UUID.randomUUID().toString(),
        message,
        System.currentTimeMillis(),
        null,
        null,
        null,
        null,
        null,
        null,
        null
      );
      subscribers.values().stream()
        .filter(sender -> sender.userId().equals(uuid))
        .filter(sender -> sender.eventPredicate().test(messageEvent, true))
        .forEach(sender -> sender.sendMessage(fromEvent(messageEvent, true)));
    }

    public void disconnect(UUID uuid) {
      subscribers.values().stream()
        .filter(sender -> sender.userId().equals(uuid))
        .forEach(sender -> sender.responseObserver().onCompleted());

      subscribers.entrySet().removeIf(entry -> entry.getValue().userId().equals(uuid));
    }

    public void broadcastLogMessage(SFLogAppender.SFLogEvent message) {
      for (var sender : subscribers.values()) {
        if (sender.eventPredicate().test(message, false)) {
          sender.sendMessage(fromEvent(message, false));
        }
      }
    }

    private record ConnectionMessageSender(StateHolder stateHolder,
                                           UUID userId,
                                           ServerCallStreamObserver<LogResponse> responseObserver,
                                           EventPredicate eventPredicate) {
      public ConnectionMessageSender {
        var responseId = UUID.randomUUID();
        stateHolder.subscribers.put(responseId, this);

        responseObserver.setOnCancelHandler(() -> stateHolder.subscribers.remove(responseId));
        responseObserver.setOnCloseHandler(() -> stateHolder.subscribers.remove(responseId));
      }

      public void sendMessage(LogString message) {
        if (responseObserver.isCancelled()) {
          return;
        }

        responseObserver.onNext(LogResponse.newBuilder()
          .setMessage(message)
          .build());
      }
    }
  }
}
