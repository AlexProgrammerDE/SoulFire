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
package com.soulfiremc.server.script.nodes.trigger;

import com.soulfiremc.server.script.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Trigger node that fires at a configurable interval.
/// Input: intervalMs - the interval in milliseconds (default: 1000)
/// Output: executionCount - number of times this trigger has fired
public final class OnIntervalNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("trigger.on_interval")
    .displayName("On Interval")
    .category(NodeCategory.TRIGGERS)
    .addInputs(
      PortDefinition.inputWithDefault("intervalMs", "Interval (ms)", PortType.NUMBER, "1000", "Interval in milliseconds between executions")
    )
    .addOutputs(
      PortDefinition.execOut(),
      PortDefinition.output("executionCount", "Execution Count", PortType.NUMBER, "Number of times this trigger has fired")
    )
    .isTrigger(true)
    .description("Fires at a configurable interval")
    .icon("timer")
    .color("#4CAF50")
    .addKeywords("interval", "timer", "repeat", "periodic", "schedule")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var executionCount = getLongInput(inputs, "executionCount", 1L);
    return completed(result("executionCount", executionCount));
  }
}
