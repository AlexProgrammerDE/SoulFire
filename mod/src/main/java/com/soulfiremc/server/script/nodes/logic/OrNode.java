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
package com.soulfiremc.server.script.nodes.logic;

import com.soulfiremc.server.script.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Logic node that performs logical OR on two boolean values.
/// Inputs: a, b (booleans)
/// Output: result = a OR b
public final class OrNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("logic.or")
    .displayName("Or")
    .category(CategoryRegistry.LOGIC)
    .addInputs(
      PortDefinition.inputWithDefault("a", "A", PortType.BOOLEAN, "false", "First boolean"),
      PortDefinition.inputWithDefault("b", "B", PortType.BOOLEAN, "false", "Second boolean")
    )
    .addOutputs(
      PortDefinition.output("result", "Result", PortType.BOOLEAN, "A OR B")
    )
    .description("Returns true if either input is true")
    .icon("circle")
    .color("#9C27B0")
    .addKeywords("or", "either", "any", "logic")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var a = getBooleanInput(inputs, "a", false);
    var b = getBooleanInput(inputs, "b", false);
    return completed(result("result", a || b));
  }
}
