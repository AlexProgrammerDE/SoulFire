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
package com.soulfiremc.server.script.nodes.flow;

import com.soulfiremc.server.script.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/// Flow control node that rate-limits execution.
/// Input: cooldownMs (milliseconds between allowed executions)
/// Input: key (string) - unique key for this debounce instance (allows multiple debounces)
/// Output: allowed (boolean) - whether execution is allowed
/// Output: remainingMs (long) - milliseconds until next allowed execution
///
/// Uses a static map to track last execution times per key.
public final class DebounceNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("flow.debounce")
    .displayName("Debounce")
    .category(NodeCategory.FLOW)
    .addInputs(
      PortDefinition.execIn(),
      PortDefinition.inputWithDefault("cooldownMs", "Cooldown (ms)", PortType.NUMBER, "1000", "Milliseconds between allowed executions"),
      PortDefinition.inputWithDefault("key", "Key", PortType.STRING, "\"default\"", "Unique key for this debounce")
    )
    .addOutputs(
      PortDefinition.execOut(),
      PortDefinition.output("allowed", "Allowed", PortType.BOOLEAN, "Whether execution was allowed"),
      PortDefinition.output("remainingMs", "Remaining (ms)", PortType.NUMBER, "Milliseconds until next allowed execution")
    )
    .description("Rate-limits execution by enforcing a cooldown period")
    .icon("timer")
    .color("#607D8B")
    .addKeywords("debounce", "rate limit", "cooldown", "throttle")
    .build();

  // Track last execution time per key
  private static final Map<String, Long> lastExecutionTimes = new ConcurrentHashMap<>();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var cooldownMs = getLongInput(inputs, "cooldownMs", 1000L);
    var key = getStringInput(inputs, "key", "default");

    var now = System.currentTimeMillis();
    var lastExecution = lastExecutionTimes.getOrDefault(key, 0L);
    var elapsed = now - lastExecution;

    if (elapsed >= cooldownMs) {
      // Allow execution and update timestamp
      lastExecutionTimes.put(key, now);
      return completed(results(
        "allowed", true,
        "remainingMs", 0L
      ));
    } else {
      // Deny execution
      return completed(results(
        "allowed", false,
        "remainingMs", cooldownMs - elapsed
      ));
    }
  }
}
