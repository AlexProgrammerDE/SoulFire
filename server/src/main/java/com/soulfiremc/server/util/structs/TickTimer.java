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

import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;

public class TickTimer {
  private final float msPerTick;
  private final FloatUnaryOperator targetMsptProvider;
  private float deltaTicks;
  private float deltaTickResidual;
  private float realtimeDeltaTicks;
  private float pausedDeltaTickResidual;
  private long lastMs;
  private long lastUiMs;
  private boolean paused;
  private boolean frozen;

  public TickTimer(float f, long l, FloatUnaryOperator floatUnaryOperator) {
    this.msPerTick = 1000.0F / f;
    this.lastUiMs = this.lastMs = l;
    this.targetMsptProvider = floatUnaryOperator;
  }

  public int advanceTime(long time, boolean advanceGameTime) {
    this.advanceRealTime(time);
    return advanceGameTime ? this.advanceGameTime(time) : 0;
  }

  private int advanceGameTime(long time) {
    this.deltaTicks = (float) (time - this.lastMs) / this.targetMsptProvider.apply(this.msPerTick);
    this.lastMs = time;
    this.deltaTickResidual = this.deltaTickResidual + this.deltaTicks;
    var i = (int) this.deltaTickResidual;
    this.deltaTickResidual -= (float) i;
    return i;
  }

  private void advanceRealTime(long time) {
    this.realtimeDeltaTicks = (float) (time - this.lastUiMs) / this.msPerTick;
    this.lastUiMs = time;
  }

  public void updatePauseState(boolean paused) {
    if (paused) {
      this.pause();
    } else {
      this.unPause();
    }
  }

  private void pause() {
    if (!this.paused) {
      this.pausedDeltaTickResidual = this.deltaTickResidual;
    }

    this.paused = true;
  }

  private void unPause() {
    if (this.paused) {
      this.deltaTickResidual = this.pausedDeltaTickResidual;
    }

    this.paused = false;
  }

  public void updateFrozenState(boolean frozen) {
    this.frozen = frozen;
  }

  public float getGameTimeDeltaTicks() {
    return this.deltaTicks;
  }

  public float getGameTimeDeltaPartialTick(boolean runsNormally) {
    if (!runsNormally && this.frozen) {
      return 1.0F;
    } else {
      return this.paused ? this.pausedDeltaTickResidual : this.deltaTickResidual;
    }
  }

  public float getRealtimeDeltaTicks() {
    return this.realtimeDeltaTicks > 7.0F ? 0.5F : this.realtimeDeltaTicks;
  }
}
