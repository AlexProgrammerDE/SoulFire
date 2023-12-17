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
package net.pistonmaster.serverwrecker.server.api;

import net.pistonmaster.serverwrecker.server.util.RandomUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ExecutorHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutorHelper.class);

    private ExecutorHelper() {
    }

    public static void executeRandomDelaySeconds(ScheduledExecutorService executorService, Runnable runnable,
                                                 int minDelay, int maxDelay) {
        var delay = new AtomicInteger();
        var counter = new AtomicInteger();
        executorService.scheduleWithFixedDelay(() -> {
            if (counter.get() == 0) {
                delay.set(RandomUtil.getRandomInt(minDelay, maxDelay));
            }

            if (counter.get() == delay.get()) {
                try {
                    runnable.run();
                } catch (Throwable t) {
                    LOGGER.error("Error while executing task!", t);
                }

                counter.set(0);
            } else {
                counter.getAndIncrement();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }
}
