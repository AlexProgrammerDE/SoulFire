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
package com.soulfiremc.server.script;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/// Handles implicit type conversions between node port types (Blender-style).
/// Conversions are performed automatically when connecting ports of different types.
public final class TypeConverter {

  private TypeConverter() {
    // Utility class
  }

  /// Converts a NodeValue from one type to another if needed.
  /// Returns the original value if no conversion is needed or possible.
  ///
  /// @param value      the value to convert
  /// @param sourceType the type of the source port
  /// @param targetType the type of the target port
  /// @return the converted value, or the original if same type or conversion not possible
  public static NodeValue convert(NodeValue value, PortType sourceType, PortType targetType) {
    // No conversion needed for same type or ANY
    if (sourceType == targetType || targetType == PortType.ANY || sourceType == PortType.ANY) {
      return value;
    }

    // Handle null values
    if (value == null || value.isNull()) {
      return value;
    }

    return switch (targetType) {
      case NUMBER -> convertToNumber(value, sourceType);
      case STRING -> convertToString(value);
      case BOOLEAN -> convertToBoolean(value, sourceType);
      case VECTOR3 -> convertToVector3(value, sourceType);
      case LIST -> convertToList(value);
      default -> value; // No conversion for BOT, ENTITY, ITEM, BLOCK, EXEC
    };
  }

  /// Converts a value to NUMBER.
  /// - BOOLEAN: false→0, true→1
  /// - STRING: parsed as number, NaN on failure
  /// - VECTOR3: average of components (like Blender)
  private static NodeValue convertToNumber(NodeValue value, PortType sourceType) {
    return switch (sourceType) {
      case BOOLEAN -> NodeValue.of(value.asBoolean(false) ? 1.0 : 0.0);
      case STRING -> {
        var str = value.asString("");
        try {
          yield NodeValue.of(Double.parseDouble(str));
        } catch (NumberFormatException e) {
          yield NodeValue.of(Double.NaN);
        }
      }
      case VECTOR3 -> {
        // Average of components (like Blender's Vector → Value conversion)
        var list = value.asList();
        if (list.size() >= 3) {
          var x = list.get(0).asDouble(0);
          var y = list.get(1).asDouble(0);
          var z = list.get(2).asDouble(0);
          yield NodeValue.of((x + y + z) / 3.0);
        }
        yield NodeValue.of(0.0);
      }
      default -> value;
    };
  }

  /// Converts a value to STRING via toString().
  private static NodeValue convertToString(NodeValue value) {
    return NodeValue.of(value.toString());
  }

  /// Converts a value to BOOLEAN.
  /// - NUMBER: 0→false, else→true
  /// - STRING: "true"→true, else→false
  private static NodeValue convertToBoolean(NodeValue value, PortType sourceType) {
    return switch (sourceType) {
      case NUMBER -> NodeValue.of(value.asDouble(0) != 0);
      case STRING -> NodeValue.of("true".equalsIgnoreCase(value.asString("")));
      default -> value;
    };
  }

  /// Converts a value to VECTOR3.
  /// - NUMBER: expanded to (n, n, n) (like Blender's Value → Vector)
  /// - LIST: first 3 elements as x, y, z
  private static NodeValue convertToVector3(NodeValue value, PortType sourceType) {
    return switch (sourceType) {
      case NUMBER -> {
        var n = value.asDouble(0);
        yield NodeValue.of(vectorToList(new Vec3(n, n, n)));
      }
      case LIST -> {
        var list = value.asList();
        var x = list.size() > 0 ? list.get(0).asDouble(0) : 0;
        var y = list.size() > 1 ? list.get(1).asDouble(0) : 0;
        var z = list.size() > 2 ? list.get(2).asDouble(0) : 0;
        yield NodeValue.of(vectorToList(new Vec3(x, y, z)));
      }
      default -> value;
    };
  }

  /// Converts a value to LIST by wrapping it as a single-element list.
  private static NodeValue convertToList(NodeValue value) {
    if (value instanceof NodeValue.Json json && json.element().isJsonArray()) {
      return value; // Already a list
    }
    return NodeValue.of(List.of(value));
  }

  /// Helper to convert Vec3 to a JSON array for NodeValue storage.
  private static JsonArray vectorToList(Vec3 vec) {
    var array = new JsonArray();
    array.add(new JsonPrimitive(vec.x));
    array.add(new JsonPrimitive(vec.y));
    array.add(new JsonPrimitive(vec.z));
    return array;
  }

  /// Checks if a conversion is possible between two types.
  /// This is a static check - actual runtime conversion may still fail.
  ///
  /// @param sourceType the source port type
  /// @param targetType the target port type
  /// @return true if conversion is possible
  public static boolean canConvert(PortType sourceType, PortType targetType) {
    return targetType.canAccept(sourceType);
  }
}
