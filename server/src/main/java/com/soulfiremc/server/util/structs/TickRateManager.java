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

import com.soulfiremc.server.util.TimeUtil;

public class TickRateManager {
  public static final float MIN_TICKRATE = 1.0F;
  protected float tickrate = 20.0F;
  protected long nanosecondsPerTick = TimeUtil.NANOSECONDS_PER_SECOND / 20L;
  protected int frozenTicksToRun = 0;
  protected boolean runGameElements = true;
  protected boolean isFrozen = false;

  public void setTickRate(float tickRate) {
    this.tickrate = Math.max(tickRate, MIN_TICKRATE);
    this.nanosecondsPerTick = (long) ((double) TimeUtil.NANOSECONDS_PER_SECOND / (double) this.tickrate);
  }

  public float tickrate() {
    return this.tickrate;
  }

  public float millisecondsPerTick() {
    return (float) this.nanosecondsPerTick / (float) TimeUtil.NANOSECONDS_PER_MILLISECOND;
  }

  public long nanosecondsPerTick() {
    return this.nanosecondsPerTick;
  }

  public boolean runsNormally() {
    return this.runGameElements;
  }

  public boolean isSteppingForward() {
    return this.frozenTicksToRun > 0;
  }

  public void setFrozenTicksToRun(int frozenTicksToRun) {
    this.frozenTicksToRun = frozenTicksToRun;
  }

  public int frozenTicksToRun() {
    return this.frozenTicksToRun;
  }

  public boolean isFrozen() {
    return this.isFrozen;
  }

  public void setFrozen(boolean frozen) {
    this.isFrozen = frozen;
  }

  public void tick() {
    this.runGameElements = !this.isFrozen || this.frozenTicksToRun > 0;
    if (this.frozenTicksToRun > 0) {
      this.frozenTicksToRun--;
    }
  }
}
