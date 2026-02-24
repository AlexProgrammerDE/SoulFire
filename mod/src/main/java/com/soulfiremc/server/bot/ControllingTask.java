/*
 * SoulFire
 * Copyright (C) 2026  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.soulfiremc.server.bot;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.function.LongSupplier;

public interface ControllingTask {
  static ControllingTask singleTick(Runnable runnable) {
    return new SingleTickTask(null, runnable);
  }

  static ControllingTask singleTick(String description, Runnable runnable) {
    return new SingleTickTask(description, runnable);
  }

  static ControllingTask staged(List<Stage> stages) {
    return new StagedTask(stages);
  }

  static ManualControllingTask manual(ManualTaskMarker taskMarker) {
    return new ManualControllingTask(taskMarker);
  }

  void tick();

  void stop();

  boolean isDone();

  /// Returns a human-readable description of this task for error logging.
  /// By default returns null, meaning no extra context is available.
  default @Nullable String description() {
    return null;
  }

  interface Stage {
  }

  interface ManualTaskMarker {
  }

  @RequiredArgsConstructor
  class SingleTickTask implements ControllingTask {
    private final @Nullable String taskDescription;
    private final Runnable runnable;
    private boolean done;

    @Override
    public void tick() {
      if (done) {
        return;
      }

      runnable.run();
      done = true;
    }

    @Override
    public void stop() {
      done = true;
    }

    @Override
    public boolean isDone() {
      return done;
    }

    @Override
    public @Nullable String description() {
      return taskDescription;
    }
  }

  record RunnableStage(Runnable runnable) implements Stage {}

  record WaitDelayStage(LongSupplier delaySupplier) implements Stage {}

  @RequiredArgsConstructor
  class StagedTask implements ControllingTask {
    private final List<Stage> stages;
    private long currentDelay;
    private int currentStage;
    private boolean done;

    @Override
    public void tick() {
      if (done) {
        return;
      }

      var stage = stages.get(currentStage);
      if (stage instanceof RunnableStage(var runnable)) {
        runnable.run();
      } else if (stage instanceof WaitDelayStage(var delaySupplier)) {
        if (currentDelay == 0) {
          currentDelay = System.currentTimeMillis() + delaySupplier.getAsLong();
        }

        // Wait for the delay to pass
        if (System.currentTimeMillis() < currentDelay) {
          return;
        }

        currentDelay = 0;
      }

      if (currentStage >= stages.size() - 1) {
        done = true;
      } else {
        currentStage++;
      }
    }

    @Override
    public void stop() {
      done = true;
    }

    @Override
    public boolean isDone() {
      return done;
    }
  }

  @RequiredArgsConstructor
  class ManualControllingTask implements ControllingTask {
    @Getter
    private final ManualTaskMarker marker;
    private boolean done;

    @Override
    public void tick() {
    }

    @Override
    public void stop() {
      done = true;
    }

    @Override
    public boolean isDone() {
      return done;
    }
  }
}
