package com.soulfiremc.server.util;

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

  public void setFrozen(boolean frozen) {
    this.isFrozen = frozen;
  }

  public boolean isFrozen() {
    return this.isFrozen;
  }

  public void tick() {
    this.runGameElements = !this.isFrozen || this.frozenTicksToRun > 0;
    if (this.frozenTicksToRun > 0) {
      this.frozenTicksToRun--;
    }
  }
}
