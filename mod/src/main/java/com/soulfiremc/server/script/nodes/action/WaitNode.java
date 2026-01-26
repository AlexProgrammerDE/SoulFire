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

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/// Action node that delays execution for a specified duration.
/// Input: durationMs (milliseconds to wait)
public final class WaitNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("action.wait")
    .displayName("Wait")
    .category(CategoryRegistry.ACTIONS)
    .addInputs(
      PortDefinition.execIn(),
      PortDefinition.inputWithDefault("durationMs", "Duration (ms)", PortType.NUMBER, "1000", "Time to wait in milliseconds")
    )
    .addOutputs(
      PortDefinition.execOut()
    )
    .description("Delays execution for a specified duration in milliseconds")
    .icon("clock")
    .color("#FF9800")
    .addKeywords("wait", "delay", "sleep", "pause", "timer")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var durationMs = getLongInput(inputs, "durationMs", 1000L);

    if (durationMs <= 0) {
      return completedEmpty();
    }

    var future = new CompletableFuture<Map<String, NodeValue>>();

    runtime.scheduler().schedule(
      () -> future.complete(emptyResult()),
      durationMs,
      TimeUnit.MILLISECONDS
    );

    return future;
  }
}
