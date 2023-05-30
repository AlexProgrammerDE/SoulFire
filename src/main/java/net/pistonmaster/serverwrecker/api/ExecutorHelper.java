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
package net.pistonmaster.serverwrecker.api;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ExecutorHelper {
    public static void executeRandomDelaySeconds(ScheduledExecutorService executorService, Runnable runnable,
                                                 int minDelay, int maxDelay) {
        AtomicInteger delay = new AtomicInteger();
        AtomicInteger counter = new AtomicInteger();
        executorService.scheduleAtFixedRate(() -> {
            if (counter.get() == 0) {
                delay.set(ThreadLocalRandom.current().nextInt(minDelay, maxDelay + 1));
            }

            if (counter.get() == delay.get()) {
                runnable.run();
                counter.set(0);
            } else {
                counter.getAndIncrement();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }
}
