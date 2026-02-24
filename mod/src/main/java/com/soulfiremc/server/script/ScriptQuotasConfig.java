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

import com.soulfiremc.grpc.generated.ScriptQuotas;

import java.time.Duration;

/// Immutable configuration record for script execution quotas.
/// Duration.ZERO means "no timeout" (timeouts disabled).
public record ScriptQuotasConfig(
  long maxExecutionCount,
  Duration dataEdgeTimeout,
  Duration nodeExecutionTimeout,
  int maxConcurrentTriggers,
  long maxStateStoreEntries,
  boolean disableTimeouts
) {
  public static final long DEFAULT_MAX_EXECUTION_COUNT = 100_000;
  public static final Duration DEFAULT_DATA_EDGE_TIMEOUT = Duration.ofSeconds(30);
  public static final Duration DEFAULT_NODE_EXECUTION_TIMEOUT = Duration.ofSeconds(90);
  public static final int DEFAULT_MAX_CONCURRENT_TRIGGERS = 1;
  public static final long DEFAULT_MAX_STATE_STORE_ENTRIES = Long.MAX_VALUE;

  public static final ScriptQuotasConfig DEFAULTS = new ScriptQuotasConfig(
    DEFAULT_MAX_EXECUTION_COUNT,
    DEFAULT_DATA_EDGE_TIMEOUT,
    DEFAULT_NODE_EXECUTION_TIMEOUT,
    DEFAULT_MAX_CONCURRENT_TRIGGERS,
    DEFAULT_MAX_STATE_STORE_ENTRIES,
    false
  );

  /// Creates a ScriptQuotasConfig from a protobuf ScriptQuotas message.
  /// Unset fields use server defaults.
  public static ScriptQuotasConfig fromProto(ScriptQuotas proto) {
    var maxExec = proto.hasMaxExecutionCount() ? proto.getMaxExecutionCount() : DEFAULT_MAX_EXECUTION_COUNT;
    var maxConcurrent = proto.hasMaxConcurrentTriggers() ? proto.getMaxConcurrentTriggers() : DEFAULT_MAX_CONCURRENT_TRIGGERS;
    var maxState = proto.hasMaxStateStoreEntries() ? proto.getMaxStateStoreEntries() : DEFAULT_MAX_STATE_STORE_ENTRIES;
    var disable = proto.getDisableTimeouts();

    Duration dataTimeout;
    Duration nodeTimeout;
    if (disable) {
      dataTimeout = Duration.ZERO;
      nodeTimeout = Duration.ZERO;
    } else {
      dataTimeout = proto.hasMaxExecutionTimeMs()
        ? Duration.ofMillis(proto.getMaxExecutionTimeMs())
        : DEFAULT_DATA_EDGE_TIMEOUT;
      nodeTimeout = proto.hasMaxExecutionTimeMs()
        ? Duration.ofMillis(proto.getMaxExecutionTimeMs())
        : DEFAULT_NODE_EXECUTION_TIMEOUT;
    }

    return new ScriptQuotasConfig(maxExec, dataTimeout, nodeTimeout, maxConcurrent, maxState, disable);
  }
}
