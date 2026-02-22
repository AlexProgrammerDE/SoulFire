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
package com.soulfiremc.server.script.nodes.flow;

import com.soulfiremc.server.script.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/// Terminal node for check chains in condition-based loops.
/// Takes a boolean input and sets the check result flag on the runtime.
/// Must be placed at the end of an exec_check chain (e.g., from RepeatUntilNode).
/// Has no outputs — it is a terminal node that signals back to the loop.
public final class ResultNode extends AbstractScriptNode {
  public static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("flow.result")
    .displayName("Result")
    .category(CategoryRegistry.FLOW)
    .addInputs(
      PortDefinition.execIn(),
      PortDefinition.inputWithDefault("value", "Value", PortType.BOOLEAN, "false",
        "The boolean result to report back to the enclosing loop")
    )
    .description("Reports a boolean result back to a loop's check chain")
    .icon("flag")
    .color("#607D8B")
    .addKeywords("result", "check", "condition", "loop", "terminal")
    .build();

  @Override
  public Mono<Map<String, NodeValue>> executeReactive(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var value = getBooleanInput(inputs, "value", false);
    runtime.setCheckResult(value);
    return Mono.just(Map.of());
  }
}
