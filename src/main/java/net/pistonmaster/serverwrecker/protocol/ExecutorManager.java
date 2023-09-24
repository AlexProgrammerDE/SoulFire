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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

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

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(getThreadFactory(threadName));

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
}
