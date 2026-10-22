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

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReactiveScriptContextTest {
  private final ReactiveScriptContext context = new ReactiveScriptContext(ScriptEventListener.NOOP);

  @Test
  void completionFallsBackToAnOlderNodeAndCancellationKeepsOtherInvocations() {
    var older = context.trackNode("older", Mono.never()).subscribe();
    var first = context.trackNode("shared", Mono.never()).subscribe();
    var last = context.trackNode("shared", Mono.never()).subscribe();
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
      var tracked = context.trackNode("node", operation);
      assertTrue(context.activeNodeId().isEmpty());
      tracked.onErrorComplete().block();
      assertTrue(context.activeNodeId().isEmpty());
    }
    var operation = Sinks.one();
    var subscription = context.trackNode("waiting", operation.asMono()).subscribe();
    assertEquals("waiting", context.activeNodeId().orElseThrow());
    operation.tryEmitEmpty();
    assertTrue(context.activeNodeId().isEmpty());
    subscription.dispose();
  }

  @Test
  void cancellingContextDropsTrackingAndPreventsLateSubscriptions() {
    var active = context.trackNode("active", Mono.never()).subscribe();
    context.cancel();
    assertTrue(context.activeNodeId().isEmpty());
    var executed = new AtomicBoolean();
    context.trackNode("late", Mono.fromRunnable(() -> executed.set(true))).block();
    active.dispose();
    assertFalse(executed.get());
    assertTrue(context.activeNodeId().isEmpty());
  }
}
