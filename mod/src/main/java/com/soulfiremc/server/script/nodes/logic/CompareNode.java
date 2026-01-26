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
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/// Logic node that compares two values using a specified operator.
/// Inputs: a, b, operator (one of: ==, !=, <, >, <=, >=)
/// Output: result (boolean)
public final class CompareNode extends AbstractScriptNode {
  public static final String TYPE = "logic.compare";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public Map<String, NodeValue> getDefaultInputs() {
    return Map.of("a", NodeValue.ofNumber(0.0), "b", NodeValue.ofNumber(0.0), "operator", NodeValue.ofString("=="));
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(ScriptContext context, Map<String, NodeValue> inputs) {
    var a = inputs.get("a");
    var b = inputs.get("b");
    var operator = getStringInput(inputs, "operator", "==");

    boolean resultValue;

    // Try numeric comparison first
    var da = a != null ? a.asDouble(Double.NaN) : Double.NaN;
    var db = b != null ? b.asDouble(Double.NaN) : Double.NaN;

    if (!Double.isNaN(da) && !Double.isNaN(db)) {
      // Handle numeric comparisons
      resultValue = switch (operator) {
        case "==" -> da == db;
        case "!=" -> da != db;
        case "<" -> da < db;
        case ">" -> da > db;
        case "<=" -> da <= db;
        case ">=" -> da >= db;
        default -> false;
      };
    } else {
      // Handle equality for non-numeric types (compare string representations)
      var strA = a != null ? a.asString("") : "";
      var strB = b != null ? b.asString("") : "";
      resultValue = switch (operator) {
        case "==" -> Objects.equals(strA, strB);
        case "!=" -> !Objects.equals(strA, strB);
        default -> false; // Other operators don't make sense for non-numeric types
      };
    }

    return completed(result("result", resultValue));
  }
}
