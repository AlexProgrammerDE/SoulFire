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

import com.soulfiremc.server.script.*;

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
  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("math.formula")
    .displayName("Formula")
    .category(NodeCategory.MATH)
    .addInputs(
      PortDefinition.inputWithDefault("expression", "Expression", PortType.STRING, "\"a + b\"", "Mathematical expression"),
      PortDefinition.inputWithDefault("a", "A", PortType.NUMBER, "0", "Variable a"),
      PortDefinition.inputWithDefault("b", "B", PortType.NUMBER, "0", "Variable b"),
      PortDefinition.inputWithDefault("c", "C", PortType.NUMBER, "0", "Variable c"),
      PortDefinition.inputWithDefault("d", "D", PortType.NUMBER, "0", "Variable d"),
      PortDefinition.inputWithDefault("e", "E", PortType.NUMBER, "0", "Variable e"),
      PortDefinition.inputWithDefault("f", "F", PortType.NUMBER, "0", "Variable f")
    )
    .addOutputs(
      PortDefinition.output("result", "Result", PortType.NUMBER, "Expression result"),
      PortDefinition.output("error", "Error", PortType.STRING, "Error message if any")
    )
    .description("Evaluates a custom mathematical expression")
    .icon("function-square")
    .color("#2196F3")
    .addKeywords("formula", "expression", "evaluate", "math")
    .build();

  private static final ScriptEngine ENGINE;

  static {
    var manager = new ScriptEngineManager();
    ENGINE = manager.getEngineByName("JavaScript");
  }

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
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
