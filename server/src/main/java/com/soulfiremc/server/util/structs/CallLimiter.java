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
package com.soulfiremc.server.util.structs;

import java.util.concurrent.TimeUnit;

/**
 * A class that only allows a call to be made once in a given interval.
 */
public class CallLimiter implements Runnable {
  private final Runnable c;
  private final long interval;
  private final boolean skipInitial;
  private volatile long lastCalled;

  public CallLimiter(Runnable c, long interval, TimeUnit unit, boolean skipInitial) {
    this.c = c;
    this.interval = unit.toMillis(interval);
    this.skipInitial = skipInitial;
  }

  @Override
  public void run() {
    if (skipInitial && lastCalled == 0) {
      lastCalled = System.currentTimeMillis();
      return;
    }

    if (lastCalled + interval < System.currentTimeMillis()) {
      lastCalled = System.currentTimeMillis();
      c.run();
    }
  }
}
