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

import com.soulfiremc.server.script.AbstractScriptNode;
import com.soulfiremc.server.script.ScriptContext;

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
  public static final String TYPE = "flow.debounce";

  // Track last execution time per key
  private static final Map<String, Long> lastExecutionTimes = new ConcurrentHashMap<>();

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public Map<String, Object> getDefaultInputs() {
    return Map.of("cooldownMs", 1000L, "key", "default");
  }

  @Override
  public CompletableFuture<Map<String, Object>> execute(ScriptContext context, Map<String, Object> inputs) {
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
