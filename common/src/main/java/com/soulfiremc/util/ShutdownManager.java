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
package com.soulfiremc.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginManager;

@Slf4j
@RequiredArgsConstructor
public class ShutdownManager {
  private final Runnable shutdownHook;
  private final PluginManager pluginManager;
  private final AtomicBoolean shutdownInProgress = new AtomicBoolean(false);
  private final CompletableFuture<Void> shutdownFuture = new CompletableFuture<>();

  /**
   * Shuts down the software if it is running.
   *
   * @param explicitExit whether the user explicitly shut down the software
   */
  public void shutdownSoftware(boolean explicitExit) {
    if (!shutdownInProgress.compareAndSet(false, true)) {
      return;
    }

    if (explicitExit) {
      log.info("Shutting down...");
    }

    shutdownHook.run();

    pluginManager.stopPlugins();
    pluginManager.unloadPlugins();

    shutdownFuture.complete(null);

    if (explicitExit) {
      System.exit(0);
    }
  }

  public void awaitShutdown() {
    shutdownFuture.join();
  }

  public boolean shutdown() {
    return shutdownFuture.isDone();
  }
}
