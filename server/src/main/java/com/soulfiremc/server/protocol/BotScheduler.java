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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BotScheduler {
  public static final ThreadLocal<BotConnection> BOT_CONNECTION_THREAD_LOCAL = new ThreadLocal<>();
  @Getter
  private final ScheduledExecutorService mainThreadExecutor;
  private final BotConnection botConnection;
  private boolean shutdown = false;

  public BotScheduler(BotConnection botConnection) {
    this.botConnection = botConnection;
    this.mainThreadExecutor = Executors.newScheduledThreadPool(1, runnable -> Thread.ofPlatform()
      .name("SoulFire-Scheduler-Bot-" + botConnection.accountName())
      .daemon()
      .unstarted(
        () -> {
          BOT_CONNECTION_THREAD_LOCAL.set(botConnection);
          runnable.run();
          BOT_CONNECTION_THREAD_LOCAL.remove();
        }));
  }

  public void schedule(Runnable command) {
    if (shutdown) {
      return;
    }

    mainThreadExecutor.execute(wrapCommand(command));
  }

  public void schedule(Runnable command, long delay, TimeUnit unit) {
    if (shutdown) {
      return;
    }

    mainThreadExecutor.schedule(wrapCommand(command), delay, unit);
  }

  public void scheduleAtFixedRate(Runnable command, long delay, long period, TimeUnit unit) {
    if (shutdown) {
      return;
    }

    mainThreadExecutor.scheduleAtFixedRate(wrapCommand(command), delay, period, unit);
  }

  public void scheduleWithFixedDelay(Runnable command, long delay, long period, TimeUnit unit) {
    if (shutdown) {
      return;
    }

    mainThreadExecutor.scheduleWithFixedDelay(wrapCommand(command), delay, period, unit);
  }

  public void shutdown() {
    shutdown = true;
    mainThreadExecutor.shutdown();
  }

  private Runnable wrapCommand(Runnable command) {
    return () -> {
      try {
        command.run();
      } catch (Throwable t) {
        botConnection.logger().error("Error in executor", t);
      }
    };
  }
}
