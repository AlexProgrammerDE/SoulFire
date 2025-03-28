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
import com.soulfiremc.server.user.PermissionContext;
import com.soulfiremc.server.user.SoulFireUser;
import com.soulfiremc.server.util.log4j.SFLogAppender;
import com.soulfiremc.server.util.structs.CachedLazyObject;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public final class LogServiceImpl extends LogsServiceGrpc.LogsServiceImplBase {
  private final SoulFireServer soulFireServer;
  private final Map<UUID, ConnectionMessageSender> subscribers = new ConcurrentHashMap<>();

  public LogServiceImpl(SoulFireServer soulFireServer) {
    this.soulFireServer = soulFireServer;
    SFLogAppender.INSTANCE.logConsumers().add(this::broadcastMessage);
  }

  private static LogString fromEvent(SFLogAppender.SFLogEvent event) {
    var builder = LogString.newBuilder()
      .setId(event.id())
      .setMessage(event.message());

    if (event.instanceId() != null) {
      builder.setInstanceId(event.instanceId().toString());
    }

    if (event.botAccountId() != null) {
      builder.setBotId(event.botAccountId().toString());
    }

    return builder.build();
  }

  public void broadcastMessage(SFLogAppender.SFLogEvent message) {
    for (var sender : subscribers.values()) {
      if (sender.eventPredicate().test(message)) {
        sender.sendMessage(fromEvent(message));
      }
    }
  }

  public void sendMessage(UUID uuid, String message) {
    var messageId = UUID.randomUUID();
    subscribers.values().stream()
      .filter(sender -> sender.userId().equals(uuid))
      .forEach(sender -> sender.sendMessage(LogString.newBuilder()
        .setId(messageId.toString())
        .setMessage(message)
        .build()));
  }

  public void disconnect(UUID uuid) {
    subscribers.values().stream()
      .filter(sender -> sender.userId().equals(uuid))
      .forEach(sender -> sender.responseObserver().onCompleted());

    subscribers.entrySet().removeIf(entry -> entry.getValue().userId().equals(uuid));
  }

  @Override
  public void getPrevious(PreviousLogRequest request, StreamObserver<PreviousLogResponse> responseObserver) {
    validateScopeAccess(request.getScope());

    try {
      var predicate = eventPredicate(request.getScope());
      responseObserver.onNext(PreviousLogResponse.newBuilder()
        .addAllMessages(SFLogAppender.INSTANCE.logs().getNewest(request.getCount())
          .stream()
          .filter(predicate::test)
          .map(LogServiceImpl::fromEvent)
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
      var user = new CachedLazyObject<>(() -> soulFireServer.authSystem().authenticateBySubject(userId, issuedAt), 1, TimeUnit.SECONDS);
      var predicate = eventPredicate(request.getScope());
      new ConnectionMessageSender(
        subscribers,
        ServerRPCConstants.USER_CONTEXT_KEY.get().getUniqueId(),
        (ServerCallStreamObserver<LogResponse>) responseObserver,
        event -> user.get().filter(soulFireUser -> hasScopeAccess(soulFireUser, request.getScope())
          && predicate.test(event)).isPresent()
      );
    } catch (Throwable t) {
      log.error("Error subscribing to logs", t);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(t.getMessage()).withCause(t));
    }
  }

  @FunctionalInterface
  public interface EventPredicate {
    boolean test(SFLogAppender.SFLogEvent event);
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
      case INSTANCE, BOT, INSTANCE_SCRIPT -> {
        var instanceId = UUID.fromString(scope.getInstance().getInstanceId());
        yield user.hasPermission(PermissionContext.instance(InstancePermission.INSTANCE_SUBSCRIBE_LOGS, instanceId));
      }
      case SCOPE_NOT_SET -> throw new IllegalArgumentException("Scope not set");
    };
  }

  private EventPredicate eventPredicate(LogScope scope) {
    return switch (scope.getScopeCase()) {
      case GLOBAL -> event -> true;
      case GLOBAL_SCRIPT -> {
        var scriptId = UUID.fromString(scope.getGlobalScript().getScriptId());
        yield event -> scriptId.equals(event.scriptId());
      }
      case INSTANCE -> {
        var instanceId = UUID.fromString(scope.getInstance().getInstanceId());
        yield event -> instanceId.equals(event.instanceId());
      }
      case BOT -> {
        var instanceId = UUID.fromString(scope.getInstance().getInstanceId());
        var botId = UUID.fromString(scope.getBot().getBotId());
        yield event -> instanceId.equals(event.instanceId()) && botId.equals(event.botAccountId());
      }
      case INSTANCE_SCRIPT -> {
        var instanceId = UUID.fromString(scope.getInstance().getInstanceId());
        var scriptId = UUID.fromString(scope.getInstanceScript().getScriptId());
        yield event -> instanceId.equals(event.instanceId()) && scriptId.equals(event.scriptId());
      }
      case SCOPE_NOT_SET -> event -> false;
    };
  }

  private record ConnectionMessageSender(Map<UUID, ConnectionMessageSender> subscribers,
                                         UUID userId,
                                         ServerCallStreamObserver<LogResponse> responseObserver,
                                         EventPredicate eventPredicate) {
    public ConnectionMessageSender {
      var responseId = UUID.randomUUID();
      subscribers.put(responseId, this);

      responseObserver.setOnCancelHandler(() -> subscribers.remove(responseId));
      responseObserver.setOnCloseHandler(() -> subscribers.remove(responseId));
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
