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

/// Math node that computes the modulo (remainder) of two numbers.
/// Inputs: a, b
/// Output: result = a % b (returns 0 if b is 0)
public final class ModuloNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("math.modulo")
    .displayName("Modulo")
    .category(NodeCategory.MATH)
    .addInputs(
      PortDefinition.inputWithDefault("a", "A", PortType.NUMBER, "0", "Dividend"),
      PortDefinition.inputWithDefault("b", "B", PortType.NUMBER, "1", "Divisor")
    )
    .addOutputs(
      PortDefinition.output("result", "Result", PortType.NUMBER, "Remainder of A divided by B")
    )
    .description("Computes the remainder of A divided by B")
    .icon("percent")
    .color("#2196F3")
    .addKeywords("modulo", "mod", "remainder", "arithmetic")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var a = getDoubleInput(inputs, "a", 0.0);
    var b = getDoubleInput(inputs, "b", 1.0);
    var resultValue = b != 0 ? a % b : 0.0;
    return completed(result("result", resultValue));
  }
}
