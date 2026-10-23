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
import com.soulfiremc.grpc.generated.BotTaskConflictPolicy;
import com.soulfiremc.grpc.generated.BotTaskStatus;
import com.soulfiremc.grpc.generated.InstancePermission;
import com.soulfiremc.grpc.generated.StartBotTaskRequest;
import com.soulfiremc.server.InstanceManager;
import com.soulfiremc.server.SoulFireScheduler;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.bot.BotConnection;
import com.soulfiremc.server.bot.BotControlAPI;
import com.soulfiremc.server.user.PermissionContext;
import com.soulfiremc.server.user.SoulFireUser;
import com.soulfiremc.test.utils.TestBootstrap;
import net.minecraft.client.Minecraft;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

final class BotTaskHistoryTest {
  @BeforeAll
  static void bootstrapMinecraft() {
    TestBootstrap.bootstrapForTest();
  }

  @ParameterizedTest
  @ValueSource(ints = {3, 50})
  void checksVisibilityOncePerInstanceAndRechecksItOnLaterRequests(int pageSize) {
    var server = mock(SoulFireServer.class);
    var instanceIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    var owner = mock(SoulFireUser.class);
    when(owner.getUniqueId()).thenReturn(UUID.randomUUID());
    when(owner.getUsername()).thenReturn("owner");
    var visible = new HashSet<>(instanceIds.subList(0, 2));
    var reader = mock(SoulFireUser.class);
    when(reader.hasPermission(any())).thenAnswer(call ->
      visible.contains(((PermissionContext.InstanceContext) call.getArgument(0)).instanceId()));

    try (var manager = new BotTaskManager(server)) {
      for (var instanceId : instanceIds) {
        var botId = UUID.randomUUID();
        var instance = mock(InstanceManager.class);
        var bot = mock(BotConnection.class);
        var minecraft = mock(Minecraft.class);
        when(server.getInstance(instanceId)).thenReturn(Optional.of(instance));
        when(instance.botConnections()).thenReturn(Map.of(botId, bot));
        when(bot.minecraft()).thenReturn(minecraft);
        when(minecraft.isSameThread()).thenReturn(true);
        when(bot.runnableWrapper()).thenReturn((SoulFireScheduler.RunnableWrapper) runnable -> runnable);
        when(bot.botControl()).thenReturn(mock(BotControlAPI.class));
        for (var index = 0; index < 5; index++) {
          var task = manager.start(StartBotTaskRequest.newBuilder()
            .setInstanceId(instanceId.toString()).setBotId(botId.toString())
            .setInput(Any.pack(AutoEatTask.getDefaultInstance()))
            .setConflictPolicy(BotTaskConflictPolicy.BOT_TASK_CONFLICT_POLICY_REJECT).build(), owner);
          assertEquals(BotTaskStatus.BOT_TASK_STATUS_FAILED, task.getStatus());
        }
      }

      var taskIds = new HashSet<String>();
      var pageToken = "";
      var requests = 0;
      do {
        var page = manager.list(Optional.empty(), Optional.empty(), Set.of(), true, pageSize, pageToken, reader);
        for (var task : page.tasks()) {
          assertTrue(visible.contains(UUID.fromString(task.getInstanceId())));
          assertTrue(taskIds.add(task.getTaskId()));
        }
        pageToken = page.nextPageToken();
        requests++;
      } while (!pageToken.isEmpty());
      assertEquals(10, taskIds.size());
      for (var instanceId : instanceIds) {
        verify(reader, times(requests)).hasPermission(PermissionContext.instance(InstancePermission.READ_BOT_INFO, instanceId));
      }

      clearInvocations(reader);
      visible.clear();
      visible.add(instanceIds.getLast());
      var changed = manager.list(Optional.empty(), Optional.empty(), Set.of(), true, 50, "", reader);
      assertEquals(5, changed.tasks().size());
      assertEquals(Set.of(instanceIds.getLast().toString()), changed.tasks().stream()
        .map(task -> task.getInstanceId()).collect(Collectors.toSet()));
      for (var instanceId : instanceIds) {
        verify(reader, times(1)).hasPermission(PermissionContext.instance(InstancePermission.READ_BOT_INFO, instanceId));
      }
    }
  }
}
