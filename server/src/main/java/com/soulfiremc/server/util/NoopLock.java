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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import org.jetbrains.annotations.NotNull;

public class NoopLock implements Lock {
  @Override
  public void lock() {}

  @Override
  public void lockInterruptibly() {}

  @Override
  public boolean tryLock() {
    return true;
  }

  @Override
  public boolean tryLock(long time, @NotNull TimeUnit unit) {
    return true;
  }

  @Override
  public void unlock() {}

  @NotNull
  @Override
  public Condition newCondition() {
    throw new UnsupportedOperationException();
  }
}
