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
package com.soulfiremc.server.script.nodes.util;

import com.soulfiremc.server.script.AbstractScriptNode;
import com.soulfiremc.server.script.ScriptContext;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Utility node that parses a string to a number.
/// Input: value, default
/// Outputs: result, success (boolean)
public final class ToNumberNode extends AbstractScriptNode {
  public static final String TYPE = "util.to_number";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public Map<String, Object> getDefaultInputs() {
    return Map.of("value", "", "default", 0.0);
  }

  @Override
  public CompletableFuture<Map<String, Object>> execute(ScriptContext context, Map<String, Object> inputs) {
    var value = inputs.get("value");
    var defaultValue = getDoubleInput(inputs, "default", 0.0);

    if (value instanceof Number n) {
      return completed(results("result", n.doubleValue(), "success", true));
    }

    if (value != null) {
      try {
        var parsed = Double.parseDouble(value.toString().trim());
        return completed(results("result", parsed, "success", true));
      } catch (NumberFormatException ignored) {
        // Fall through to default
      }
    }

    return completed(results("result", defaultValue, "success", false));
  }
}
