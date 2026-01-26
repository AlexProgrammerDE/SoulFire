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
package com.soulfiremc.server.script.nodes.list;

import com.soulfiremc.server.script.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// List node that generates a range of numbers.
/// Inputs: start, end, step
/// Output: list
public final class RangeNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("list.range")
    .displayName("Range")
    .category(NodeCategory.LIST)
    .addInputs(
      PortDefinition.inputWithDefault("start", "Start", PortType.NUMBER, "0", "Starting value (inclusive)"),
      PortDefinition.inputWithDefault("end", "End", PortType.NUMBER, "10", "Ending value (exclusive)"),
      PortDefinition.inputWithDefault("step", "Step", PortType.NUMBER, "1", "Step increment between values")
    )
    .addOutputs(
      PortDefinition.listOutput("list", "List", PortType.NUMBER, "Generated list of numbers")
    )
    .description("Generates a list of numbers from start to end with a given step")
    .icon("list-ordered")
    .color("#00BCD4")
    .addKeywords("list", "range", "sequence", "numbers", "generate", "create")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var start = getDoubleInput(inputs, "start", 0.0);
    var end = getDoubleInput(inputs, "end", 10.0);
    var step = getDoubleInput(inputs, "step", 1.0);

    // Prevent infinite loops
    if (step == 0 || (step > 0 && start > end) || (step < 0 && start < end)) {
      return completed(result("list", List.of()));
    }

    // Limit to 10000 elements to prevent memory issues
    var maxElements = 10000;
    var list = new ArrayList<Double>();

    if (step > 0) {
      for (double i = start; i < end && list.size() < maxElements; i += step) {
        list.add(i);
      }
    } else {
      for (double i = start; i > end && list.size() < maxElements; i += step) {
        list.add(i);
      }
    }

    return completed(result("list", list));
  }
}
