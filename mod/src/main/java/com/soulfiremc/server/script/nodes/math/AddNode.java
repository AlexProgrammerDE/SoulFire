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

/// Math node that adds two numbers.
/// Inputs: a, b
/// Output: result = a + b
public final class AddNode extends AbstractScriptNode {
  public static final String TYPE = "math.add";
  private static final NodeMetadata METADATA = NodeMetadata.builder(TYPE)
    .displayName("Add")
    .description("Adds two numbers together")
    .category("Math")
    .inputs(
      PortDefinition.inputWithDefault("a", "A", PortType.NUMBER, "0", "First number"),
      PortDefinition.inputWithDefault("b", "B", PortType.NUMBER, "0", "Second number")
    )
    .outputs(
      PortDefinition.output("result", "Result", PortType.NUMBER, "Sum of A and B")
    )
    .icon("plus")
    .color("#2196F3")
    .keywords("add", "plus", "sum", "arithmetic")
    .build();

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public Map<String, NodeValue> getDefaultInputs() {
    return Map.of("a", NodeValue.ofNumber(0.0), "b", NodeValue.ofNumber(0.0));
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var a = getDoubleInput(inputs, "a", 0.0);
    var b = getDoubleInput(inputs, "b", 0.0);
    return completed(result("result", a + b));
  }
}
