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

import com.soulfiremc.server.script.AbstractScriptNode;
import com.soulfiremc.server.script.ScriptContext;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Flow control node that conditionally passes through execution.
/// Input: condition (boolean) - whether to allow execution to pass through
/// Input: value (any) - the value to pass through
/// Output: passed (boolean) - whether execution was allowed
/// Output: value (any) - the passed-through value (only if condition is true)
///
/// Acts as a conditional gate - if condition is false, the output stops here.
public final class GateNode extends AbstractScriptNode {
  public static final String TYPE = "flow.gate";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public Map<String, Object> getDefaultInputs() {
    return Map.of("condition", true, "value", null);
  }

  @Override
  public CompletableFuture<Map<String, Object>> execute(ScriptContext context, Map<String, Object> inputs) {
    var condition = getBooleanInput(inputs, "condition", true);
    var value = inputs.get("value");

    return completed(results(
      "passed", condition,
      "value", condition ? value : null
    ));
  }
}
