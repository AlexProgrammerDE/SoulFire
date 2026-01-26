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
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

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
    .category(CategoryRegistry.FLOW)
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

  // Track last execution time per key using AtomicLong for thread-safe compare-and-set
  private static final Map<String, AtomicLong> lastExecutionTimes = new ConcurrentHashMap<>();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    // Delegate to reactive implementation
    return executeReactive(runtime, inputs).toFuture();
  }

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var cooldownMs = getLongInput(inputs, "cooldownMs", 1000L);
    var key = getStringInput(inputs, "key", "default");

    var now = System.currentTimeMillis();
    var lastExecutionTime = lastExecutionTimes.computeIfAbsent(key, k -> new AtomicLong(0));

    // Atomic check-and-set to avoid race conditions
    while (true) {
      var lastExecution = lastExecutionTime.get();
      var elapsed = now - lastExecution;

      if (elapsed >= cooldownMs) {
        // Try to update atomically
        if (lastExecutionTime.compareAndSet(lastExecution, now)) {
          // Successfully claimed the execution
          return completedMono(results(
            "allowed", true,
            "remainingMs", 0L
          ));
        }
        // Lost race, retry with new value
      } else {
        // Deny execution (no race condition possible here)
        return completedMono(results(
          "allowed", false,
          "remainingMs", cooldownMs - elapsed
        ));
      }
    }
  }
}
