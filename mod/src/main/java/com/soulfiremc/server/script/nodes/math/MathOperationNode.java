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

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Combined math node with mode-based visibility (Blender-style).
/// The operation dropdown controls which inputs are visible:
/// - Unary operations (ABS, NEGATE, SQRT, etc.) only show input A
/// - Binary operations (ADD, SUBTRACT, etc.) show inputs A and B
/// - Ternary operations (CLAMP, LERP) show inputs A, B, and C
public final class MathOperationNode extends AbstractScriptNode {
  // Unary operations - only A is visible
  private static final String UNARY_OPS = "ABS,NEGATE,SQRT,SIGN,FLOOR,CEIL,ROUND,SIN,COS,TAN,ASIN,ACOS,ATAN,SINH,COSH,TANH,LOG,LOG10,LOG2,EXP,EXP2,DEGREES,RADIANS,FRACT";

  // Ternary operations - A, B, and C are visible
  private static final String TERNARY_OPS = "CLAMP,LERP,SMOOTHSTEP";

  private static final NodeMetadata METADATA = NodeMetadata.builder()
    .type("math.operation")
    .displayName("Math")
    .category(CategoryRegistry.MATH)
    .hasDynamicVisibility(true)
    .visibilityControlField("operation")
    .addInputs(
      // A is always visible
      PortDefinition.inputWithDefault("a", "A", PortType.NUMBER, "0", "First operand (Value for unary ops)"),
      // B is visible for binary and ternary operations
      PortDefinition.conditionalInputWithDefault(
        "b", "B", PortType.NUMBER, "0",
        "operation!=" + UNARY_OPS.replace(",", ",operation!="),
        "Second operand (Min for CLAMP, Start for LERP)"
      ),
      // C is only visible for ternary operations
      PortDefinition.conditionalInputWithDefault(
        "c", "C", PortType.NUMBER, "0",
        "operation=" + TERNARY_OPS.replace(",", ",operation="),
        "Third operand (Max for CLAMP, Factor for LERP)"
      )
    )
    .addOutputs(
      PortDefinition.output("result", "Result", PortType.NUMBER, "Operation result")
    )
    .description("Performs various math operations. Input visibility changes based on operation type.")
    .icon("calculator")
    .color("#2196F3")
    .addKeywords("math", "add", "subtract", "multiply", "divide", "abs", "negate", "sqrt", "power",
                 "modulo", "min", "max", "clamp", "lerp", "sin", "cos", "tan", "log", "exp", "floor",
                 "ceil", "round", "atan2", "smoothstep", "operation", "arithmetic")
    .build();

  @Override
  public NodeMetadata getMetadata() {
    return METADATA;
  }

  @Override
  public CompletableFuture<Map<String, NodeValue>> execute(NodeRuntime runtime, Map<String, NodeValue> inputs) {
    var a = getDoubleInput(inputs, "a", 0.0);
    var b = getDoubleInput(inputs, "b", 0.0);
    var c = getDoubleInput(inputs, "c", 0.0);
    var operation = getStringInput(inputs, "operation", "ADD");

    var result = switch (operation.toUpperCase()) {
      // ===== Binary operations =====
      case "ADD" -> a + b;
      case "SUBTRACT" -> a - b;
      case "MULTIPLY" -> a * b;
      case "DIVIDE" -> b != 0 ? a / b : Double.NaN;
      case "MODULO" -> b != 0 ? a % b : Double.NaN;
      case "POWER" -> Math.pow(a, b);
      case "MIN" -> Math.min(a, b);
      case "MAX" -> Math.max(a, b);
      case "ATAN2" -> Math.atan2(a, b);
      case "HYPOT" -> Math.hypot(a, b);
      case "COPYSIGN" -> Math.copySign(a, b);

      // ===== Ternary operations =====
      case "CLAMP" -> Math.max(b, Math.min(c, a));  // Clamp A between B (min) and C (max)
      case "LERP" -> a + (b - a) * c;               // Linear interpolate from A to B by factor C
      case "SMOOTHSTEP" -> {                        // Smooth Hermite interpolation
        var t = Math.max(0, Math.min(1, (a - b) / (c - b)));
        yield t * t * (3 - 2 * t);
      }

      // ===== Unary operations =====
      case "ABS" -> Math.abs(a);
      case "NEGATE" -> -a;
      case "SQRT" -> Math.sqrt(a);
      case "SIGN" -> Math.signum(a);
      case "FLOOR" -> Math.floor(a);
      case "CEIL" -> Math.ceil(a);
      case "ROUND" -> Math.round(a);
      case "FRACT" -> a - Math.floor(a);           // Fractional part

      // Trigonometry
      case "SIN" -> Math.sin(a);
      case "COS" -> Math.cos(a);
      case "TAN" -> Math.tan(a);
      case "ASIN" -> Math.asin(a);
      case "ACOS" -> Math.acos(a);
      case "ATAN" -> Math.atan(a);
      case "SINH" -> Math.sinh(a);
      case "COSH" -> Math.cosh(a);
      case "TANH" -> Math.tanh(a);

      // Angle conversion
      case "DEGREES" -> Math.toDegrees(a);
      case "RADIANS" -> Math.toRadians(a);

      // Logarithms and exponentials
      case "LOG" -> Math.log(a);                   // Natural log
      case "LOG10" -> Math.log10(a);
      case "LOG2" -> Math.log(a) / Math.log(2);
      case "EXP" -> Math.exp(a);
      case "EXP2" -> Math.pow(2, a);

      default -> 0.0;
    };

    return completed(result("result", result));
  }
}
