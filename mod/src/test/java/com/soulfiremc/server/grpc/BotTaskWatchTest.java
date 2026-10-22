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

import com.soulfiremc.grpc.generated.BotTask;
import com.soulfiremc.grpc.generated.BotTaskEvent;
import com.soulfiremc.grpc.generated.BotTaskStatus;
import com.soulfiremc.grpc.generated.WatchBotTaskRequest;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.task.BotTaskManager;
import com.soulfiremc.server.user.SoulFireUser;
import io.grpc.Context;
import io.grpc.stub.ServerCallStreamObserver;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class BotTaskWatchTest {
  @ParameterizedTest
  @EnumSource(value = BotTaskStatus.class, names = {
    "BOT_TASK_STATUS_RUNNING", "BOT_TASK_STATUS_COMPLETED", "BOT_TASK_STATUS_CANCELLED"
  })
  void catchesUpdatesBetweenTheInitialSnapshotAndSubscription(BotTaskStatus status) throws Exception {
    var server = mock(SoulFireServer.class);
    var manager = mock(BotTaskManager.class);
    var user = mock(SoulFireUser.class);
    var subscription = mock(AutoCloseable.class);
    var taskId = UUID.randomUUID();
    var initial = BotTask.newBuilder().setTaskId(taskId.toString())
      .setInstanceId(UUID.randomUUID().toString()).setRevision(1)
      .setStatus(BotTaskStatus.BOT_TASK_STATUS_QUEUED).build();
    var updated = initial.toBuilder().setRevision(2).setStatus(status).build();
    var current = new AtomicReference<>(initial);
    when(server.botTaskManager()).thenReturn(manager);
    when(manager.get(taskId)).thenAnswer(_ -> current.get());
    when(manager.subscribe(any())).thenReturn(subscription);
    // A task can finish while an ownership check reads the database.
    doAnswer(_ -> {
      current.set(updated);
      return null;
    }).when(user).hasPermissionOrThrow(any());
    ServerCallStreamObserver<BotTaskEvent> observer = mock();
    var request = WatchBotTaskRequest.newBuilder().setTaskId(taskId.toString()).setFollow(true).build();
    Context.current().withValue(ServerRPCConstants.USER_CONTEXT_KEY, user).run(() ->
      new BotTaskServiceImpl(server).watchBotTask(request, observer));

    var events = ArgumentCaptor.forClass(BotTaskEvent.class);
    verify(observer, atLeastOnce()).onNext(events.capture());
    assertEquals(updated, events.getAllValues().getLast().getTask());
    if (BotTaskManager.isTerminal(status)) {
      verify(observer).onCompleted();
      verify(subscription).close();
    } else {
      verify(observer, never()).onCompleted();
      verify(subscription, never()).close();
      var cancelled = ArgumentCaptor.forClass(Runnable.class);
      verify(observer).setOnCancelHandler(cancelled.capture());
      cancelled.getValue().run();
      verify(subscription).close();
    }
  }
}
