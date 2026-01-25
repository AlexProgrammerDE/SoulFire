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
  public Map<String, Object> getDefaultInputs() {
    return Map.of("a", 0.0, "b", 0.0, "operator", "==");
  }

  @Override
  public CompletableFuture<Map<String, Object>> execute(ScriptContext context, Map<String, Object> inputs) {
    var a = inputs.get("a");
    var b = inputs.get("b");
    var operator = getStringInput(inputs, "operator", "==");

    boolean resultValue;

    // Handle numeric comparisons
    if (a instanceof Number na && b instanceof Number nb) {
      var da = na.doubleValue();
      var db = nb.doubleValue();
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
      // Handle equality for non-numeric types
      resultValue = switch (operator) {
        case "==" -> Objects.equals(a, b);
        case "!=" -> !Objects.equals(a, b);
        default -> false; // Other operators don't make sense for non-numeric types
      };
    }

    return completed(result("result", resultValue));
  }
}
