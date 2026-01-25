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
package com.soulfiremc.server.script.nodes.action;

import com.soulfiremc.server.script.AbstractScriptNode;
import com.soulfiremc.server.script.ScriptContext;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Action node that sets a variable value.
/// Input: variableName (from node data), value (any)
public final class SetVariableNode extends AbstractScriptNode {
  public static final String TYPE = "action.set_variable";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public Map<String, Object> getDefaultInputs() {
    return Map.of("variableName", "myVar");
  }

  @Override
  public CompletableFuture<Map<String, Object>> execute(ScriptContext context, Map<String, Object> inputs) {
    var varName = getStringInput(inputs, "variableName", "");
    var value = getInput(inputs, "value", null);

    if (!varName.isEmpty()) {
      context.setVariable(varName, value);
    }

    return completedEmpty();
  }
}
