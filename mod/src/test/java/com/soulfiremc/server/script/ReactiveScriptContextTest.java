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
package com.soulfiremc.server.script;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

final class ReactiveScriptContextTest {
  private final ReactiveScriptContext context = new ReactiveScriptContext(ScriptEventListener.NOOP);

  @Test
  void invocationIdentifiersAreUniqueAndFactoryFailuresReleaseTracking() {
    var ids = new ArrayList<Long>();
    var operation = context.trackNode("shared", id -> {
      ids.add(id);
      return Mono.never();
    });
    var first = operation.subscribe();
    var second = operation.subscribe();
    assertEquals(2, ids.stream().distinct().count());
    assertTrue(ids.stream().allMatch(id -> id > 0));
    first.dispose();
    second.dispose();
    context.trackNode("failure", _ -> { throw new IllegalStateException(); }).onErrorComplete().block();
    assertTrue(context.activeNodeId().isEmpty());
  }

  @Test
  void recorderForwardsInvocationIdentityAndTiming() {
    ScriptEventListener listener = mock();
    var recorder = new ExecutionRecorder(listener);
    recorder.onNodeStarted("node", 7);
    recorder.onNodeCompleted("node", Map.of(), 9, 7);
    recorder.onNodeError("node", "failure", 8);
    verify(listener).onNodeStarted("node", 7);
    verify(listener).onNodeCompleted("node", Map.of(), 9, 7);
    verify(listener).onNodeError("node", "failure", 8);
    assertEquals(3, recorder.getEvents().size());
  }

  @Test
  void completionFallsBackToAnOlderNodeAndCancellationKeepsOtherInvocations() {
    var older = context.trackNode("older", _ -> Mono.never()).subscribe();
    var first = context.trackNode("shared", _ -> Mono.never()).subscribe();
    var last = context.trackNode("shared", _ -> Mono.never()).subscribe();
    assertEquals("shared", context.activeNodeId().orElseThrow());
    first.dispose();
    assertEquals("shared", context.activeNodeId().orElseThrow());
    last.dispose();
    assertEquals("older", context.activeNodeId().orElseThrow());
    older.dispose();
    assertTrue(context.activeNodeId().isEmpty());
  }

  @Test
  void synchronousCompletionEmptyResultsAndFailuresReleaseTracking() {
    for (var operation : List.of(Mono.just(1), Mono.empty(), Mono.error(new IllegalStateException()))) {
      var tracked = context.trackNode("node", _ -> operation);
      assertTrue(context.activeNodeId().isEmpty());
      tracked.onErrorComplete().block();
      assertTrue(context.activeNodeId().isEmpty());
    }
    var operation = Sinks.one();
    var subscription = context.trackNode("waiting", _ -> operation.asMono()).subscribe();
    assertEquals("waiting", context.activeNodeId().orElseThrow());
    operation.tryEmitEmpty();
    assertTrue(context.activeNodeId().isEmpty());
    subscription.dispose();
  }

  @Test
  void cancellingContextDropsTrackingAndPreventsLateSubscriptions() {
    var active = context.trackNode("active", _ -> Mono.never()).subscribe();
    context.cancel();
    assertTrue(context.activeNodeId().isEmpty());
    var executed = new AtomicBoolean();
    context.trackNode("late", _ -> Mono.fromRunnable(() -> executed.set(true))).block();
    active.dispose();
    assertFalse(executed.get());
    assertTrue(context.activeNodeId().isEmpty());
  }
}
