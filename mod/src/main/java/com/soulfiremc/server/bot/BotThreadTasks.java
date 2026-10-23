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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

/// Queued game-thread calls belong to one connection and expire when it starts closing.
final class BotThreadTasks implements AutoCloseable {
  private final Set<Task<?>> pending = new HashSet<>();
  private boolean closed;

  <T> CompletableFuture<T> submit(Executor executor, Callable<T> action) {
    var task = new Task<>(action);
    synchronized (this) {
      if (closed) {
        task.result.cancel(false);
        return task.result;
      }
      pending.add(task);
    }
    task.result.whenComplete((_, _) -> {
      task.action.set(null);
      synchronized (this) {
        pending.remove(task);
      }
    });
    try {
      if (!task.result.isDone()) {
        executor.execute(task);
      }
    } catch (Throwable failure) {
      task.result.completeExceptionally(failure);
    }
    return task.result;
  }

  @Override
  public void close() {
    final Task<?>[] tasks;
    synchronized (this) {
      closed = true;
      tasks = pending.toArray(Task<?>[]::new);
      pending.clear();
    }
    for (var task : tasks) {
      task.result.cancel(false);
    }
  }

  private static final class Task<T> implements Runnable {
    private final AtomicReference<Callable<T>> action;
    private final CompletableFuture<T> result = new CompletableFuture<>();

    private Task(Callable<T> action) {
      this.action = new AtomicReference<>(action);
    }

    @Override
    public void run() {
      var current = action.getAndSet(null);
      if (current == null || result.isDone()) {
        return;
      }
      try {
        result.complete(current.call());
      } catch (Throwable failure) {
        result.completeExceptionally(failure);
      }
    }
  }
}
