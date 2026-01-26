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

/// Math node that subtracts two numbers.
/// Inputs: a, b
/// Output: result = a - b
public final class SubtractNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("math.subtract")
    .displayName("Subtract")
    .category(NodeCategory.MATH)
    .addInputs(
      PortDefinition.inputWithDefault("a", "A", PortType.NUMBER, "0", "First number"),
      PortDefinition.inputWithDefault("b", "B", PortType.NUMBER, "0", "Second number")
    )
    .addOutputs(
      PortDefinition.output("result", "Result", PortType.NUMBER, "A minus B")
    )
    .description("Subtracts B from A")
    .icon("minus")
    .color("#2196F3")
    .addKeywords("subtract", "minus", "difference", "arithmetic")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var a = getDoubleInput(inputs, "a", 0.0);
    var b = getDoubleInput(inputs, "b", 0.0);
    return completed(result("result", a - b));
  }
}
