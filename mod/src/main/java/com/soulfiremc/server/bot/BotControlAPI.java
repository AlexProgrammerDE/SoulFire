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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReference;

/// This class is used to control the bot. The goal is to reduce friction for doing simple things.
@Slf4j
@RequiredArgsConstructor
public final class BotControlAPI {
  private final AtomicReference<ControllingTask> controllingTask = new AtomicReference<>();

  public void tick() {
    var localTask = this.controllingTask.get();
    if (localTask != null) {
      if (localTask.isDone()) {
        localTask.stop();
        unregisterControllingTask(localTask);
      } else {
        try {
          localTask.tick();
        } catch (Throwable t) {
          log.error("Error while executing controlling task, unregistering", t);
          localTask.stop();
          unregisterControllingTask(localTask);
          return;
        }

        if (localTask.isDone()) {
          localTask.stop();
          unregisterControllingTask(localTask);
        }
      }
    }
  }

  public boolean stopControllingTask() {
    return this.controllingTask.updateAndGet(
      current -> {
        if (current != null) {
          current.stop();
          return null;
        }

        return null;
      }) != null;
  }

  public boolean activelyControlled() {
    return this.controllingTask.get() != null;
  }

  public void registerControllingTask(ControllingTask task) {
    this.controllingTask.updateAndGet(
      current -> {
        if (current != null) {
          current.stop();
        }

        return task;
      });
  }

  public void unregisterControllingTask(ControllingTask task) {
    this.controllingTask.compareAndSet(task, null);
  }

  public void maybeRegister(ControllingTask task) {
    this.controllingTask.compareAndSet(null, task);
  }

  public <M extends ControllingTask.ManualTaskMarker> M getMarkerAndUnregister(Class<M> clazz) {
    var task = this.controllingTask.get();
    if (task instanceof ControllingTask.ManualControllingTask manual
      && clazz.isInstance(manual.marker())) {
      unregisterControllingTask(task);
      return clazz.cast(manual.marker());
    }

    return null;
  }
}
