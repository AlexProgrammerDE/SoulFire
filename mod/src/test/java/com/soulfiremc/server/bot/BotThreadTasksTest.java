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
package com.soulfiremc.server.bot;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.concurrent.CompletionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BotThreadTasksTest {
  @Test
  void cancelsPendingCallsAndRejectsNewCallsAfterShutdown() {
    var queue = new ArrayDeque<Runnable>();
    var calls = new AtomicInteger();
    var tasks = new BotThreadTasks();
    var completed = tasks.submit(Runnable::run, calls::incrementAndGet);
    var pending = tasks.submit(queue::add, calls::incrementAndGet);
    tasks.close();
    assertEquals(1, completed.join());
    assertTrue(pending.isCancelled());
    assertTrue(tasks.submit(queue::add, calls::incrementAndGet).isCancelled());
    assertEquals(1, queue.size());
    queue.remove().run();
    assertEquals(1, calls.get());
  }

  @Test
  void shutdownBetweenRegistrationAndEnqueueCannotRunTheAction() {
    var queue = new ArrayDeque<Runnable>();
    var calls = new AtomicInteger();
    var tasks = new BotThreadTasks();
    var result = tasks.submit(action -> {
      tasks.close();
      queue.add(action);
    }, calls::incrementAndGet);
    queue.remove().run();
    assertTrue(result.isCancelled());
    assertEquals(0, calls.get());
  }

  @Test
  void callerCancellationPreventsLateMutations() {
    var queue = new ArrayDeque<Runnable>();
    var calls = new AtomicInteger();
    try (var tasks = new BotThreadTasks()) {
      var result = tasks.submit(queue::add, calls::incrementAndGet);
      assertFalse(result.isDone());
      result.cancel(false);
      queue.remove().run();
      assertEquals(0, calls.get());
      var next = tasks.submit(Runnable::run, calls::incrementAndGet);
      assertEquals(1, next.join());
    }
  }

  @Test
  void propagatesActionAndSubmissionFailuresWithoutBlockingOtherCalls() {
    try (var tasks = new BotThreadTasks()) {
      var actionFailure = new IllegalArgumentException();
      var failedAction = tasks.submit(Runnable::run, () -> { throw actionFailure; });
      assertSame(actionFailure, assertThrows(CompletionException.class, failedAction::join).getCause());
      var rejection = new RejectedExecutionException();
      var rejected = tasks.submit(_ -> { throw rejection; }, () -> 1);
      assertSame(rejection, assertThrows(CompletionException.class, rejected::join).getCause());
      assertEquals(42, tasks.submit(Runnable::run, () -> 42).join());
    }
  }
}
