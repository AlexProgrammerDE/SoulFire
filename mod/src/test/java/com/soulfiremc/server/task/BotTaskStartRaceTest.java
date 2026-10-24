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
package com.soulfiremc.server.task;

import com.google.protobuf.Any;
import com.soulfiremc.grpc.generated.AutoEatTask;
import com.soulfiremc.grpc.generated.BotTask;
import com.soulfiremc.grpc.generated.BotTaskReconnectPolicy;
import com.soulfiremc.grpc.generated.BotTaskStatus;
import com.soulfiremc.grpc.generated.StartBotTaskRequest;
import com.soulfiremc.server.InstanceManager;
import com.soulfiremc.server.SoulFireScheduler;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.api.SoulFireAPI;
import com.soulfiremc.server.api.event.bot.BotConnectedEvent;
import com.soulfiremc.server.api.event.bot.BotDisconnectedEvent;
import com.soulfiremc.server.api.event.bot.BotPostEntityTickEvent;
import com.soulfiremc.server.bot.BotConnection;
import com.soulfiremc.server.bot.BotControlAPI;
import com.soulfiremc.server.user.SoulFireUser;
import com.soulfiremc.test.utils.TestBootstrap;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.kyori.adventure.text.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class BotTaskStartRaceTest {
  @BeforeAll
  static void bootstrapMinecraft() {
    TestBootstrap.bootstrapForTest();
  }

  @ParameterizedTest
  @MethodSource("failures")
  void failureFromRetiredStartCannotFailTheRecoveredExecution(int failingCall, int failureStage) throws Exception {
    var server = mock(SoulFireServer.class);
    var instance = mock(InstanceManager.class);
    var instanceId = UUID.randomUUID();
    var botId = UUID.randomUUID();
    when(server.getInstance(instanceId)).thenReturn(Optional.of(instance));
    when(instance.id()).thenReturn(instanceId);
    when(instance.soulFireServer()).thenReturn(server);
    var previous = bot(instance, botId);
    var replacement = bot(instance, botId);
    when(instance.botConnections()).thenReturn(Map.of(botId, previous));
    var entered = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var calls = new AtomicInteger();
    when(previous.runnableWrapper()).thenReturn((SoulFireScheduler.RunnableWrapper) runnable -> () -> {
      if (calls.incrementAndGet() == failingCall) {
        entered.countDown();
        try {
          release.await();
        } catch (InterruptedException failure) {
          Thread.currentThread().interrupt();
          throw new IllegalStateException(failure);
        }
        throw new IllegalStateException();
      }
      runnable.run();
    });
    var owner = mock(SoulFireUser.class);
    when(owner.getUniqueId()).thenReturn(UUID.randomUUID());
    when(owner.getUsername()).thenReturn("owner");
    when(owner.hasPermission(any())).thenReturn(true);
    try (var manager = new BotTaskManager(server)) {
      var started = new CompletableFuture<BotTask>();
      var caller = Thread.ofVirtual().start(() -> {
        try {
          started.complete(manager.start(StartBotTaskRequest.newBuilder()
            .setInstanceId(instanceId.toString()).setBotId(botId.toString())
            .setInput(Any.pack(AutoEatTask.getDefaultInstance()))
            .setReconnectPolicy(BotTaskReconnectPolicy.BOT_TASK_RECONNECT_POLICY_PAUSE_AND_RESUME)
            .build(), owner));
        } catch (Throwable failure) {
          started.completeExceptionally(failure);
        }
      });
      try {
        assertTrue(entered.await(2, TimeUnit.SECONDS));
        var accepted = manager.list(Optional.empty(), Optional.empty(), Set.of(), true, 10, "", owner)
          .tasks().getFirst();
        var taskId = UUID.fromString(accepted.getTaskId());
        if (failureStage > 0) {
          SoulFireAPI.postEvent(new BotDisconnectedEvent(previous, Component.empty()));
          assertEquals(BotTaskStatus.BOT_TASK_STATUS_RECOVERING, manager.get(taskId).getStatus());
        } else {
          when(previous.isDisconnecting()).thenReturn(true);
        }
        Runnable recover = () -> {
          when(instance.botConnections()).thenReturn(Map.of(botId, replacement));
          SoulFireAPI.postEvent(new BotConnectedEvent(replacement));
          SoulFireAPI.postEvent(new BotPostEntityTickEvent(replacement));
          assertEquals(BotTaskStatus.BOT_TASK_STATUS_RUNNING, manager.get(taskId).getStatus());
        };
        if (failureStage == 2) {
          recover.run();
        }
        var expected = switch (failureStage) {
          case 0 -> BotTaskStatus.BOT_TASK_STATUS_QUEUED;
          case 1 -> BotTaskStatus.BOT_TASK_STATUS_RECOVERING;
          default -> BotTaskStatus.BOT_TASK_STATUS_RUNNING;
        };
        release.countDown();
        assertEquals(expected, started.get(2, TimeUnit.SECONDS).getStatus());
        assertEquals(expected, manager.get(taskId).getStatus());
        if (failureStage == 0) {
          SoulFireAPI.postEvent(new BotDisconnectedEvent(previous, Component.empty()));
          assertEquals(BotTaskStatus.BOT_TASK_STATUS_RECOVERING, manager.get(taskId).getStatus());
        }
        if (failureStage < 2) {
          recover.run();
        }
        assertTrue(replacement.botControl().hasActiveTask());
        manager.cancel(taskId, "test cleanup");
        assertEquals(BotTaskStatus.BOT_TASK_STATUS_CANCELLED, manager.get(taskId).getStatus());
        assertFalse(replacement.botControl().hasActiveTask());
      } finally {
        release.countDown();
        caller.interrupt();
        caller.join();
      }
    }
  }

  @Test
  void rejectsStartsOnAClosingConnectionBeforeCreatingHistory() {
    var server = mock(SoulFireServer.class);
    var instance = mock(InstanceManager.class);
    var instanceId = UUID.randomUUID();
    var botId = UUID.randomUUID();
    var bot = bot(instance, botId);
    when(server.getInstance(instanceId)).thenReturn(Optional.of(instance));
    when(instance.botConnections()).thenReturn(Map.of(botId, bot));
    when(bot.isDisconnecting()).thenReturn(true);
    var owner = mock(SoulFireUser.class);
    when(owner.getUniqueId()).thenReturn(UUID.randomUUID());
    when(owner.getUsername()).thenReturn("owner");
    when(owner.hasPermission(any())).thenReturn(true);
    try (var manager = new BotTaskManager(server)) {
      var failure = assertThrows(StatusRuntimeException.class, () -> manager.start(StartBotTaskRequest.newBuilder()
        .setInstanceId(instanceId.toString()).setBotId(botId.toString())
        .setInput(Any.pack(AutoEatTask.getDefaultInstance())).build(), owner));
      assertEquals(Status.Code.FAILED_PRECONDITION, failure.getStatus().getCode());
      assertTrue(manager.list(Optional.empty(), Optional.empty(), Set.of(), true, 10, "", owner).tasks().isEmpty());
    }
  }

  private static Stream<Arguments> failures() {
    return Stream.of(1, 2).flatMap(call -> Stream.of(0, 1, 2)
      .map(stage -> Arguments.of(call, stage)));
  }

  private static BotConnection bot(InstanceManager instance, UUID botId) {
    var bot = mock(BotConnection.class);
    var minecraft = mock(Minecraft.class);
    minecraft.player = mock(LocalPlayer.class);
    minecraft.level = mock(ClientLevel.class);
    var listener = mock(ClientPacketListener.class);
    when(listener.hasClientLoaded()).thenReturn(true);
    when(minecraft.getConnection()).thenReturn(listener);
    when(minecraft.isSameThread()).thenReturn(true);
    when(bot.minecraft()).thenReturn(minecraft);
    when(bot.instanceManager()).thenReturn(instance);
    when(bot.accountProfileId()).thenReturn(botId);
    when(bot.botControl()).thenReturn(new BotControlAPI());
    var scheduler = mock(SoulFireScheduler.class);
    doAnswer(call -> {
      ((Runnable) call.getArgument(0)).run();
      return null;
    }).when(scheduler).execute(any(Runnable.class));
    when(bot.scheduler()).thenReturn(scheduler);
    when(bot.runnableWrapper()).thenReturn((SoulFireScheduler.RunnableWrapper) runnable -> runnable);
    return bot;
  }
}
