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
package com.soulfiremc.server.script.nodes.math;

import com.soulfiremc.server.script.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Math node that rounds a number to the nearest integer.
/// Input: value
/// Output: result
public final class RoundNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("math.round")
    .displayName("Round")
    .category(CategoryRegistry.MATH)
    .addInputs(
      PortDefinition.inputWithDefault("value", "Value", PortType.NUMBER, "0", "Input number")
    )
    .addOutputs(
      PortDefinition.output("result", "Result", PortType.NUMBER, "Rounded value")
    )
    .description("Rounds a number to the nearest integer")
    .icon("circle")
    .color("#2196F3")
    .addKeywords("round", "nearest", "integer")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var value = getDoubleInput(inputs, "value", 0.0);
    return completed(result("result", (double) Math.round(value)));
  }
}
