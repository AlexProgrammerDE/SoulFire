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
package com.soulfiremc.server.util;

import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;

public class TickTimer {
  private final float msPerTick;
  private final FloatUnaryOperator targetMsptProvider;
  public float partialTick;
  public float tickDelta;
  private long lastMs;

  public TickTimer(float f, long l, FloatUnaryOperator floatUnaryOperator) {
    this.msPerTick = 1000.0F / f;
    this.lastMs = l;
    this.targetMsptProvider = floatUnaryOperator;
  }

  public int advanceTime(long gameTime) {
    this.tickDelta = (float) (gameTime - this.lastMs) / this.targetMsptProvider.apply(this.msPerTick);
    this.lastMs = gameTime;
    this.partialTick = this.partialTick + this.tickDelta;
    var i = (int) this.partialTick;
    this.partialTick -= (float) i;
    return i;
  }
}
