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
package com.soulfiremc.server.script.nodes.math;

import com.soulfiremc.server.script.AbstractScriptNode;
import com.soulfiremc.server.script.ScriptContext;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Math node that evaluates a custom mathematical expression.
/// Inputs: expression (string), variables a through f (double)
/// Output: result (double)
///
/// Supports basic math operators (+, -, *, /, %), parentheses,
/// and Java Math functions (sin, cos, sqrt, abs, etc.) via JavaScript engine.
public final class FormulaNode extends AbstractScriptNode {
  public static final String TYPE = "math.formula";
  private static final ScriptEngine ENGINE;

  static {
    var manager = new ScriptEngineManager();
    ENGINE = manager.getEngineByName("JavaScript");
  }

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public Map<String, Object> getDefaultInputs() {
    return Map.of(
      "expression", "a + b",
      "a", 0.0,
      "b", 0.0,
      "c", 0.0,
      "d", 0.0,
      "e", 0.0,
      "f", 0.0
    );
  }

  @Override
  public CompletableFuture<Map<String, Object>> execute(ScriptContext context, Map<String, Object> inputs) {
    var expression = getStringInput(inputs, "expression", "0");
    var a = getDoubleInput(inputs, "a", 0.0);
    var b = getDoubleInput(inputs, "b", 0.0);
    var c = getDoubleInput(inputs, "c", 0.0);
    var d = getDoubleInput(inputs, "d", 0.0);
    var e = getDoubleInput(inputs, "e", 0.0);
    var f = getDoubleInput(inputs, "f", 0.0);

    try {
      // Build JavaScript with Math functions available
      var script = String.format(
        "var a=%f, b=%f, c=%f, d=%f, e=%f, f=%f; " +
          "var sin=Math.sin, cos=Math.cos, tan=Math.tan, " +
          "sqrt=Math.sqrt, abs=Math.abs, floor=Math.floor, ceil=Math.ceil, " +
          "round=Math.round, pow=Math.pow, min=Math.min, max=Math.max, " +
          "PI=Math.PI, E=Math.E, log=Math.log, exp=Math.exp; " +
          "(%s)",
        a, b, c, d, e, f, expression
      );

      var resultValue = ENGINE.eval(script);
      double doubleResult;
      if (resultValue instanceof Number n) {
        doubleResult = n.doubleValue();
      } else {
        doubleResult = 0.0;
      }
      return completed(result("result", doubleResult));
    } catch (ScriptException ex) {
      // Return 0 on error
      return completed(results("result", 0.0, "error", ex.getMessage()));
    }
  }
}
