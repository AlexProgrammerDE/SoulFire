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
package com.soulfiremc.server.script.nodes.string;

import com.soulfiremc.server.script.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/// String node that concatenates two strings.
/// Inputs: a, b
/// Output: result = a + b
public final class ConcatNode extends AbstractScriptNode {
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("string.concat")
    .displayName("Concat")
    .category(CategoryRegistry.STRING)
    .addInputs(
      PortDefinition.inputWithDefault("a", "A", PortType.STRING, "\"\"", "First string"),
      PortDefinition.inputWithDefault("b", "B", PortType.STRING, "\"\"", "Second string")
    )
    .addOutputs(
      PortDefinition.output("result", "Result", PortType.STRING, "Concatenated string")
    )
    .description("Concatenates two strings together")
    .icon("text")
    .color("#8BC34A")
    .addKeywords("string", "concat", "concatenate", "join", "append", "combine")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var a = getStringInput(inputs, "a", "");
    var b = getStringInput(inputs, "b", "");
    return completedMono(result("result", a + b));
  }
}
