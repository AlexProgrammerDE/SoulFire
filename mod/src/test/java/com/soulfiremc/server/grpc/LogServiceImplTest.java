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

import com.soulfiremc.grpc.generated.BotLogScope;
import com.soulfiremc.grpc.generated.InstanceLogScope;
import com.soulfiremc.grpc.generated.InstanceScriptLogScope;
import com.soulfiremc.grpc.generated.LogScope;
import com.soulfiremc.grpc.generated.LogString;
import com.soulfiremc.grpc.generated.PreviousLogRequest;
import com.soulfiremc.grpc.generated.PreviousLogResponse;
import com.soulfiremc.server.SoulFireServer;
import com.soulfiremc.server.user.PermissionContext;
import com.soulfiremc.server.user.SoulFireUser;
import com.soulfiremc.shared.SFLogAppender;
import io.grpc.Context;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class LogServiceImplTest {
  private enum Scope { INSTANCE, BOT, SCRIPT }

  private final LogServiceImpl service = new LogServiceImpl(mock(SoulFireServer.class));
  private final SoulFireUser user = mock();
  private final UUID instanceId = UUID.randomUUID();
  private final UUID botId = UUID.randomUUID();
  private final UUID scriptId = UUID.randomUUID();

  @ParameterizedTest
  @EnumSource(Scope.class)
  void historyLimitsTheNewestMatchingEventsRatherThanTheGlobalTail(Scope kind) {
    when(user.hasPermission(any(PermissionContext.class))).thenReturn(true);
    var expected = new ArrayList<String>();
    for (var i = 0; i < 4; i++) {
      expected.add(publish(instanceId, botId, scriptId));
      publish(kind == Scope.INSTANCE ? UUID.randomUUID() : instanceId,
        kind == Scope.BOT ? UUID.randomUUID() : botId,
        kind == Scope.SCRIPT ? UUID.randomUUID() : scriptId);
    }
    var scope = switch (kind) {
      case INSTANCE -> instanceScope();
      case BOT -> LogScope.newBuilder().setBot(BotLogScope.newBuilder()
        .setInstanceId(instanceId.toString()).setBotId(botId.toString())).build();
      case SCRIPT -> LogScope.newBuilder().setInstanceScript(InstanceScriptLogScope.newBuilder()
        .setInstanceId(instanceId.toString()).setScriptId(scriptId.toString())).build();
    };

    assertEquals(expected.subList(2, 4), history(scope, 2).stream().map(LogString::getId).toList());
    assertEquals(expected, history(scope, 300).stream().map(LogString::getId).toList());
    assertTrue(history(scope, 0).isEmpty());
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, Integer.MIN_VALUE, 301, Integer.MAX_VALUE})
  void invalidCountsAreBadRequests(int count) {
    when(user.hasPermission(any(PermissionContext.class))).thenReturn(true);

    var error = assertThrows(StatusRuntimeException.class, () -> history(instanceScope(), count));

    assertEquals(Status.Code.INVALID_ARGUMENT, error.getStatus().getCode());
  }

  @Test
  void scopeAccessIsRequiredEvenWhenNoMessagesAreRequested() {
    var error = assertThrows(StatusRuntimeException.class, () -> history(instanceScope(), 0));

    assertEquals(Status.Code.PERMISSION_DENIED, error.getStatus().getCode());
  }

  private LogScope instanceScope() {
    return LogScope.newBuilder().setInstance(InstanceLogScope.newBuilder().setInstanceId(instanceId.toString())).build();
  }

  private String publish(UUID instance, UUID bot, UUID script) {
    var id = UUID.randomUUID().toString();
    SFLogAppender.INSTANCE.publishLogEvent(new SFLogAppender.SFLogEvent(id, "history", System.currentTimeMillis(),
      null, null, instance, bot, script, null, null));
    return id;
  }

  private List<LogString> history(LogScope scope, int count) {
    var response = new AtomicReference<PreviousLogResponse>();
    Context.current().withValue(ServerRPCConstants.USER_CONTEXT_KEY, user).run(() ->
      service.getPrevious(PreviousLogRequest.newBuilder().setScope(scope).setCount(count).build(), new StreamObserver<>() {
        @Override public void onNext(PreviousLogResponse value) { response.set(value); }
        @Override public void onError(Throwable error) { throw new AssertionError(error); }
        @Override public void onCompleted() { }
      }));
    assertNotNull(response.get());
    return response.get().getMessagesList();
  }
}
