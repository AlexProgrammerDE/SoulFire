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
import com.soulfiremc.server.script.NodeValue;
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
  public Map<String, NodeValue> getDefaultInputs() {
    return Map.of("value", NodeValue.ofString(""), "default", NodeValue.ofNumber(0.0));
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(ScriptContext context, Map<String, NodeValue> inputs) {
    var value = inputs.get("value");
    var defaultValue = getDoubleInput(inputs, "default", 0.0);

    if (value != null && !value.isNull()) {
      // Try to get as number directly
      var asNum = value.asDouble(Double.NaN);
      if (!Double.isNaN(asNum)) {
        return completed(results("result", asNum, "success", true));
      }

      // Try to parse as string
      try {
        var parsed = Double.parseDouble(value.asString("").trim());
        return completed(results("result", parsed, "success", true));
      } catch (NumberFormatException ignored) {
        // Fall through to default
      }
    }

    return completed(results("result", defaultValue, "success", false));
  }
}
