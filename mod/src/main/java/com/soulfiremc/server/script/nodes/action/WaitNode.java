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
package com.soulfiremc.server.script.nodes.action;

import com.soulfiremc.server.script.*;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/// Action node that delays execution for a specified duration with optional jitter.
public final class WaitNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("action.wait")
    .displayName("Wait")
    .category(CategoryRegistry.ACTIONS)
    .addInputs(
      PortDefinition.execIn(),
      PortDefinition.inputWithDefault("baseMs", "Duration (ms)", PortType.NUMBER, "1000", "Base delay in milliseconds"),
      PortDefinition.inputWithDefault("jitterMs", "Jitter (ms)", PortType.NUMBER, "0", "Random jitter +/- this value (0 = no jitter)"),
      PortDefinition.inputWithDefault("minMs", "Min (ms)", PortType.NUMBER, "0", "Minimum delay (clamp)"),
      PortDefinition.inputWithDefault("maxMs", "Max (ms)", PortType.NUMBER, "60000", "Maximum delay (clamp)")
    )
    .addOutputs(
      PortDefinition.execOut(),
      PortDefinition.output("actualMs", "Actual (ms)", PortType.NUMBER, "Actual delay used")
    )
    .description("Delays execution for a specified duration with optional random jitter")
    .icon("clock")
    .color("#FF9800")
    .addKeywords("wait", "delay", "sleep", "pause", "timer", "jitter")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var baseMs = getLongInput(inputs, "baseMs", 1000L);
    var jitterMs = getLongInput(inputs, "jitterMs", 0L);
    var minMs = getLongInput(inputs, "minMs", 0L);
    var maxMs = getLongInput(inputs, "maxMs", 60000L);

    var jitter = jitterMs > 0 ? ThreadLocalRandom.current().nextLong(-jitterMs, jitterMs + 1) : 0;
    var delay = Math.max(minMs, Math.min(maxMs, baseMs + jitter));

    if (delay <= 0) {
      return completedMono(result("actualMs", 0L));
    }

    return delayedMono(Duration.ofMillis(delay), result("actualMs", delay));
  }
}
