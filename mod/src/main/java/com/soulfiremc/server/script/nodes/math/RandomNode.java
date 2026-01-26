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
import java.util.concurrent.ThreadLocalRandom;

/// Math node that generates a random number within a range.
/// Inputs: min (default 0), max (default 1)
/// Output: result (random double between min and max)
public final class RandomNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("math.random")
    .displayName("Random")
    .category(NodeCategory.MATH)
    .addInputs(
      PortDefinition.inputWithDefault("min", "Min", PortType.NUMBER, "0", "Minimum value"),
      PortDefinition.inputWithDefault("max", "Max", PortType.NUMBER, "1", "Maximum value")
    )
    .addOutputs(
      PortDefinition.output("result", "Result", PortType.NUMBER, "Random number between min and max")
    )
    .description("Generates a random number within a range")
    .icon("shuffle")
    .color("#2196F3")
    .addKeywords("random", "rand", "chance", "dice")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var min = getDoubleInput(inputs, "min", 0.0);
    var max = getDoubleInput(inputs, "max", 1.0);

    if (min > max) {
      // Swap if min > max
      var temp = min;
      min = max;
      max = temp;
    }

    var randomValue = ThreadLocalRandom.current().nextDouble(min, max);
    return completed(result("result", randomValue));
  }
}
