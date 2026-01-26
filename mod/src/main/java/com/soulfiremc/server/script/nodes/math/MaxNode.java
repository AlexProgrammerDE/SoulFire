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

/// Math node that returns the maximum of two numbers.
/// Inputs: a, b
/// Output: result
public final class MaxNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("math.max")
    .displayName("Maximum")
    .category(NodeCategory.MATH)
    .addInputs(
      PortDefinition.inputWithDefault("a", "A", PortType.NUMBER, "0", "First number"),
      PortDefinition.inputWithDefault("b", "B", PortType.NUMBER, "0", "Second number")
    )
    .addOutputs(
      PortDefinition.output("result", "Result", PortType.NUMBER, "Larger of A and B")
    )
    .description("Returns the larger of two numbers")
    .icon("arrow-up-to-line")
    .color("#2196F3")
    .addKeywords("max", "maximum", "larger", "greatest")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var a = getDoubleInput(inputs, "a", 0.0);
    var b = getDoubleInput(inputs, "b", 0.0);
    return completed(result("result", Math.max(a, b)));
  }
}
