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
package com.soulfiremc.server;

import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.event.Level;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/// Lightweight scheduler for async tasks.
/// Used for most of the async tasks in the server, bots and plugins.
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
  private final AtomicBoolean blockNewTasks = new AtomicBoolean();
  private final AtomicBoolean isShutdown = new AtomicBoolean();

  public SoulFireScheduler(RunnableWrapper runnableWrapper) {
    this.runnableWrapper = runnableWrapper;

    MANAGEMENT_SERVICE.submit(this::managementTask);
  }

  private void managementTask() {
    if (isShutdown.get()) {
      return;
    }

    synchronized (executionQueue) {
      while (!blockNewTasks.get() && !executionQueue.isEmpty() && executionQueue.first().isReady()) {
        schedule(executionQueue.dequeue().runnable());
      }
    }

    MANAGEMENT_SERVICE.schedule(this::managementTask, 1, TimeUnit.MILLISECONDS);
  }

  public void schedule(Runnable command) {
    if (blockNewTasks.get()) {
      FinalizableRunnable.finalize(command);
      return;
    }

    executor.execute(() -> runCommand(command));
  }

  public void schedule(Runnable command, long delay, TimeUnit unit) {
    if (blockNewTasks.get()) {
      FinalizableRunnable.finalize(command);
      return;
    }

    synchronized (executionQueue) {
      if (blockNewTasks.get()) {
        FinalizableRunnable.finalize(command);
        return;
      }

      executionQueue.enqueue(TimedRunnable.of(command, delay, unit));
    }
  }

  public void scheduleAtFixedRate(Runnable command, long delay, long period, TimeUnit unit) {
    var periodMs = unit.toMillis(period);
    var scheduledTime = System.currentTimeMillis() + unit.toMillis(delay);
    scheduleAtFixedRate(command, scheduledTime, periodMs);
  }

  private void scheduleAtFixedRate(Runnable command, long scheduledTime, long periodMs) {
    scheduleAbsolute(FinalizableRunnable.chainFinalizers(() -> {
      scheduleAtFixedRate(command, scheduledTime + periodMs, periodMs);
      runCommandWithoutFinalize(command);
    }, command), scheduledTime);
  }

  private void scheduleAbsolute(Runnable command, long absoluteTimeMs) {
    if (blockNewTasks.get()) {
      FinalizableRunnable.finalize(command);
      return;
    }

    synchronized (executionQueue) {
      if (blockNewTasks.get()) {
        FinalizableRunnable.finalize(command);
        return;
      }

      executionQueue.enqueue(new TimedRunnable(command, absoluteTimeMs));
    }
  }

  public void scheduleWithFixedDelay(Runnable command, long delay, long period, TimeUnit unit) {
    schedule(FinalizableRunnable.chainFinalizers(() -> {
      runCommandWithoutFinalize(command);
      scheduleWithFixedDelay(command, period, period, unit);
    }, command), delay, unit);
  }

  public void scheduleWithDynamicDelay(Runnable command, LongSupplier delay, TimeUnit unit) {
    schedule(FinalizableRunnable.chainFinalizers(() -> {
      runCommandWithoutFinalize(command);
      scheduleWithDynamicDelay(command, delay, unit);
    }, command), delay.getAsLong(), unit);
  }

  public void drainQueue() {
    synchronized (executionQueue) {
      while (!executionQueue.isEmpty()) {
        FinalizableRunnable.finalize(executionQueue.dequeue().runnable());
      }
    }
  }

  public void shutdown() {
    blockNewTasks.set(true);
    if (!isShutdown.compareAndSet(false, true)) {
      return;
    }

    drainQueue();
    executor.shutdown();
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
      if (blockNewTasks.get()) {
        FinalizableRunnable.finalize(command);
        throw new CompletionException(new CancellationException("Scheduler is shutting down"));
      }

      try {
        runnableWrapper.runWrapped(command);
      } catch (Throwable t) {
        runnableWrapper.runWrapped(() -> log.atLevel(errorLevel).setCause(t).log("Error in async runnable future executor"));
        throw new CompletionException(t);
      }
    };
  }

  private <T> Supplier<T> wrapFuture(Supplier<T> command, Level errorLevel) {
    return () -> {
      if (blockNewTasks.get()) {
        return null;
      }

      try {
        return command.get();
      } catch (Throwable t) {
        runnableWrapper.runWrapped(() -> log.atLevel(errorLevel).setCause(t).log("Error in async supplier future executor"));
        throw new CompletionException(t);
      }
    };
  }

  /// Runs a command without finalizing it. Used by recurring methods where
  /// finalization is handled by the outer [#runCommand(Runnable)] via [FinalizableRunnable#chainFinalizers].
  private void runCommandWithoutFinalize(Runnable command) {
    if (blockNewTasks.get()) {
      return;
    }

    try {
      runnableWrapper.runWrapped(command);
    } catch (Throwable t) {
      runnableWrapper.runWrapped(() -> log.error("Error in async executor", t));
    }
  }

  private void runCommand(Runnable command) {
    if (blockNewTasks.get()) {
      FinalizableRunnable.finalize(command);
      return;
    }

    try {
      runnableWrapper.runWrapped(command);
    } catch (Throwable t) {
      runnableWrapper.runWrapped(() -> log.error("Error in async executor", t));
    } finally {
      FinalizableRunnable.finalize(command);
    }
  }

  @Override
  public void execute(Runnable command) {
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

    default <T> Callable<T> wrap(Callable<T> callable) {
      return () -> {
        var result = new Object() {
          T value;
        };
        final var runnable = (Runnable) () -> {
          try {
            result.value = callable.call();
          } catch (Exception e) {
            throw new CatchableException(e);
          }
        };

        try {
          wrap(runnable).run();
        } catch (CatchableException e) {
          throw e.exception;
        }
        return result.value;
      };
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

    class CatchableException extends RuntimeException {
      private final Exception exception;

      public CatchableException(Exception cause) {
        super(cause);
        this.exception = cause;
      }
    }
  }

  @FunctionalInterface
  public interface RunnableIOException {
    void run() throws IOException;
  }

  public interface FinalizableRunnable extends Runnable {
    static FinalizableRunnable withFinalizer(Runnable runnable, Runnable finalizer) {
      return new FinalizableRunnable() {
        @Override
        public void run() {
          runnable.run();
        }

        @Override
        public void finalizeTask() {
          finalizer.run();
        }
      };
    }

    static FinalizableRunnable chainFinalizers(Runnable runnable, Runnable other) {
      return new FinalizableRunnable() {
        @Override
        public void run() {
          runnable.run();
        }

        @Override
        public void finalizeTask() {
          FinalizableRunnable.finalize(runnable);
          FinalizableRunnable.finalize(other);
        }
      };
    }

    static void finalize(Runnable runnable) {
      if (runnable instanceof FinalizableRunnable finalizableRunnable) {
        finalizableRunnable.finalizeTask();
      }
    }

    /// We run this method either when the task is finished or when the scheduler is shutting down.
    ///
    /// This should be used to clean up resources used by the task.
    /// e.g. locks or file handles.
    void finalizeTask();
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
