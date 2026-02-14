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

/// String node that returns the length of a string.
/// Input: text
/// Output: length
public final class StringLengthNode extends AbstractScriptNode {
  public static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("string.length")
    .displayName("String Length")
    .category(CategoryRegistry.STRING)
    .addInputs(
      PortDefinition.inputWithDefault("text", "Text", PortType.STRING, "\"\"", "Input string")
    )
    .addOutputs(
      PortDefinition.output("length", "Length", PortType.NUMBER, "Length of the string")
    )
    .description("Returns the length of a string")
    .icon("text")
    .color("#8BC34A")
    .addKeywords("string", "length", "count", "size")
    .build();

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var text = getStringInput(inputs, "text", "");
    return completedMono(result("length", text.length()));
  }
}
