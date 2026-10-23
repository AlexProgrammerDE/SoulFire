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
import com.soulfiremc.server.bot.ControlTask;
import com.soulfiremc.server.user.SoulFireUser;
import com.soulfiremc.test.utils.TestBootstrap;
import net.kyori.adventure.text.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class BotTaskRecoveryTest {
  @BeforeAll
  static void bootstrapMinecraft() {
    TestBootstrap.bootstrapForTest();
  }

  @Test
  void resumesOnceOnlyAfterTheNewClientHasLoaded() {
    try (var fixture = new Fixture()) {
      fixture.login();
      fixture.tick();
      fixture.assertStatus(BotTaskStatus.BOT_TASK_STATUS_RECOVERING);
      fixture.minecraft.player = mock(LocalPlayer.class);
      fixture.tick();
      fixture.assertStatus(BotTaskStatus.BOT_TASK_STATUS_RECOVERING);
      fixture.minecraft.level = mock(ClientLevel.class);
      fixture.tick();
      fixture.assertStatus(BotTaskStatus.BOT_TASK_STATUS_RECOVERING);
      var connection = mock(ClientPacketListener.class);
      when(fixture.minecraft.getConnection()).thenReturn(connection);
      fixture.tick();
      fixture.assertStatus(BotTaskStatus.BOT_TASK_STATUS_RECOVERING);
      verify(fixture.controls, never()).tryStart(any());

      when(connection.hasClientLoaded()).thenReturn(true);
      fixture.tick();
      fixture.assertStatus(BotTaskStatus.BOT_TASK_STATUS_RUNNING);
      fixture.tick();
      verify(fixture.controls, times(1)).tryStart(any());
    }
  }

  @Test
  void doesNotResurrectTasksCancelledWhileWaitingForTheWorld() {
    try (var fixture = new Fixture()) {
      fixture.login();
      fixture.manager.cancel(fixture.taskId, "Cancel during login");
      fixture.load();
      fixture.tick();
      fixture.assertStatus(BotTaskStatus.BOT_TASK_STATUS_CANCELLED);
      verify(fixture.controls, never()).tryStart(any());
    }
  }

  @Test
  void ignoresTicksAndLateLoginFromADisconnectingConnection() {
    try (var fixture = new Fixture()) {
      fixture.login();
      when(fixture.bot.isDisconnecting()).thenReturn(true);
      SoulFireAPI.postEvent(new BotDisconnectedEvent(fixture.bot, Component.empty()));
      // Login and disconnect notifications can overlap on different threads.
      fixture.login();
      fixture.load();
      fixture.tick();
      fixture.assertStatus(BotTaskStatus.BOT_TASK_STATUS_RECOVERING);
      verify(fixture.controls, never()).tryStart(any());
    }
  }

  private static final class Fixture implements AutoCloseable {
    private final BotTaskManager manager;
    private final BotConnection bot = mock(BotConnection.class);
    private final BotConnection previous = mock(BotConnection.class);
    private final Minecraft minecraft = mock(Minecraft.class);
    private final BotControlAPI controls = mock(BotControlAPI.class);
    private final UUID taskId;

    private Fixture() {
      var server = mock(SoulFireServer.class);
      var instance = mock(InstanceManager.class);
      var instanceId = UUID.randomUUID();
      var botId = UUID.randomUUID();
      when(server.getInstance(instanceId)).thenReturn(Optional.of(instance));
      when(instance.id()).thenReturn(instanceId);
      when(instance.soulFireServer()).thenReturn(server);
      when(instance.botConnections()).thenReturn(Map.of(botId, previous));
      var oldControls = mock(BotControlAPI.class);
      for (var connection : new BotConnection[]{previous, bot}) {
        when(connection.instanceManager()).thenReturn(instance);
        when(connection.accountProfileId()).thenReturn(botId);
        when(connection.minecraft()).thenReturn(minecraft);
        when(connection.runnableWrapper()).thenReturn((SoulFireScheduler.RunnableWrapper) runnable -> runnable);
      }
      when(minecraft.isSameThread()).thenReturn(true);
      when(previous.botControl()).thenReturn(oldControls);
      when(bot.botControl()).thenReturn(controls);
      for (var control : new BotControlAPI[]{oldControls, controls}) {
        when(control.tryStart(any())).thenAnswer(call -> {
          ((ControlTask) call.getArgument(0)).onStarted();
          return true;
        });
      }
      var owner = mock(SoulFireUser.class);
      when(owner.getUniqueId()).thenReturn(UUID.randomUUID());
      when(owner.getUsername()).thenReturn("owner");
      when(owner.hasPermission(any())).thenReturn(true);
      manager = new BotTaskManager(server);
      taskId = UUID.fromString(manager.start(StartBotTaskRequest.newBuilder()
        .setInstanceId(instanceId.toString()).setBotId(botId.toString())
        .setInput(Any.pack(AutoEatTask.getDefaultInstance()))
        .setReconnectPolicy(BotTaskReconnectPolicy.BOT_TASK_RECONNECT_POLICY_PAUSE_AND_RESUME)
        .build(), owner).getTaskId());
      assertStatus(BotTaskStatus.BOT_TASK_STATUS_RUNNING);
      SoulFireAPI.postEvent(new BotDisconnectedEvent(previous, Component.empty()));
      when(instance.botConnections()).thenReturn(Map.of(botId, bot));
      assertStatus(BotTaskStatus.BOT_TASK_STATUS_RECOVERING);
    }

    private void login() {
      SoulFireAPI.postEvent(new BotConnectedEvent(bot));
    }

    private void load() {
      minecraft.player = mock(LocalPlayer.class);
      minecraft.level = mock(ClientLevel.class);
      var connection = mock(ClientPacketListener.class);
      when(minecraft.getConnection()).thenReturn(connection);
      when(connection.hasClientLoaded()).thenReturn(true);
    }

    private void tick() {
      SoulFireAPI.postEvent(new BotPostEntityTickEvent(bot));
    }

    private void assertStatus(BotTaskStatus status) {
      assertEquals(status, manager.get(taskId).getStatus());
    }

    @Override
    public void close() {
      when(bot.isDisconnected()).thenReturn(true);
      manager.close();
    }
  }
}
