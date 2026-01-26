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
package com.soulfiremc.server.script.nodes.constant;

import com.soulfiremc.server.script.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Constant node that outputs a configurable number value.
/// Output: value (number)
public final class NumberConstantNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("constant.number")
    .displayName("Number")
    .category(NodeCategory.CONSTANTS)
    .addInputs(
      PortDefinition.inputWithDefault("value", "Value", PortType.NUMBER, "0", "The constant value")
    )
    .addOutputs(
      PortDefinition.output("value", "Value", PortType.NUMBER, "The constant value")
    )
    .description("Outputs a constant number value")
    .icon("hash")
    .color("#E91E63")
    .addKeywords("number", "constant", "value")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var value = getDoubleInput(inputs, "value", 0.0);
    return completed(result("value", value));
  }
}
