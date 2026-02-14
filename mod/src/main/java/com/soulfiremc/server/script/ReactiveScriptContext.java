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
package com.soulfiremc.server.script;

import com.soulfiremc.server.InstanceManager;
import com.soulfiremc.server.SoulFireScheduler;
import lombok.Getter;
import reactor.core.Disposable;
import reactor.core.scheduler.Scheduler;

/// Reactive execution context for scripts.
/// Per-invocation state (output sinks) lives in ExecutionRun, not here.
@Getter
public final class ReactiveScriptContext {
  private final ScriptStateStore stateStore = new ScriptStateStore();
  private final InstanceManager instance;
  private final Scheduler reactorScheduler;
  private final ScriptEventListener eventListener;

  /// For cancellation via Disposable.
  private volatile Disposable execution;
  private volatile boolean cancelled;

  /// Creates a new reactive script context.
  ///
  /// @param instance      the SoulFire instance
  /// @param eventListener listener for script execution events
  public ReactiveScriptContext(InstanceManager instance, ScriptEventListener eventListener) {
    this.instance = instance;
    this.reactorScheduler = new SoulFireReactorScheduler(instance.scheduler());
    this.eventListener = eventListener;
  }

  public SoulFireScheduler scheduler() {
    return instance.scheduler();
  }

  public void log(String level, String message) {
    eventListener.onLog(level, message);
  }

  /// Gets the Reactor scheduler for reactive operations.
  ///
  /// @return the Reactor scheduler
  public Scheduler getReactorScheduler() {
    return reactorScheduler;
  }

  /// Checks if execution has been cancelled.
  ///
  /// @return true if cancelled
  public boolean isCancelled() {
    return cancelled;
  }

  /// Cancels the script execution.
  public void cancel() {
    this.cancelled = true;
    if (execution != null) {
      execution.dispose();
    }
    eventListener.onScriptCancelled();
  }

  /// Sets the main execution disposable for cancellation.
  ///
  /// @param execution the disposable to set
  public void setExecution(Disposable execution) {
    this.execution = execution;
  }
}
