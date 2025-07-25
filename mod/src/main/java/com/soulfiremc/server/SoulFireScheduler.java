/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server;

import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.event.Level;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.*;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Lightweight scheduler for async tasks.
 * Used for most of the async tasks in the server, bots and plugins.
 */
@Slf4j
public final class SoulFireScheduler implements Executor {
  private static final ScheduledExecutorService MANAGEMENT_SERVICE = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual()
    .name("SoulFireScheduler-Management-", 0)
    .factory());
  private final ExecutorService executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
    .name("SoulFireScheduler-Task-", 0)
    .factory());
  private final PriorityQueue<TimedRunnable> executionQueue = new ObjectHeapPriorityQueue<>();
  private final RunnableWrapper runnableWrapper;
  private boolean blockNewTasks = false;
  private boolean isShutdown = false;

  public SoulFireScheduler(RunnableWrapper runnableWrapper) {
    this.runnableWrapper = runnableWrapper;

    MANAGEMENT_SERVICE.submit(this::managementTask);
  }

  private void managementTask() {
    if (isShutdown) {
      return;
    }

    synchronized (executionQueue) {
      while (!blockNewTasks && !executionQueue.isEmpty() && executionQueue.first().isReady()) {
        var timedRunnable = executionQueue.dequeue();
        schedule(() -> runCommand(timedRunnable.runnable()));
      }
    }

    MANAGEMENT_SERVICE.schedule(this::managementTask, 1, TimeUnit.MILLISECONDS);
  }

  public void schedule(Runnable command) {
    if (blockNewTasks) {
      return;
    }

    executor.execute(() -> runCommand(command));
  }

  public void schedule(Runnable command, long delay, TimeUnit unit) {
    if (blockNewTasks) {
      return;
    }

    synchronized (executionQueue) {
      if (blockNewTasks) {
        return;
      }

      executionQueue.enqueue(TimedRunnable.of(command, delay, unit));
    }
  }

  public void scheduleAtFixedRate(Runnable command, long delay, long period, TimeUnit unit) {
    schedule(() -> {
      scheduleAtFixedRate(command, period, period, unit);
      runCommand(command);
    }, delay, unit);
  }

  public void scheduleWithFixedDelay(Runnable command, long delay, long period, TimeUnit unit) {
    schedule(() -> {
      runCommand(command);
      scheduleWithFixedDelay(command, period, period, unit);
    }, delay, unit);
  }

  public void scheduleWithDynamicDelay(Runnable command, LongSupplier delay, TimeUnit unit) {
    schedule(() -> {
      runCommand(command);
      scheduleWithDynamicDelay(command, delay, unit);
    }, delay.getAsLong(), unit);
  }

  public void drainQueue() {
    synchronized (executionQueue) {
      executionQueue.clear();
    }
  }

  public void shutdown() {
    blockNewTasks = true;
    isShutdown = true;
    drainQueue();
  }

  public CompletableFuture<?> runAsync(Runnable command) {
    return runAsync(command, Level.ERROR);
  }

  public <T> CompletableFuture<T> supplyAsync(Supplier<T> command) {
    return supplyAsync(command, Level.ERROR);
  }

  public CompletableFuture<?> runAsync(Runnable command, Level errorLevel) {
    return CompletableFuture.runAsync(wrapFuture(command, errorLevel), this);
  }

  public <T> CompletableFuture<T> supplyAsync(Supplier<T> command, Level errorLevel) {
    return CompletableFuture.supplyAsync(wrapFuture(command, errorLevel), this);
  }

  private Runnable wrapFuture(Runnable command, Level errorLevel) {
    return () -> {
      if (blockNewTasks) {
        return;
      }

      try {
        runnableWrapper.runWrapped(command);
      } catch (Throwable t) {
        runnableWrapper.runWrapped(() -> log.atLevel(errorLevel).log("Error in async executor", t));
        throw new CompletionException(t);
      }
    };
  }

  private <T> Supplier<T> wrapFuture(Supplier<T> command, Level errorLevel) {
    return () -> {
      if (blockNewTasks) {
        return null;
      }

      try {
        return command.get();
      } catch (Throwable t) {
        runnableWrapper.runWrapped(() -> log.atLevel(errorLevel).log("Error in async executor", t));
        throw new CompletionException(t);
      }
    };
  }

  private void runCommand(Runnable command) {
    if (blockNewTasks) {
      return;
    }

    try {
      runnableWrapper.runWrapped(command);
    } catch (Throwable t) {
      runnableWrapper.runWrapped(() -> log.error("Error in async executor", t));
    }
  }

  public void blockNewTasks() {
    blockNewTasks = true;
  }

  @Override
  public void execute(@NonNull Runnable command) {
    schedule(command);
  }

  @FunctionalInterface
  public interface RunnableWrapper {
    Runnable wrap(Runnable runnable);

    default RunnableWrapper with(RunnableWrapper child) {
      return runnable -> child.wrap(wrap(runnable));
    }

    default void runWrapped(Runnable runnable) {
      wrap(runnable).run();
    }

    default void runWrappedWithIOException(RunnableIOException runnable) throws IOException {
      try {
        wrap(() -> {
          try {
            runnable.run();
          } catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        }).run();
      } catch (UncheckedIOException e) {
        throw e.getCause();
      }
    }
  }

  @FunctionalInterface
  public interface RunnableIOException {
    void run() throws IOException;
  }

  private record TimedRunnable(Runnable runnable, long time) implements Comparable<TimedRunnable> {
    public static TimedRunnable of(Runnable runnable, long delay, TimeUnit unit) {
      return new TimedRunnable(runnable, System.currentTimeMillis() + unit.toMillis(delay));
    }

    @Override
    public int compareTo(TimedRunnable o) {
      return Long.compare(time, o.time);
    }

    public boolean isReady() {
      return System.currentTimeMillis() >= time;
    }
  }
}
