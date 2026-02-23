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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/// Records script execution events for replay in the editor.
/// Implements ScriptEventListener to capture all events during execution.
public final class ExecutionRecorder implements ScriptEventListener {

  /// Type of recorded event.
  public enum EventType {
    NODE_STARTED, NODE_COMPLETED, NODE_ERROR, LOG, EXECUTION_STATS, SCRIPT_COMPLETED
  }

  /// A single recorded execution event with timestamp and data.
  public record RecordedEvent(
    Instant timestamp,
    EventType type,
    String nodeId,
    Map<String, NodeValue> outputs,
    String message,
    long executionTimeNanos
  ) {}

  private final List<RecordedEvent> events = new ArrayList<>();
  private final ScriptEventListener delegate;

  /// Creates a recorder that also delegates events to another listener.
  public ExecutionRecorder(ScriptEventListener delegate) {
    this.delegate = delegate;
  }

  /// Creates a standalone recorder with no delegation.
  public ExecutionRecorder() {
    this.delegate = null;
  }

  @Override
  public void onNodeStarted(String nodeId) {
    events.add(new RecordedEvent(Instant.now(), EventType.NODE_STARTED, nodeId, Map.of(), null, 0));
    if (delegate != null) {
      delegate.onNodeStarted(nodeId);
    }
  }

  @Override
  public void onNodeCompleted(String nodeId, Map<String, NodeValue> outputs) {
    events.add(new RecordedEvent(Instant.now(), EventType.NODE_COMPLETED, nodeId, outputs, null, 0));
    if (delegate != null) {
      delegate.onNodeCompleted(nodeId, outputs);
    }
  }

  @Override
  public void onNodeCompleted(String nodeId, Map<String, NodeValue> outputs, long executionTimeNanos) {
    events.add(new RecordedEvent(Instant.now(), EventType.NODE_COMPLETED, nodeId, outputs, null, executionTimeNanos));
    if (delegate != null) {
      delegate.onNodeCompleted(nodeId, outputs, executionTimeNanos);
    }
  }

  @Override
  public void onNodeError(String nodeId, String error) {
    events.add(new RecordedEvent(Instant.now(), EventType.NODE_ERROR, nodeId, Map.of(), error, 0));
    if (delegate != null) {
      delegate.onNodeError(nodeId, error);
    }
  }

  @Override
  public void onLog(String level, String message) {
    events.add(new RecordedEvent(Instant.now(), EventType.LOG, null, Map.of(), level + ": " + message, 0));
    if (delegate != null) {
      delegate.onLog(level, message);
    }
  }

  @Override
  public void onExecutionStats(long nodeCount, long maxCount) {
    events.add(new RecordedEvent(Instant.now(), EventType.EXECUTION_STATS, null, Map.of(),
      "nodes: " + nodeCount + "/" + maxCount, 0));
    if (delegate != null) {
      delegate.onExecutionStats(nodeCount, maxCount);
    }
  }

  @Override
  public void onScriptCompleted(boolean success) {
    events.add(new RecordedEvent(Instant.now(), EventType.SCRIPT_COMPLETED, null, Map.of(),
      success ? "success" : "failure", 0));
    if (delegate != null) {
      delegate.onScriptCompleted(success);
    }
  }

  @Override
  public void onScriptCancelled() {
    if (delegate != null) {
      delegate.onScriptCancelled();
    }
  }

  /// Returns the recorded events as an unmodifiable list.
  public List<RecordedEvent> getEvents() {
    return Collections.unmodifiableList(events);
  }

  /// Returns the number of recorded events.
  public int size() {
    return events.size();
  }
}
