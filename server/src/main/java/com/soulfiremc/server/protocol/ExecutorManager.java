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
package com.soulfiremc.server.protocol;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

@Getter
@RequiredArgsConstructor
public class ExecutorManager {
  public static final ThreadLocal<BotConnection> BOT_CONNECTION_THREAD_LOCAL = new ThreadLocal<>();
  private final List<ExecutorService> executors = Collections.synchronizedList(new ArrayList<>());
  private final String threadPrefix;
  private boolean shutdown = false;

  public ScheduledExecutorService newScheduledExecutorService(
    BotConnection botConnection, String threadName) {
    if (shutdown) {
      throw new IllegalStateException("Cannot create new executor after shutdown!");
    }

    var executor =
      new DelegatedScheduledExecutorService(
        Executors.newSingleThreadScheduledExecutor(getThreadFactory(botConnection, threadName)), botConnection);

    executors.add(executor);

    return executor;
  }

  public ExecutorService newExecutorService(BotConnection botConnection, String threadName) {
    if (shutdown) {
      throw new IllegalStateException("Cannot create new executor after shutdown!");
    }

    var executor =
      new DelegatingExecutorService(Executors.newSingleThreadExecutor(getThreadFactory(botConnection, threadName)),
        botConnection);

    executors.add(executor);

    return executor;
  }

  public ExecutorService newFixedExecutorService(
    int threadAmount, BotConnection botConnection, String threadName) {
    if (shutdown) {
      throw new IllegalStateException("Cannot create new executor after shutdown!");
    }

    var executor =
      new DelegatingExecutorService(
        Executors.newFixedThreadPool(threadAmount, getThreadFactory(botConnection, threadName)), botConnection);

    executors.add(executor);

    return executor;
  }

  public ExecutorService newCachedExecutorService(BotConnection botConnection, String threadName) {
    if (shutdown) {
      throw new IllegalStateException("Cannot create new executor after shutdown!");
    }

    var executor =
      new DelegatingExecutorService(Executors.newCachedThreadPool(getThreadFactory(botConnection, threadName)),
        botConnection);

    executors.add(executor);

    return executor;
  }

  private ThreadFactory getThreadFactory(BotConnection botConnection, String threadName) {
    return runnable -> Thread.ofPlatform()
      .name(threadPrefix + "-" + threadName)
      .daemon()
      .unstarted(
        () -> {
          BOT_CONNECTION_THREAD_LOCAL.set(botConnection);
          // Does not directly call the submitted task, the ThreadPoolExecutor wraps the task
          runnable.run();
          BOT_CONNECTION_THREAD_LOCAL.remove();
        });
  }

  public void shutdownAll() {
    shutdown = true;
    executors.forEach(ExecutorService::shutdownNow);
  }

  @RequiredArgsConstructor
  private static class DelegatingExecutorService implements ExecutorService {
    private final ExecutorService delegated;
    private final BotConnection botConnection;

    @Override
    public void shutdown() {
      delegated.shutdown();
    }

    @NotNull
    @Override
    public List<Runnable> shutdownNow() {
      return delegated.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
      return delegated.isShutdown();
    }

    @Override
    public boolean isTerminated() {
      return delegated.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
      return delegated.awaitTermination(timeout, unit);
    }

    @NotNull
    @Override
    public <T> Future<T> submit(@NotNull Callable<T> task) {
      throw new UnsupportedOperationException("Not implemented");
    }

    @NotNull
    @Override
    public <T> Future<T> submit(@NotNull Runnable task, T result) {
      return delegated.submit(wrapCommand(task), result);
    }

    @NotNull
    @Override
    public Future<?> submit(@NotNull Runnable task) {
      return delegated.submit(wrapCommand(task));
    }

    @NotNull
    @Override
    public <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks) {
      throw new UnsupportedOperationException("Not implemented");
    }

    @NotNull
    @Override
    public <T> List<Future<T>> invokeAll(@NotNull Collection<? extends Callable<T>> tasks, long timeout,
                                         @NotNull TimeUnit unit) {
      throw new UnsupportedOperationException("Not implemented");
    }

    @NotNull
    @Override
    public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks) {
      throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> T invokeAny(@NotNull Collection<? extends Callable<T>> tasks, long timeout, @NotNull TimeUnit unit) {
      throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void close() {
      delegated.shutdown();
    }

    @Override
    public void execute(@NotNull Runnable command) {
      delegated.execute(wrapCommand(command));
    }

    protected Runnable wrapCommand(Runnable command) {
      return () -> {
        try {
          command.run();
        } catch (Throwable t) {
          botConnection.logger().error("Error in executor", t);
        }
      };
    }
  }

  private static class DelegatedScheduledExecutorService extends DelegatingExecutorService
    implements ScheduledExecutorService {
    private final ScheduledExecutorService delegated;

    public DelegatedScheduledExecutorService(ScheduledExecutorService delegated, BotConnection botConnection) {
      super(delegated, botConnection);
      this.delegated = delegated;
    }

    @NotNull
    @Override
    public ScheduledFuture<?> schedule(@NotNull Runnable command, long delay, @NotNull TimeUnit unit) {
      return delegated.schedule(wrapCommand(command), delay, unit);
    }

    @NotNull
    @Override
    public <V> ScheduledFuture<V> schedule(@NotNull Callable<V> callable, long delay, @NotNull TimeUnit unit) {
      throw new UnsupportedOperationException("Not implemented");
    }

    @NotNull
    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(@NotNull Runnable command, long initialDelay, long period,
                                                  @NotNull TimeUnit unit) {
      return delegated.scheduleAtFixedRate(wrapCommand(command), initialDelay, period, unit);
    }

    @NotNull
    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(@NotNull Runnable command, long initialDelay, long delay,
                                                     @NotNull TimeUnit unit) {
      return delegated.scheduleWithFixedDelay(wrapCommand(command), initialDelay, delay, unit);
    }
  }
}
