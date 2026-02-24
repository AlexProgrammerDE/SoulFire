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
package com.soulfiremc.server.util.netty;

import com.google.common.collect.Iterators;
import com.soulfiremc.server.SoulFireScheduler;
import io.netty.channel.*;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ScheduledFuture;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SuppressWarnings("deprecation")
@RequiredArgsConstructor
public class WrappingEventExecutorGroup implements EventExecutorGroup {
  private final EventExecutorGroup delegate;
  private final SoulFireScheduler.RunnableWrapper runnableWrapper;

  @Override
  public boolean isShuttingDown() {
    return delegate.isShuttingDown();
  }

  @Override
  public Future<?> shutdownGracefully() {
    return delegate.shutdownGracefully();
  }

  @Override
  public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
    return delegate.shutdownGracefully(quietPeriod, timeout, unit);
  }

  @Override
  public Future<?> terminationFuture() {
    return delegate.terminationFuture();
  }

  @Override
  public void shutdown() {
    delegate.shutdown();
  }

  @Override
  public List<Runnable> shutdownNow() {
    return delegate.shutdownNow();
  }

  @Override
  public EventExecutor next() {
    return new WrappingEventExecutor(delegate.next(), runnableWrapper);
  }

  @Override
  public boolean isShutdown() {
    return delegate.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return delegate.isTerminated();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return delegate.awaitTermination(timeout, unit);
  }

  @SuppressWarnings("DataFlowIssue")
  @Override
  public Iterator<EventExecutor> iterator() {
    return Iterators.transform(delegate.iterator(), e -> new WrappingEventExecutor(e, runnableWrapper));
  }

  @Override
  public Future<?> submit(Runnable task) {
    return delegate.submit(runnableWrapper.wrap(task));
  }

  @Override
  public <T> List<java.util.concurrent.Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
    return delegate.invokeAll(tasks.stream()
      .map(runnableWrapper::wrap)
      .toList());
  }

  @Override
  public <T> List<java.util.concurrent.Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
    return delegate.invokeAll(tasks.stream()
      .map(runnableWrapper::wrap)
      .toList(), timeout, unit);
  }

  @Override
  public @NonNull <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
    return delegate.invokeAny(tasks.stream()
      .map(runnableWrapper::wrap)
      .toList());
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    return delegate.invokeAny(tasks.stream()
      .map(runnableWrapper::wrap)
      .toList(), timeout, unit);
  }

  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    return delegate.submit(runnableWrapper.wrap(task), result);
  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    return delegate.submit(runnableWrapper.wrap(task));
  }

  @Override
  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    return delegate.schedule(runnableWrapper.wrap(command), delay, unit);
  }

  @Override
  public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
    return delegate.schedule(runnableWrapper.wrap(callable), delay, unit);
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
    return delegate.scheduleAtFixedRate(runnableWrapper.wrap(command), initialDelay, period, unit);
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
    return delegate.scheduleWithFixedDelay(runnableWrapper.wrap(command), initialDelay, delay, unit);
  }

  @Override
  public void execute(Runnable command) {
    delegate.execute(runnableWrapper.wrap(command));
  }
}
