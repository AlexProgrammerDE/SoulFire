/*
 * ServerWrecker
 *
 * Copyright (C) 2023 ServerWrecker
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package net.pistonmaster.serverwrecker.protocol;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

@Getter
@RequiredArgsConstructor
public class ExecutorManager {
    private final List<ExecutorService> executors = Collections.synchronizedList(new ArrayList<>());
    private final String threadPrefix;
    private boolean shutdown = false;

    public ScheduledExecutorService newScheduledExecutorService(String threadName) {
        if (shutdown) {
            throw new IllegalStateException("Cannot create new executor after shutdown!");
        }

        ScheduledExecutorService executor = new FinalizableScheduledDelegatedExecutorService(
                Executors.newSingleThreadScheduledExecutor(getThreadFactory(threadName)));

        executors.add(executor);

        return executor;
    }

    public ExecutorService newExecutorService(String threadName) {
        if (shutdown) {
            throw new IllegalStateException("Cannot create new executor after shutdown!");
        }

        ExecutorService executor = Executors.newSingleThreadExecutor(getThreadFactory(threadName));

        executors.add(executor);

        return executor;
    }

    private ThreadFactory getThreadFactory(String threadName) {
        return runnable -> {
            Thread thread = new Thread(runnable);
            String usedThreadName = threadName;
            if (runnable instanceof NamedRunnable named) {
                usedThreadName = named.name();
            }
            thread.setName(threadPrefix + "-" + usedThreadName);
            return thread;
        };
    }

    public void shutdownAll() {
        shutdown = true;
        executors.forEach(ExecutorService::shutdownNow);
    }

    private record NamedRunnable(Runnable runnable, String name) implements Runnable {
        @Override
        public void run() {
            runnable.run();
        }
    }

    @SuppressWarnings("NullableProblems")
    private static class DelegatedExecutorService
            implements ExecutorService {
        private final ExecutorService e;

        DelegatedExecutorService(ExecutorService executor) {
            e = executor;
        }

        public void execute(Runnable command) {
            e.execute(command);
        }

        public void shutdown() {
            e.shutdown();
        }

        public List<Runnable> shutdownNow() {
            return e.shutdownNow();
        }

        public boolean isShutdown() {
            return e.isShutdown();
        }

        public boolean isTerminated() {
            return e.isTerminated();
        }

        public boolean awaitTermination(long timeout, TimeUnit unit)
                throws InterruptedException {
            return e.awaitTermination(timeout, unit);
        }

        public Future<?> submit(Runnable task) {
            return e.submit(task);
        }

        public <T> Future<T> submit(Callable<T> task) {
            return e.submit(task);
        }

        public <T> Future<T> submit(Runnable task, T result) {
            return e.submit(task, result);
        }

        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
                throws InterruptedException {
            return e.invokeAll(tasks);
        }

        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
                                             long timeout, TimeUnit unit)
                throws InterruptedException {
            return e.invokeAll(tasks, timeout, unit);
        }

        public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
                throws InterruptedException, ExecutionException {
            return e.invokeAny(tasks);
        }

        public <T> T invokeAny(Collection<? extends Callable<T>> tasks,
                               long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            return e.invokeAny(tasks, timeout, unit);
        }
    }

    @SuppressWarnings("NullableProblems")
    private static class DelegatedScheduledExecutorService
            extends DelegatedExecutorService
            implements ScheduledExecutorService {
        private final ScheduledExecutorService e;

        DelegatedScheduledExecutorService(ScheduledExecutorService executor) {
            super(executor);
            e = executor;
        }

        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            return e.schedule(command, delay, unit);
        }

        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
            return e.schedule(callable, delay, unit);
        }

        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            return e.scheduleAtFixedRate(command, initialDelay, period, unit);
        }

        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
            return e.scheduleWithFixedDelay(command, initialDelay, delay, unit);
        }
    }

    private static class FinalizableScheduledDelegatedExecutorService
            extends DelegatedScheduledExecutorService {
        FinalizableScheduledDelegatedExecutorService(ScheduledExecutorService executor) {
            super(executor);
        }

        @SuppressWarnings("removal")
        protected void finalize() {
            super.shutdown();
        }
    }
}
