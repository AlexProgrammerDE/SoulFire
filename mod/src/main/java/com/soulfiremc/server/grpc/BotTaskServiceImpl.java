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

import com.soulfiremc.grpc.generated.*;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.bot.BotControlLeaseManager;
import com.soulfiremc.server.task.BotTaskManager;
import com.soulfiremc.server.user.PermissionContext;
import com.soulfiremc.server.user.SoulFireUser;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/// gRPC facade for the unified core and plugin bot task lifecycle.
@RequiredArgsConstructor
public final class BotTaskServiceImpl
  extends BotTaskServiceGrpc.BotTaskServiceImplBase {
  private final SoulFireServer server;

  @Override
  public void startBotTask(
    StartBotTaskRequest request,
    StreamObserver<BotTask> responseObserver
  ) {
    try {
      var user = user();
      var instanceId = parseUuid(request.getInstanceId(), "instance_id");
      var botId = parseUuid(request.getBotId(), "bot_id");
      user.hasPermissionOrThrow(PermissionContext.instance(
        InstancePermission.CONTROL_BOT_ACTIONS,
        instanceId
      ));
      authorizeLease(instanceId, botId);
      if (request.getDisconnectPolicy()
        == BotTaskDisconnectPolicy.BOT_TASK_DISCONNECT_POLICY_CANCEL_WITH_CALL) {
        throw Status.INVALID_ARGUMENT
          .withDescription(
            "CANCEL_WITH_CALL requires RunBotTask because StartBotTask is unary"
          )
          .asRuntimeException();
      }
      var task = server.botTaskManager().start(request, user);
      responseObserver.onNext(task);
      responseObserver.onCompleted();
    } catch (Throwable throwable) {
      responseObserver.onError(toGrpcError(throwable));
    }
  }

  @Override
  public void runBotTask(
    StartBotTaskRequest request,
    StreamObserver<BotTaskEvent> responseObserver
  ) {
    try {
      var user = user();
      var instanceId = parseUuid(request.getInstanceId(), "instance_id");
      var botId = parseUuid(request.getBotId(), "bot_id");
      user.hasPermissionOrThrow(PermissionContext.instance(
        InstancePermission.CONTROL_BOT_ACTIONS,
        instanceId
      ));
      authorizeLease(instanceId, botId);
      var task = server.botTaskManager().start(request, user);
      var taskId = UUID.fromString(task.getTaskId());
      var observer = (ServerCallStreamObserver<BotTaskEvent>) responseObserver;
      var closed = new AtomicBoolean();
      var lastRevision = new AtomicLong();
      var subscriptionRef = new AtomicReference<AutoCloseable>(() -> {
      });
      var subscription = server.botTaskManager().subscribe(event -> {
        if (!event.getTask().getTaskId().equals(task.getTaskId())
          || event.getTask().getRevision() <= lastRevision.get()) {
          return;
        }
        emit(observer, closed, event, lastRevision);
        if (BotTaskManager.isTerminal(event.getTask().getStatus())) {
          close(observer, subscriptionRef.get(), closed);
        }
      });
      subscriptionRef.set(subscription);
      if (closed.get()) {
        close(subscription, new AtomicBoolean());
        return;
      }
      observer.setOnCancelHandler(() -> {
        close(subscription, closed);
        if (task.getDisconnectPolicy()
          == BotTaskDisconnectPolicy.BOT_TASK_DISCONNECT_POLICY_CANCEL_WITH_CALL) {
          server.botTaskManager().cancel(
            taskId,
            "Owning RunBotTask stream was cancelled"
          );
        }
      });
      emit(observer, closed, eventForSnapshot(task), lastRevision);
      var latest = server.botTaskManager().get(taskId);
      if (latest.getRevision() > lastRevision.get()) {
        emit(observer, closed, eventForSnapshot(latest), lastRevision);
      }
      if (BotTaskManager.isTerminal(latest.getStatus())) {
        close(observer, subscription, closed);
      }
    } catch (Throwable throwable) {
      responseObserver.onError(toGrpcError(throwable));
    }
  }

  @Override
  public void getBotTask(
    GetBotTaskRequest request,
    StreamObserver<BotTask> responseObserver
  ) {
    try {
      var task = server.botTaskManager().get(parseUuid(request.getTaskId(), "task_id"));
      authorizeRead(user(), task);
      responseObserver.onNext(task);
      responseObserver.onCompleted();
    } catch (Throwable throwable) {
      responseObserver.onError(toGrpcError(throwable));
    }
  }

  @Override
  public void listBotTasks(
    ListBotTasksRequest request,
    StreamObserver<ListBotTasksResponse> responseObserver
  ) {
    try {
      var statuses = EnumSet.noneOf(BotTaskStatus.class);
      statuses.addAll(request.getStatusesList());
      if (statuses.contains(BotTaskStatus.UNRECOGNIZED)
        || statuses.contains(BotTaskStatus.BOT_TASK_STATUS_UNSPECIFIED)) {
        throw Status.INVALID_ARGUMENT
          .withDescription("statuses must contain recognized task states")
          .asRuntimeException();
      }
      var result = server.botTaskManager().list(
        request.hasInstanceId()
          ? Optional.of(parseUuid(request.getInstanceId(), "instance_id"))
          : Optional.empty(),
        request.hasBotId()
          ? Optional.of(parseUuid(request.getBotId(), "bot_id"))
          : Optional.empty(),
        statuses,
        request.getIncludeTerminal(),
        request.getPageSize(),
        request.getPageToken(),
        user()
      );
      responseObserver.onNext(ListBotTasksResponse.newBuilder()
        .addAllTasks(result.tasks())
        .setNextPageToken(result.nextPageToken())
        .build());
      responseObserver.onCompleted();
    } catch (Throwable throwable) {
      responseObserver.onError(toGrpcError(throwable));
    }
  }

  @Override
  public void watchBotTask(
    WatchBotTaskRequest request,
    StreamObserver<BotTaskEvent> responseObserver
  ) {
    try {
      var taskId = parseUuid(request.getTaskId(), "task_id");
      var current = server.botTaskManager().get(taskId);
      authorizeRead(user(), current);
      var observer = (ServerCallStreamObserver<BotTaskEvent>) responseObserver;
      if (!request.getFollow()) {
        if (current.getRevision() > request.getAfterRevision()) {
          observer.onNext(eventForSnapshot(current));
        }
        observer.onCompleted();
        return;
      }

      var closed = new AtomicBoolean();
      var lastRevision = new AtomicLong(request.getAfterRevision());
      var subscriptionRef = new AtomicReference<AutoCloseable>(() -> {
      });
      var subscription = server.botTaskManager().subscribe(event -> {
        if (!event.getTask().getTaskId().equals(taskId.toString())
          || event.getTask().getRevision() <= lastRevision.get()) {
          return;
        }
        emit(observer, closed, event, lastRevision);
        if (BotTaskManager.isTerminal(event.getTask().getStatus())) {
          close(observer, subscriptionRef.get(), closed);
        }
      });
      subscriptionRef.set(subscription);
      if (closed.get()) {
        close(subscription, new AtomicBoolean());
        return;
      }
      observer.setOnCancelHandler(() -> close(subscription, closed));
      var latest = server.botTaskManager().get(taskId);
      if (latest.getRevision() > lastRevision.get()) {
        emit(observer, closed, eventForSnapshot(latest), lastRevision);
      }
      if (BotTaskManager.isTerminal(latest.getStatus())) {
        close(observer, subscription, closed);
      }
    } catch (Throwable throwable) {
      responseObserver.onError(toGrpcError(throwable));
    }
  }

  @Override
  public void watchBotTasks(
    WatchBotTasksRequest request,
    StreamObserver<BotTaskEvent> responseObserver
  ) {
    try {
      var user = user();
      var instanceId = request.hasInstanceId()
        ? Optional.of(parseUuid(request.getInstanceId(), "instance_id"))
        : Optional.<UUID>empty();
      var botId = request.hasBotId()
        ? Optional.of(parseUuid(request.getBotId(), "bot_id"))
        : Optional.<UUID>empty();
      if (botId.isPresent() && instanceId.isEmpty()) {
        throw Status.INVALID_ARGUMENT
          .withDescription("bot_id requires instance_id")
          .asRuntimeException();
      }
      instanceId.ifPresent(value -> user.hasPermissionOrThrow(
        PermissionContext.instance(InstancePermission.READ_BOT_INFO, value)
      ));
      var statuses = EnumSet.noneOf(BotTaskStatus.class);
      statuses.addAll(request.getStatusesList());
      statuses.remove(BotTaskStatus.BOT_TASK_STATUS_UNSPECIFIED);
      statuses.remove(BotTaskStatus.UNRECOGNIZED);

      var observer = (ServerCallStreamObserver<BotTaskEvent>) responseObserver;
      var closed = new AtomicBoolean();
      var lastSequence = new AtomicLong(request.getAfterSequence());
      var subscription = server.botTaskManager().subscribe(event -> {
        if (matches(event.getTask(), instanceId, botId, statuses, user)
          && event.getSequence() > lastSequence.get()) {
          emitSequence(observer, closed, event, lastSequence);
        }
      });
      observer.setOnCancelHandler(() -> close(subscription, closed));

      var earliest = server.botTaskManager().earliestRetainedSequence();
      if (request.getAfterSequence() >= earliest - 1) {
        server.botTaskManager().eventsAfter(request.getAfterSequence()).stream()
          .filter(event -> matches(event.getTask(), instanceId, botId, statuses, user))
          .forEach(event -> emitSequence(observer, closed, event, lastSequence));
      } else if (request.getIncludeSnapshot()) {
        emitTaskSnapshot(
          observer,
          closed,
          lastSequence,
          instanceId,
          botId,
          statuses,
          user
        );
      }
    } catch (Throwable throwable) {
      responseObserver.onError(toGrpcError(throwable));
    }
  }

  @Override
  public void cancelBotTask(
    CancelBotTaskRequest request,
    StreamObserver<BotTask> responseObserver
  ) {
    try {
      var taskId = parseUuid(request.getTaskId(), "task_id");
      var current = server.botTaskManager().get(taskId);
      authorizeControl(user(), current);
      authorizeLease(
        UUID.fromString(current.getInstanceId()),
        UUID.fromString(current.getBotId())
      );
      responseObserver.onNext(
        server.botTaskManager().cancel(taskId, request.getReason())
      );
      responseObserver.onCompleted();
    } catch (Throwable throwable) {
      responseObserver.onError(toGrpcError(throwable));
    }
  }

  private void emitTaskSnapshot(
    ServerCallStreamObserver<BotTaskEvent> observer,
    AtomicBoolean closed,
    AtomicLong lastSequence,
    Optional<UUID> instanceId,
    Optional<UUID> botId,
    EnumSet<BotTaskStatus> statuses,
    SoulFireUser user
  ) {
    var pageToken = "";
    do {
      var page = server.botTaskManager().list(
        instanceId,
        botId,
        statuses,
        true,
        500,
        pageToken,
        user
      );
      for (var task : page.tasks()) {
        emitSequence(observer, closed, eventForSnapshot(task), lastSequence);
      }
      pageToken = page.nextPageToken();
    } while (!pageToken.isBlank() && !closed.get());
  }

  private static boolean matches(
    BotTask task,
    Optional<UUID> instanceId,
    Optional<UUID> botId,
    EnumSet<BotTaskStatus> statuses,
    SoulFireUser user
  ) {
    var taskInstanceId = UUID.fromString(task.getInstanceId());
    return instanceId.map(value -> value.equals(taskInstanceId)).orElse(true)
      && botId.map(value -> value.toString().equals(task.getBotId())).orElse(true)
      && (statuses.isEmpty() || statuses.contains(task.getStatus()))
      && user.hasPermission(PermissionContext.instance(
        InstancePermission.READ_BOT_INFO,
        taskInstanceId
      ));
  }

  private static BotTaskEvent eventForSnapshot(BotTask task) {
    return BotTaskEvent.newBuilder()
      .setSequence(0)
      .setObservedAt(task.getUpdatedAt())
      .setTask(task)
      .build();
  }

  private static void emit(
    ServerCallStreamObserver<BotTaskEvent> observer,
    AtomicBoolean closed,
    BotTaskEvent event,
    AtomicLong lastRevision
  ) {
    synchronized (observer) {
      if (closed.get() || observer.isCancelled()) {
        return;
      }
      observer.onNext(event);
      lastRevision.set(event.getTask().getRevision());
    }
  }

  private static void emitSequence(
    ServerCallStreamObserver<BotTaskEvent> observer,
    AtomicBoolean closed,
    BotTaskEvent event,
    AtomicLong lastSequence
  ) {
    synchronized (observer) {
      if (closed.get() || observer.isCancelled()) {
        return;
      }
      observer.onNext(event);
      lastSequence.accumulateAndGet(event.getSequence(), Math::max);
    }
  }

  private static void close(
    ServerCallStreamObserver<BotTaskEvent> observer,
    AtomicBoolean closed
  ) {
    synchronized (observer) {
      if (closed.compareAndSet(false, true) && !observer.isCancelled()) {
        observer.onCompleted();
      }
    }
  }

  private static void close(AutoCloseable subscription, AtomicBoolean closed) {
    if (!closed.compareAndSet(false, true)) {
      return;
    }
    try {
      subscription.close();
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to close task subscription", exception);
    }
  }

  private static void close(
    ServerCallStreamObserver<BotTaskEvent> observer,
    AutoCloseable subscription,
    AtomicBoolean closed
  ) {
    try {
      subscription.close();
    } catch (Exception exception) {
      observer.onError(Status.INTERNAL
        .withDescription("Failed to close task subscription")
        .withCause(exception)
        .asRuntimeException());
      closed.set(true);
      return;
    }
    close(observer, closed);
  }

  private void authorizeLease(UUID instanceId, UUID botId) {
    var instance = server.getInstance(instanceId)
      .orElseThrow(() -> Status.NOT_FOUND
        .withDescription("Instance '%s' not found".formatted(instanceId))
        .asRuntimeException());
    try {
      instance.botControlLeaseManager().authorize(
        botId,
        ServerRPCConstants.BOT_CONTROL_TOKEN_CONTEXT_KEY.get()
      );
    } catch (BotControlLeaseManager.InvalidLeaseException exception) {
      throw Status.PERMISSION_DENIED
        .withDescription(exception.getMessage())
        .withCause(exception)
        .asRuntimeException();
    }
  }

  private static void authorizeRead(SoulFireUser user, BotTask task) {
    user.hasPermissionOrThrow(PermissionContext.instance(
      InstancePermission.READ_BOT_INFO,
      UUID.fromString(task.getInstanceId())
    ));
  }

  private static void authorizeControl(SoulFireUser user, BotTask task) {
    user.hasPermissionOrThrow(PermissionContext.instance(
      InstancePermission.CONTROL_BOT_ACTIONS,
      UUID.fromString(task.getInstanceId())
    ));
  }

  private static SoulFireUser user() {
    return ServerRPCConstants.USER_CONTEXT_KEY.get();
  }

  private static UUID parseUuid(String value, String name) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException exception) {
      throw Status.INVALID_ARGUMENT
        .withDescription("%s must be a UUID".formatted(name))
        .withCause(exception)
        .asRuntimeException();
    }
  }

  private static Throwable toGrpcError(Throwable throwable) {
    if (throwable instanceof StatusRuntimeException) {
      return throwable;
    }
    return Status.INTERNAL
      .withDescription("Bot task operation failed")
      .withCause(throwable)
      .asRuntimeException();
  }
}
