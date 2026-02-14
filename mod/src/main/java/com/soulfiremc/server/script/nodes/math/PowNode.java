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
import reactor.core.publisher.Mono;

import java.util.Map;

/// Math node that raises a number to a power.
/// Inputs: base, exponent
/// Output: result = base ^ exponent
public final class PowNode extends AbstractScriptNode {
  public static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("math.pow")
    .displayName("Power")
    .category(CategoryRegistry.MATH)
    .addInputs(
      PortDefinition.inputWithDefault("base", "Base", PortType.NUMBER, "0", "Base number"),
      PortDefinition.inputWithDefault("exponent", "Exponent", PortType.NUMBER, "1", "Exponent")
    )
    .addOutputs(
      PortDefinition.output("result", "Result", PortType.NUMBER, "Base raised to exponent")
    )
    .description("Raises a number to a power")
    .icon("superscript")
    .color("#2196F3")
    .addKeywords("pow", "power", "exponent", "raise")
    .build();

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var base = getDoubleInput(inputs, "base", 0.0);
    var exponent = getDoubleInput(inputs, "exponent", 1.0);
    return completedMono(result("result", Math.pow(base, exponent)));
  }
}
