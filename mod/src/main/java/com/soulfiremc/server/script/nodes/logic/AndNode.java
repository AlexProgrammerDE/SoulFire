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

import com.soulfiremc.server.script.AbstractScriptNode;
import com.soulfiremc.server.script.NodeValue;
import com.soulfiremc.server.script.ScriptContext;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Logic node that performs logical AND on two boolean values.
/// Inputs: a, b (booleans)
/// Output: result = a AND b
public final class AndNode extends AbstractScriptNode {
  public static final String TYPE = "logic.and";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public Map<String, NodeValue> getDefaultInputs() {
    return Map.of("a", NodeValue.ofBoolean(false), "b", NodeValue.ofBoolean(false));
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(ScriptContext context, Map<String, NodeValue> inputs) {
    var a = getBooleanInput(inputs, "a", false);
    var b = getBooleanInput(inputs, "b", false);
    return completed(result("result", a && b));
  }
}
